package com.campuslands.incidents.service.indexing;

import java.util.List;

import com.campuslands.incidents.dto.indexing.ArticleIndexPayload;
import com.campuslands.incidents.dto.indexing.IndexingResult;

/**
 * Keeps the vector store (Qdrant, through n8n) in sync with the knowledge base (RF03).
 *
 * <h2>Usage rules for callers (knowledge base service)</h2>
 * <ul>
 *   <li>Call {@link #syncArticle} after an article is created, updated, activated, deactivated
 *       or its verification changes. Set {@code active} to {@code true} only when the article
 *       is both active and verified.</li>
 *   <li>Call it <b>after the article transaction commits</b>, so the vector store never
 *       holds an article that was rolled back.</li>
 *   <li>Implementations <b>never throw</b> because of AI-side failures (n8n, embeddings, Qdrant).
 *       They log the problem and report it in {@link IndexingResult#errors()}. Article CRUD must
 *       keep working even when the AI services are down; {@link #reindexAll} recovers later.</li>
 * </ul>
 *
 * Even if the vector store is stale, a deactivated article is never used as a source: the RAG
 * flow re-validates every source against the relational database.
 */
public interface IndexingService {

    /**
     * Upserts the article when {@code active} is {@code true}; removes it otherwise.
     */
    IndexingResult syncArticle(ArticleIndexPayload article);

    /**
     * Removes an article from the vector store (e.g. after a hard delete).
     */
    IndexingResult removeArticle(long articleId);

    /**
     * Full re-index: upserts every active article and removes the inactive ones.
     */
    IndexingResult reindexAll(List<ArticleIndexPayload> articles);
}
