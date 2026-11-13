package com.campuslands.incidents.service.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.campuslands.incidents.dto.indexing.IndexingResult;
import com.campuslands.incidents.service.indexing.IndexingService;

/**
 * Syncs the vector store (Qdrant, through n8n) when a knowledge article changes (RF02 → RF03).
 *
 * Runs only <b>after the article transaction commits</b>, as the {@link IndexingService} contract
 * requires: if the transaction is rolled back, nothing reaches the vector store.
 * Indexing failures never break the article CRUD; they are logged and
 * {@code POST /api/knowledge-articles/reindex} recovers the vector store later.
 */
@Component
public class KnowledgeIndexingListener {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIndexingListener.class);

    private final IndexingService indexingService;

    public KnowledgeIndexingListener(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticleChanged(KnowledgeArticleChangedEvent event) {
        long articleId = event.payload().id();
        try {
            IndexingResult result = indexingService.syncArticle(event.payload());
            if (!result.successful()) {
                log.warn("Article {} was saved but not synced with the vector store: {}", articleId, result.errors());
            }
        } catch (RuntimeException e) {
            // IndexingService should not throw, but the article is already committed: never fail the request
            log.error("Unexpected error syncing article {} with the vector store", articleId, e);
        }
    }
}
