# Entity-relationship diagram

Source of truth: [`database/init/01_schema.sql`](../../database/init/01_schema.sql). JPA entities in `backend/src/main/java/com/campuslands/incidents/entity/` match it (Hibernate runs in `validate` mode).

```mermaid
erDiagram
    USERS ||--o{ INCIDENTS : "is responsible for"
    CATEGORIES ||--o{ INCIDENTS : "classifies"
    INCIDENTS ||--o{ AI_QUERIES : "receives"
    AI_QUERIES ||--o| AI_RESPONSES : "is answered by"
    AI_RESPONSES ||--o{ RESPONSE_SOURCES : "cites"
    KNOWLEDGE_ARTICLES ||--o{ RESPONSE_SOURCES : "is used as"
    AI_RESPONSES ||--o| RATINGS : "is rated by"

    USERS {
        bigint id PK
        varchar name "100, not null"
        varchar email UK "150, not null"
        varchar area "100, not null"
    }

    CATEGORIES {
        bigint id PK
        varchar name UK "80, not null"
        varchar description "255"
    }

    INCIDENTS {
        bigint id PK
        varchar title "200, not null"
        text description "not null"
        bigint category_id FK "not null, indexed"
        bigint user_id FK "not null, indexed"
        varchar priority "P1 | P2 | P3 | P4, indexed"
        varchar status "OPEN | IN_PROGRESS | RESOLVED | CLOSED, indexed"
        timestamptz created_at "not null, indexed"
        timestamptz updated_at "not null"
        timestamptz resolved_at "set only if RESOLVED or CLOSED"
    }

    KNOWLEDGE_ARTICLES {
        bigint id PK
        varchar title "200, not null"
        text problem "not null"
        text symptoms "not null"
        text cause "not null"
        text solution "not null"
        boolean active "default true"
        boolean verified "default false"
        timestamptz created_at "not null"
        timestamptz updated_at "not null"
    }

    AI_QUERIES {
        bigint id PK
        bigint incident_id FK "not null, indexed"
        text question "not blank"
        varchar status "PENDING | ANSWERED | NO_RESULTS | ERROR"
        text error_message "only when ERROR"
        timestamptz created_at "not null"
    }

    AI_RESPONSES {
        bigint id PK
        bigint query_id FK, UK "one response per query"
        text answer "not null"
        timestamptz created_at "not null"
    }

    RESPONSE_SOURCES {
        bigint id PK
        bigint response_id FK "unique with article_id"
        bigint article_id FK "indexed"
        double similarity "0 to 1"
    }

    RATINGS {
        bigint id PK
        bigint response_id FK, UK "one rating per response"
        integer score "1 to 5"
        varchar comment "500"
        timestamptz created_at "not null"
    }
```

## Relationships and delete rules

| Relationship | Cardinality | On delete | Reason |
|---|---|---|---|
| users → incidents | 1 : N | RESTRICT | Deleting a user would erase analytics history |
| categories → incidents | 1 : N | RESTRICT | Same: categories feed most indicators |
| incidents → ai_queries | 1 : N | CASCADE | AI history belongs to the incident |
| ai_queries → ai_responses | 1 : 0..1 | CASCADE | A query has at most one response (none while PENDING) |
| ai_responses → response_sources | 1 : N | CASCADE | Sources belong to the response |
| knowledge_articles → response_sources | 1 : N | RESTRICT | An article used as a source must stay in the history; deactivate it instead |
| ai_responses → ratings | 1 : 0..1 | CASCADE | One rating per response (RF08) |

## Integrity rules (CHECK constraints)

| Table | Rule |
|---|---|
| incidents | `priority` in P1–P4, `status` in the 4 values |
| incidents | `resolved_at >= created_at` |
| incidents | `resolved_at` is not null **if and only if** status is RESOLVED or CLOSED |
| knowledge_articles | Used as a RAG source only if `active AND verified` (partial index) |
| ai_queries | `status` in PENDING, ANSWERED, NO_RESULTS, ERROR; `question` not blank (test 7) |
| response_sources | `similarity` between 0 and 1; an article appears once per response |
| ratings | `score` between 1 and 5; one rating per response |
