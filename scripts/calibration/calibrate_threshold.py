"""Similarity threshold calibration for the RAG assistant.

Embeds incident descriptions with the same model used by n8n and searches the Qdrant collection,
then reports how many relevant incidents find their article and how many questions without an
article would wrongly pass each threshold. Results are documented in docs/threshold-calibration.md.

Requirements: Qdrant running with the 25 articles indexed (POST /api/knowledge-articles/reindex),
article ids 1..25 in the order of scripts/articles.json, and OPENROUTER_* variables in .env.

Usage (from the repo root):
    python3 -I scripts/calibration/calibrate_threshold.py [output.json]
"""
import json
import statistics
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = Path(sys.argv[1]) if len(sys.argv) > 1 and sys.argv[1] else None

# Realistic IT incidents with no article in the knowledge base (some close to existing topics on purpose)
NO_ARTICLE_EXTRA = [
    "El proyector de la sala de juntas no enciende y la lámpara parpadea en rojo.",
    "El teléfono IP del escritorio no tiene tono de marcado.",
    "El lector de huellas del control de acceso no reconoce a ningún empleado.",
    "El escáner no envía los documentos escaneados al correo electrónico.",
    "La cámara web no funciona en las videollamadas y aparece una imagen negra.",
    "El UPS de la oficina pita constantemente y muestra batería baja.",
    "La pantalla táctil del kiosco de turnos no responde al tacto.",
    "La tablet de inventario de la bodega no lee los códigos de barras.",
    "El reloj biométrico no registra las marcaciones de entrada.",
    "La grabación de las cámaras de seguridad se detuvo desde anoche.",
]
OFF_DOMAIN = [
    "¿Cuál es la receta de la paella valenciana?",
    "¿Qué día se paga la prima de mitad de año?",
    "Quiero solicitar vacaciones para la próxima semana.",
    "¿Cómo va a estar el clima mañana en Bogotá?",
    "¿Quién ganó el partido de fútbol de ayer?",
]


def env(name, default=None):
    """Reads one variable from .env without loading the whole file into the environment."""
    for line in (ROOT / ".env").read_text(encoding="utf-8").splitlines():
        if line.startswith(name + "="):
            return line.split("=", 1)[1].strip() or default
    return default


def post(url, body, headers=None):
    request = urllib.request.Request(url, data=json.dumps(body).encode(), method="POST",
                                     headers={"Content-Type": "application/json", **(headers or {})})
    with urllib.request.urlopen(request, timeout=120) as response:
        return json.load(response)


def build_queries():
    """Returns (group, expected_topic, text) tuples."""
    catalog = json.loads((ROOT / "scripts/catalog.json").read_text(encoding="utf-8"))
    queries = []
    for category in catalog["categories"]:
        for topic in category["topics"]:
            group = "relevant" if topic.get("has_article") else "no_article_catalog"
            queries += [(group, topic["key"], text) for text in topic.get("descriptions", [])]
    queries += [("no_article_extra", None, text) for text in NO_ARTICLE_EXTRA]
    queries += [("off_domain", None, text) for text in OFF_DOMAIN]
    return queries


def search(queries):
    articles = json.loads((ROOT / "scripts/articles.json").read_text(encoding="utf-8"))
    topic_by_id = {i + 1: article["topic"] for i, article in enumerate(articles)}

    embeddings = post(f"{env('OPENROUTER_BASE_URL')}/embeddings",
                      {"model": env("OPENROUTER_EMBEDDING_MODEL"), "input": [q[2] for q in queries]},
                      {"Authorization": f"Bearer {env('OPENROUTER_API_KEY')}"})
    vectors = [d["embedding"] for d in sorted(embeddings["data"], key=lambda d: d["index"])]

    qdrant = f"http://localhost:{env('QDRANT_PORT', '6333')}"
    collection = env("QDRANT_COLLECTION", "knowledge_articles")
    rows = []
    for (group, expected, text), vector in zip(queries, vectors):
        hits = post(f"{qdrant}/collections/{collection}/points/search",
                    {"vector": vector, "limit": 5, "with_payload": False})["result"]
        top = [(topic_by_id[hit["id"]], round(hit["score"], 4)) for hit in hits]
        relevant = group == "relevant"
        rows.append({
            "group": group,
            "expected": expected if relevant else None,
            "text": text,
            "top": top,
            "expected_rank": next((i + 1 for i, (t, _) in enumerate(top) if relevant and t == expected), None),
            "expected_score": next((s for t, s in top if relevant and t == expected), None),
        })
    return rows


def summary(values):
    values = sorted(values)
    p10 = values[int(0.1 * (len(values) - 1))]
    return (f"n={len(values)} min={values[0]:.3f} p10={p10:.3f} "
            f"median={statistics.median(values):.3f} max={values[-1]:.3f}")


def report(rows):
    relevant = [r for r in rows if r["group"] == "relevant"]
    negatives = [r for r in rows if r["group"] != "relevant"]

    top1 = sum(r["expected_rank"] == 1 for r in relevant)
    top5 = sum(r["expected_rank"] is not None for r in relevant)
    print(f"Relevant: right article first {top1}/{len(relevant)}, in top 5 {top5}/{len(relevant)}")
    print("  score of the right article:", summary([r["expected_score"] for r in relevant if r["expected_score"]]))
    for group in ("no_article_catalog", "no_article_extra", "off_domain"):
        print(f"{group}: best score", summary([r["top"][0][1] for r in rows if r["group"] == group]))

    print("\nRelevant incidents whose article is not first:")
    for r in relevant:
        if r["expected_rank"] != 1:
            print(f"  [{r['expected']}] rank={r['expected_rank']} first={r['top'][0]} | {r['text']}")

    print("\nThreshold | relevant incidents that find their article | questions without article that pass")
    for threshold in [x / 100 for x in range(50, 72, 2)]:
        found = sum(1 for r in relevant if r["expected_score"] and r["expected_score"] >= threshold)
        passing = sum(1 for r in negatives if r["top"][0][1] >= threshold)
        print(f"  {threshold:.2f}    | {found:>2}/{len(relevant)}"
              f"{'':37}| {passing:>2}/{len(negatives)}")


if __name__ == "__main__":
    results = search(build_queries())
    if OUTPUT:
        OUTPUT.write_text(json.dumps(results, ensure_ascii=False, indent=1), encoding="utf-8")
    report(results)
