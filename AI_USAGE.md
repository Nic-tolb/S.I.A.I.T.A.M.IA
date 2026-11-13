# AI_USAGE

Short recommendations, added as development progresses.

## Recommendations

- **Same embedding model** for indexing and querying. The Qdrant collection dimension depends on the model; changing it means reindexing everything.
- **Multilingual embedding model:** the content (articles, incidents) is in Spanish, so pick a model that handles Spanish well.
- **Embed one structured text per article** (title, problem, symptoms, cause and solution with labels), so a query that only describes symptoms still matches.
- **Qdrant point id = article id** and use *upsert*. This avoids duplicates without extra logic.
- **Force HTTP/1.1 when calling n8n from Java.** The JDK `HttpClient` tries an HTTP/2 upgrade (`h2c`) by default and n8n never answers it on POST, so every call ends in a timeout. Mocked tests do not catch this: test once against the real n8n.
- **No sources, no LLM:** if nothing passes the threshold, return `NO_RESULTS` without calling the model. This is the most reliable way to stop it from making things up.
- **Double source validation:** even if Qdrant filters them, Spring Boot must confirm that every article exists and is active in the relational database.
- **Justify the threshold with data:** try relevant and irrelevant questions, record the similarities and pick the cutoff based on evidence.
- **Indexing failures must not break article CRUD:** sync after the transaction commits, log errors instead of throwing, and keep a full re-index endpoint to recover.
- **The threshold depends on the model:** with `baai/bge-m3`, relevant incidents scored 0.50–0.81 against their article, while questions without an article reached up to 0.59. A typical 0.70 would reject more than half of the valid incidents. Calibrated value: **0.60**, the lowest that keeps every question without an article out ([docs/threshold-calibration.md](docs/threshold-calibration.md)).
- **Calibrate with near misses, not only with off-topic questions:** off-domain questions never passed 0.45; the hard cases are IT incidents close to an existing topic (3D printer vs. network printer). They set the threshold.
- **One place for threshold and top-k:** they live in the Spring Boot config and are sent to n8n on every query. They are not repeated inside the workflow.
- **Low temperature and JSON output** for generation, so answers are stable and easy to validate. Ask the model to answer in Spanish, since the answer is shown to users.
- **Second safety gate in the prompt:** even when sources pass the threshold, let the model answer `{"relevant": false}` if they do not cover the problem. Every tested model applied it correctly ([docs/chat-model-selection.md](docs/chat-model-selection.md)).
- **Test with prompt injection in the incident text:** two of five models refused a legitimate incident because its description contained instructions. Tell the model the incident text is data, and check it still answers.
- **Avoid reasoning models for short JSON answers** unless their reasoning budget is configured: `gpt-5-nano` spent its whole output budget thinking and returned empty content, with 10–30 s latency.
- **Dashboard figures come from SQL**, never from the AI model.
- **Keys only in `.env`**, read in n8n with `$env.*`. Review exported JSONs before committing.
- **Use AI inside your own branch:** each member reviews and understands generated code before committing. The history must reflect each person's work, and everyone must be able to explain their code in the presentation.
- **Reproducible synthetic data:** generate it with a script and a fixed seed, and document its rules.
