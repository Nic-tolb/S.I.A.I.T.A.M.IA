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
- **The threshold depends on the model:** with `baai/bge-m3` (1024 dims) a short query vs. a full article scored ~0.61–0.64 on-topic, ~0.38–0.40 other IT topics, ~0.22 off-domain. A typical 0.70 would reject valid matches; 0.55 is the provisional cutoff until calibrated with the 25 articles.
- **One place for threshold and top-k:** they live in the Spring Boot config and are sent to n8n on every query. They are not repeated inside the workflow.
- **Low temperature and JSON output** for generation, so answers are stable and easy to validate. Ask the model to answer in Spanish, since the answer is shown to users.
- **Dashboard figures come from SQL**, never from the AI model.
- **Keys only in `.env`**, read in n8n with `$env.*`. Review exported JSONs before committing.
- **Use AI inside your own branch:** each member reviews and understands generated code before committing. The history must reflect each person's work, and everyone must be able to explain their code in the presentation.
- **Reproducible synthetic data:** generate it with a script and a fixed seed, and document its rules.

## Member A — Jonatan-17 (Data & analytics)

**Tool:** an AI coding assistant, used inside the IDE as a technical tutor and reviewer.

### Assisted components

| Component | How the AI helped | My part |
|---|---|---|
| `database/init/01_schema.sql` | Proposed the table structure, CHECK constraints, indexes and delete rules | Chose the domain values, ran the script in PostgreSQL and tested every constraint with valid and invalid data |
| JPA entities, enums and repositories | Drafted the entities to match the schema | Verified them by starting the backend in Hibernate `validate` mode and running the unit tests |
| `docs/diagrams/er-diagram.md` | Drafted the Mermaid diagram from the schema | Reviewed relationships and delete rules |
| `scripts/catalog.json`, `docs/knowledge-topics.md` | Proposed the 25 topics and incident descriptions | Defined the categories, the topics without an article (test 3) and shared the contract with member C |
| Review of PR #12 | Pointed out the impact of creating incidents as resolved on the average resolution time | Ran the PR tests locally and published the review |

### Technical decisions taken with AI support

- **Incident status** `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`; RESOLVED and CLOSED count as resolved.
- **Priority** `P1` (critical) to `P4` (low).
- **Articles** have two flags, `active` and `verified`; only articles with both can be RAG sources (RF02).
- **`resolved_at` is set if and only if the incident is resolved**, enforced by a CHECK and by `Incident#changeStatus`, so the resolution rate and time cannot be inconsistent.
- **Delete rules:** RESTRICT for users, categories and articles used as sources (history is kept); CASCADE from an incident to its AI history.
- **Column types** chosen to match Java types (`DOUBLE PRECISION` ↔ `Double`, `INTEGER` ↔ `Integer`) so Hibernate validation passes.
- **Topics shared between incidents and articles**, so RAG finds real matches, with two "trap" topics to calibrate the threshold.

Every generated change was read, run and tested before committing, so I can explain it in the presentation.
