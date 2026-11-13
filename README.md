# Technology Incident Analysis and AI Assistance System

Web platform to manage support incidents, analyze historical data and suggest solutions using semantic search and RAG.
Original assignment (Spanish): [proyecto_incidentes_ia.md](proyecto_incidentes_ia.md).

## Architecture

```
Frontend (nginx) ──/api──► Spring Boot ──► PostgreSQL
                                │
                                └──► n8n ──► Qdrant
                                        └──► OpenRouter
```

The frontend only consumes Spring Boot. n8n, Qdrant and OpenRouter are never called from the browser.

## Structure

```
├── backend/            Spring Boot 4 (Java 21): REST API, JPA, validation
├── frontend/           HTML, CSS and JS (served by nginx)
├── nginx/              /api → backend proxy
├── database/
│   ├── init/           Schema and initial data (run when the volume is created)
│   └── analytics/      SQL queries for the indicators
├── n8n/workflows/      Workflows exported as JSON
├── scripts/            Synthetic data generator
├── docs/               Diagrams, testing, data analysis report, Postman/Swagger evidence
├── docker-compose.yml
└── .env.example
```

## Getting started

```bash
cp .env.example .env          # fill in keys and models
docker compose up -d --build
```

| Service | URL |
|---|---|
| Frontend | http://localhost:8081 |
| API / Swagger | http://localhost:8080/swagger-ui.html |
| n8n | http://localhost:5678 |
| Qdrant (dashboard) | http://localhost:6333/dashboard |

Scripts in `database/init` only run when the volume is empty. To recreate the database: `docker compose down -v && docker compose up -d`.

## Conventions

- **Language:** code, comments, commits, PRs and documentation in English. User-visible frontend text (labels, buttons, messages) and user-facing data (incidents, knowledge articles, AI answers) in Spanish.
- **Branches:** `main` holds releases, `develop` is for integration. Use `feature/<module>` and `fix/<description>` branched from `develop`, merged through Pull Requests.
- **Commits:** small, with a conventional prefix, e.g. `feat(incidents): add priority filter`.

## Team

Tasks, phases and integration points: [docs/WORK_PLAN.md](docs/WORK_PLAN.md).

| Member | Module |
|---|---|
| | A: Data & analytics (RF06, RF07, data model) |
| Nic-tolb | B: Management & operations (RF01, RF02, RF08) |
| Mic022 | C: AI & RAG (RF03, RF04, RF05) |
