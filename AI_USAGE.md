# AI_USAGE

Short recommendations, added as development progresses.

## Recommendations

- **Same embedding model** for indexing and querying. The Qdrant collection dimension depends on the model; changing it means reindexing everything.
- **Multilingual embedding model:** the content (articles, incidents) is in Spanish, so pick a model that handles Spanish well.
- **Qdrant point id = article id** and use *upsert*. This avoids duplicates without extra logic.
- **No sources, no LLM:** if nothing passes the threshold, return `NO_RESULTS` without calling the model. This is the most reliable way to stop it from making things up.
- **Double source validation:** even if Qdrant filters them, Spring Boot must confirm that every article exists and is active in the relational database.
- **Justify the threshold with data:** try relevant and irrelevant questions, record the similarities and pick the cutoff based on evidence.
- **One place for threshold and top-k:** they live in the Spring Boot config and are sent to n8n on every query. They are not repeated inside the workflow.
- **Low temperature and JSON output** for generation, so answers are stable and easy to validate. Ask the model to answer in Spanish, since the answer is shown to users.
- **Dashboard figures come from SQL**, never from the AI model.
- **Keys only in `.env`**, read in n8n with `$env.*`. Review exported JSONs before committing.
- **Use AI inside your own branch:** each member reviews and understands generated code before committing. The history must reflect each person's work, and everyone must be able to explain their code in the presentation.
- **Reproducible synthetic data:** generate it with a script and a fixed seed, and document its rules.
