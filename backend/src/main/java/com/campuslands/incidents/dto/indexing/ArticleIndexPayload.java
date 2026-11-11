package com.campuslands.incidents.dto.indexing;

/**
 * Knowledge article data sent to the indexing flow (matches the n8n contract in n8n/README.md).
 *
 * @param id       article id; also used as the Qdrant point id, so re-indexing never duplicates
 * @param active   {@code true} only when the article is active <b>and</b> verified (RF02).
 *                 When {@code false}, the article is removed from the vector store.
 */
public record ArticleIndexPayload(
        long id,
        String title,
        String problem,
        String symptoms,
        String cause,
        String solution,
        boolean active) {
}
