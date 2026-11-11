package com.campuslands.incidents.dto.knowledge;

import java.time.Instant;

/**
 * Knowledge article returned by the API (RF02).
 *
 * @param usableAsSource {@code true} only when the article is active and verified,
 *                       i.e. when the RAG assistant may cite it (same rule as
 *                       {@code KnowledgeArticle#isUsableAsSource()})
 */
public record KnowledgeArticleResponse(
        Long id,
        String title,
        String problem,
        String symptoms,
        String cause,
        String solution,
        boolean active,
        boolean verified,
        boolean usableAsSource,
        Instant createdAt,
        Instant updatedAt) {
}
