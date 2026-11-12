package com.campuslands.incidents.dto.indexing;

import com.campuslands.incidents.entity.KnowledgeArticle;

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

    /**
     * Builds the payload from a saved article. {@code active} is true only when the article is
     * usable as a RAG source (active and verified), so callers never repeat that rule.
     */
    public static ArticleIndexPayload from(KnowledgeArticle article) {
        return new ArticleIndexPayload(
                article.getId(),
                article.getTitle(),
                article.getProblem(),
                article.getSymptoms(),
                article.getCause(),
                article.getSolution(),
                article.isUsableAsSource());
    }
}
