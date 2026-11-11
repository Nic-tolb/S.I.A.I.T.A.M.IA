-- =====================================================================
-- 01_schema.sql — Relational schema (PostgreSQL)
-- Runs automatically the first time the postgres volume is created.
-- Hibernate runs in "validate" mode: JPA entities must match this script.
--
-- Domain values (agreed by the team):
--   Incident status:   OPEN, IN_PROGRESS, RESOLVED, CLOSED
--                      RESOLVED and CLOSED count as resolved (both have resolved_at).
--   Incident priority: P1 (critical) > P2 (high) > P3 (medium) > P4 (low)
--   AI query status:   PENDING, ANSWERED, NO_RESULTS, ERROR
--   Knowledge article: used as a RAG source only if active AND verified.
-- =====================================================================

-- ---------------------------------------------------------------------
-- users  ("user" is a reserved word in PostgreSQL)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id    BIGSERIAL    PRIMARY KEY,
    name  VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    area  VARCHAR(100) NOT NULL
);

-- ---------------------------------------------------------------------
-- categories
-- ---------------------------------------------------------------------
CREATE TABLE categories (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- ---------------------------------------------------------------------
-- incidents
-- Categories and users cannot be deleted while they have incidents
-- (RESTRICT): deleting them would erase analytics history.
-- ---------------------------------------------------------------------
CREATE TABLE incidents (
    id          BIGSERIAL    PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    description TEXT         NOT NULL,
    category_id BIGINT       NOT NULL REFERENCES categories (id) ON DELETE RESTRICT,
    user_id     BIGINT       NOT NULL REFERENCES users (id)      ON DELETE RESTRICT,
    priority    VARCHAR(2)   NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ,

    CONSTRAINT chk_incidents_priority CHECK (priority IN ('P1', 'P2', 'P3', 'P4')),
    CONSTRAINT chk_incidents_status   CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    CONSTRAINT chk_incidents_resolved_after_created
        CHECK (resolved_at IS NULL OR resolved_at >= created_at),
    -- resolved_at is set if and only if the incident is resolved:
    -- keeps "resolution rate" and "average resolution time" consistent.
    CONSTRAINT chk_incidents_resolved_at_matches_status
        CHECK ((status IN ('RESOLVED', 'CLOSED')) = (resolved_at IS NOT NULL))
);

-- Indexes for the analytics filters (date, category, priority, status)
CREATE INDEX idx_incidents_created_at  ON incidents (created_at);
CREATE INDEX idx_incidents_category_id ON incidents (category_id);
CREATE INDEX idx_incidents_priority    ON incidents (priority);
CREATE INDEX idx_incidents_status      ON incidents (status);
CREATE INDEX idx_incidents_user_id     ON incidents (user_id);

-- ---------------------------------------------------------------------
-- knowledge_articles
-- Articles are deactivated, not deleted (RF02, test 6).
-- ---------------------------------------------------------------------
CREATE TABLE knowledge_articles (
    id         BIGSERIAL    PRIMARY KEY,
    title      VARCHAR(200) NOT NULL,
    problem    TEXT         NOT NULL,
    symptoms   TEXT         NOT NULL,
    cause      TEXT         NOT NULL,
    solution   TEXT         NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Speeds up "which articles can be used as sources"
CREATE INDEX idx_knowledge_articles_usable
    ON knowledge_articles (id) WHERE active AND verified;

-- ---------------------------------------------------------------------
-- ai_queries
-- Deleting an incident deletes its AI history (CASCADE down the chain).
-- ---------------------------------------------------------------------
CREATE TABLE ai_queries (
    id            BIGSERIAL   PRIMARY KEY,
    incident_id   BIGINT      NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    question      TEXT        NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_ai_queries_status
        CHECK (status IN ('PENDING', 'ANSWERED', 'NO_RESULTS', 'ERROR')),
    -- An empty description must never reach the AI flow (test 7)
    CONSTRAINT chk_ai_queries_question_not_blank CHECK (btrim(question) <> '')
);

CREATE INDEX idx_ai_queries_incident_id ON ai_queries (incident_id);
CREATE INDEX idx_ai_queries_status      ON ai_queries (status);

-- ---------------------------------------------------------------------
-- ai_responses  (at most one response per query)
-- ---------------------------------------------------------------------
CREATE TABLE ai_responses (
    id         BIGSERIAL   PRIMARY KEY,
    query_id   BIGINT      NOT NULL UNIQUE REFERENCES ai_queries (id) ON DELETE CASCADE,
    answer     TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- response_sources
-- An article already used as a source cannot be deleted (RESTRICT):
-- the history must keep its sources. Use deactivation instead.
-- ---------------------------------------------------------------------
CREATE TABLE response_sources (
    id          BIGSERIAL        PRIMARY KEY,
    response_id BIGINT           NOT NULL REFERENCES ai_responses (id)       ON DELETE CASCADE,
    article_id  BIGINT           NOT NULL REFERENCES knowledge_articles (id) ON DELETE RESTRICT,
    similarity  DOUBLE PRECISION NOT NULL,

    CONSTRAINT chk_response_sources_similarity CHECK (similarity BETWEEN 0 AND 1),
    CONSTRAINT uq_response_sources_response_article UNIQUE (response_id, article_id)
);

CREATE INDEX idx_response_sources_article_id ON response_sources (article_id);

-- ---------------------------------------------------------------------
-- ratings  (one rating per response, score 1..5)
-- ---------------------------------------------------------------------
CREATE TABLE ratings (
    id          BIGSERIAL    PRIMARY KEY,
    response_id BIGINT       NOT NULL UNIQUE REFERENCES ai_responses (id) ON DELETE CASCADE,
    score       INTEGER      NOT NULL,
    comment     VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_ratings_score CHECK (score BETWEEN 1 AND 5)
);
