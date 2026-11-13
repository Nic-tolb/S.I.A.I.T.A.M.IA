# Similarity threshold calibration

**Decision: `RAG_SIMILARITY_THRESHOLD = 0.60`** (was 0.55, provisional).

The threshold decides whether a knowledge article can be used as a source. If no article reaches it,
the assistant answers `NO_RESULTS` without calling the language model, so it never makes up a solution.

## Setup

| Item | Value |
|---|---|
| Embedding model | `baai/bge-m3` (1024 dimensions, cosine similarity) |
| Indexed articles | The 25 articles in `scripts/articles.json`, active and verified |
| Indexed text | Title, problem, symptoms, cause and solution, with labels (n8n flow 1) |
| Query text | One incident description, as a user would write it |
| Script | [`scripts/calibration/calibrate_threshold.py`](../scripts/calibration/calibrate_threshold.py) |

94 questions in four groups:

| Group | Count | Source |
|---|---:|---|
| Relevant: incidents with an article | 75 | `scripts/catalog.json`, 3 descriptions per topic |
| No article, from the catalog (test 3) | 4 | `impresora-3d-no-calibra`, `software-cad-licencia-red` |
| No article, other IT incidents | 10 | Written for this calibration; some close to existing topics on purpose (scanner, webcam, UPS) |
| Off domain | 5 | Not IT questions (recipes, payroll, weather) |

## Results

**Retrieval works well:** the right article is the first result for **74 of 75** relevant incidents
and is in the top 5 for all 75. The only miss ("Ningún sitio web carga, pero las carpetas compartidas
de la red sí abren") ranks the network folder permissions article first and the right one second.

| Group | Best score: min | median | max |
|---|---:|---:|---:|
| Relevant (score of the right article) | 0.503 | 0.694 | 0.811 |
| No article, catalog | 0.488 | 0.526 | 0.589 |
| No article, other IT | 0.433 | 0.508 | 0.575 |
| Off domain | 0.291 | 0.338 | 0.449 |

The ranges overlap between 0.50 and 0.59, so no threshold is perfect. Effect of each threshold:

| Threshold | Relevant incidents that find their article | Questions without article that pass |
|---:|---:|---:|
| 0.50 | 75/75 | 8/19 |
| 0.55 (previous) | 74/75 | 4–6/19 |
| 0.58 | 71/75 | 1/19 (a test 3 question) |
| **0.60** | **67/75 (89%)** | **0/19** |
| 0.65 | 57–61/75 | 0/19 |
| 0.70 (common default) | 33/75 | 0/19 |

## Why 0.60

- **It is the lowest value that passes test 3.** At 0.58, "La impresora 3D detiene la impresión a la mitad
  con error de extrusor" matches the network printer article (0.589) and the assistant would answer
  with an unrelated procedure.
- **Above 0.60 nothing is gained:** no question without an article passes, and every step up loses
  relevant matches. A common default of 0.70 would reject more than half of the valid incidents.
- **Misses fail safely.** The 8 relevant incidents below 0.60 are vague descriptions (for example
  "El instalador se cierra sin mostrar ningún error", 0.503). They get `NO_RESULTS` instead of a wrong
  answer, which is what the requirements ask for.
- **Mandatory tests:** test 1 (`sin-internet`) scores 0.641 and 0.751 for its two clear descriptions;
  test 2 (`error-ejecucion-app`) scores between 0.664 and 0.791. All four catalog questions without an
  article stay below 0.60 (max 0.589).

## Limitations

- The relevant questions come from the same catalog used to write the articles' symptoms, so real
  incidents may score somewhat lower.
- 15 of the 19 negative questions were written for this calibration; only 4 come from the catalog.
- Queries use the description only. If the assistant sends title and description together, the
  calibration should be repeated with that format.
- Changing the embedding model, the indexed text or the articles changes the scores: repeat the
  calibration with the script.

## How to repeat it

```bash
docker compose up -d postgres qdrant n8n
# load the articles and index them: POST /api/knowledge-articles/reindex
python3 -I scripts/calibration/calibrate_threshold.py calibration-results.json
```
