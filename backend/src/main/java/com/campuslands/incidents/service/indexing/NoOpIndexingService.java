package com.campuslands.incidents.service.indexing;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.campuslands.incidents.dto.indexing.ArticleIndexPayload;
import com.campuslands.incidents.dto.indexing.IndexingResult;

/**
 * Temporary implementation that does nothing, so the knowledge base module can call
 * {@link IndexingService} before the n8n indexing flow exists.
 * It will be replaced by the n8n-backed implementation (feature/n8n-indexing).
 */
@Service
public class NoOpIndexingService implements IndexingService {

    private static final Logger log = LoggerFactory.getLogger(NoOpIndexingService.class);

    @Override
    public IndexingResult syncArticle(ArticleIndexPayload article) {
        log.info("Indexing not implemented yet: skipping sync of article {} (active={})", article.id(), article.active());
        return IndexingResult.empty();
    }

    @Override
    public IndexingResult removeArticle(long articleId) {
        log.info("Indexing not implemented yet: skipping removal of article {}", articleId);
        return IndexingResult.empty();
    }

    @Override
    public IndexingResult reindexAll(List<ArticleIndexPayload> articles) {
        log.info("Indexing not implemented yet: skipping re-index of {} articles", articles.size());
        return IndexingResult.empty();
    }
}
