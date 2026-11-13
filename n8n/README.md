# n8n workflows

Workflows are exported as JSON into `workflows/` (mandatory deliverable).

```bash
# Import all workflows into the container
docker compose exec n8n n8n import:workflow --separate --input=/workflows

# Activate (publish) them and restart n8n so the production webhooks are registered.
# Importing a workflow again leaves it as a draft: publish it again after every import.
docker compose exec n8n n8n publish:workflow --id=IdxArticlesFlow1
docker compose exec n8n n8n publish:workflow --id=RagAssistFlow2
docker compose up -d n8n   # restarts, and also applies .env changes to the container

# Export after editing them in the UI (http://localhost:5678)
docker compose exec n8n n8n export:workflow --all --separate --output=/workflows
```

The Qdrant collection must exist before indexing: `./scripts/qdrant/create_collection.sh`.

| File | Workflow id | Webhook |
|---|---|---|
| `01-index-articles.json` | `IdxArticlesFlow1` | `POST /webhook/index-articles` |
| `02-rag-assistant.json` | `RagAssistFlow2` | `POST /webhook/rag-assistant` |

Right after n8n starts, the webhooks take a few seconds to register and answer 404 meanwhile.

Every webhook must validate the `X-Webhook-Secret` header against `$env.N8N_WEBHOOK_SECRET`.
Keys (OpenRouter) are read with `$env.*` and never written inside the workflow.

## Contract with Spring Boot

### Flow 1: Indexing (`POST /webhook/index-articles`)

Request:
```json
{ "articles": [ { "id": 12, "title": "...", "problem": "...", "symptoms": "...", "cause": "...", "solution": "...", "active": true } ] }
```
- Active and verified: generate the embedding and **upsert** into Qdrant with point `id` = article `id` (no duplicates).
  The point payload keeps the whole article (title, problem, symptoms, cause, solution), so flow 2 builds its
  prompt from Qdrant alone and every sync keeps it up to date.
- Inactive: delete the point from Qdrant.

Response:
```json
{ "indexed": 1, "deleted": 0, "errors": [] }
```

### Flow 2: RAG assistant (`POST /webhook/rag-assistant`)

Request (threshold and topK are set by Spring Boot, the single source of truth):
```json
{ "queryId": 45, "description": "No hay conexión a internet en el piso 3", "threshold": 0.60, "topK": 5 }
```

Response:
```json
{
  "status": "ANSWERED | NO_RESULTS | ERROR",
  "answer": { "solution": "...", "steps": ["..."] },
  "sources": [ { "articleId": 12, "title": "...", "similarity": 0.83 } ],
  "error": null
}
```
- If no source passes the threshold, return `NO_RESULTS` **without calling the LLM**.
- Spring Boot re-validates that every `articleId` exists and is active in the relational database.
- `description` and `answer` contain user-facing text, so they are in Spanish.
- Second gate: the model may answer `{"relevant": false}` when the sources that passed the threshold do not
  cover the problem; the flow returns `NO_RESULTS` in that case too.
- HTTP codes: `200` for `ANSWERED` and `NO_RESULTS`; `400` for a blank description or an invalid threshold/topK
  (`status: ERROR`); `401` for a wrong secret; `502` (`status: ERROR`, `error` names the failing step) when
  OpenRouter or Qdrant fail or the model returns invalid JSON.

Checked against the running stack with the 25 articles indexed and threshold 0.60:

| Case | Result |
|---|---|
| No secret / blank description / threshold 1.5 | `401` / `400` / `400`, no AI call |
| Test 1: "No hay conexión a internet en el piso 3 desde esta mañana." | `ANSWERED`, source 1 (0.641), 7 steps, 4.4 s |
| Test 2: "La aplicación de facturación se cierra con un error de ejecución al generar reportes." | `ANSWERED`, source 16 (0.791), 7 steps, 4.1 s |
| Test 3: 3D printer calibration | `NO_RESULTS` in 0.8 s (model not called) |
| Second gate: 3D printer extruder error with threshold 0.40 | Network printer article passed (0.589); model answered `relevant: false` → `NO_RESULTS` |
| Off-domain question | `NO_RESULTS` |
| Test 6: article 1 deactivated, test 1 repeated | `NO_RESULTS`: the deactivated article is never used |
| Test 8: Qdrant stopped | `502`, `error: "Search Qdrant failed: ..."` |
