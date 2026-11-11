# n8n workflows

Workflows are exported as JSON into `workflows/` (mandatory deliverable).

```bash
# Import all workflows into the container
docker compose exec n8n n8n import:workflow --separate --input=/workflows

# Activate (publish) them and restart n8n so the production webhooks are registered
docker compose exec n8n n8n publish:workflow --id=IdxArticlesFlow1
docker compose restart n8n

# Export after editing them in the UI (http://localhost:5678)
docker compose exec n8n n8n export:workflow --all --separate --output=/workflows
```

The Qdrant collection must exist before indexing: `./scripts/qdrant/create_collection.sh`.

| File | Workflow id | Webhook |
|---|---|---|
| `01-index-articles.json` | `IdxArticlesFlow1` | `POST /webhook/index-articles` |

Every webhook must validate the `X-Webhook-Secret` header against `$env.N8N_WEBHOOK_SECRET`.
Keys (OpenRouter) are read with `$env.*` and never written inside the workflow.

## Contract with Spring Boot (draft, to be agreed by the team)

### Flow 1: Indexing (`POST /webhook/index-articles`)

Request:
```json
{ "articles": [ { "id": 12, "title": "...", "problem": "...", "symptoms": "...", "cause": "...", "solution": "...", "active": true } ] }
```
- Active and verified: generate the embedding and **upsert** into Qdrant with point `id` = article `id` (no duplicates).
- Inactive: delete the point from Qdrant.

Response:
```json
{ "indexed": 1, "deleted": 0, "errors": [] }
```

### Flow 2: RAG assistant (`POST /webhook/rag-assistant`)

Request (threshold and topK are set by Spring Boot, the single source of truth):
```json
{ "queryId": 45, "description": "No hay conexión a internet en el piso 3", "threshold": 0.70, "topK": 5 }
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
