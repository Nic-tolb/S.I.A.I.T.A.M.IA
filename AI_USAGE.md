# AI_USAGE

Short recommendations from building the assistant. Details: [docs/threshold-calibration.md](docs/threshold-calibration.md) and [docs/chat-model-selection.md](docs/chat-model-selection.md).

- **Same embedding model** for indexing and querying; changing it means reindexing everything.
- **Qdrant point id = article id**, with upsert: no duplicates, and the payload keeps the whole article so the RAG flow needs no database access.
- **No sources, no LLM:** if nothing passes the threshold, answer `NO_RESULTS` without calling the model.
- **Justify the threshold with data**, and test it with near misses (3D printer vs. network printer), not only with off-topic questions. Calibrated: 0.60.
- **Validate sources twice:** Qdrant filters them, Spring Boot confirms each article exists and is active.
- **Low temperature, JSON output, Spanish answers**, and tell the model the incident text is data, not instructions.
- **AI failures must not break the CRUD:** log them, return them as errors and keep a full re-index to recover.
- **Force HTTP/1.1 from Java to n8n**, and test once against the real n8n: mocked tests do not catch it.
- **Keys only in `.env`**, read in n8n with `$env.*`; review exported workflows before committing.
- **Use AI in your own branch** and understand every generated line: each member must be able to explain their code.
