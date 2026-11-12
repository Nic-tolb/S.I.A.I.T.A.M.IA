package com.campuslands.incidents.service.indexing;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.campuslands.incidents.client.N8nClient;
import com.campuslands.incidents.config.N8nProperties;
import com.campuslands.incidents.dto.indexing.ArticleIndexPayload;
import com.campuslands.incidents.dto.indexing.IndexArticlesRequest;
import com.campuslands.incidents.dto.indexing.IndexingResult;
import com.campuslands.incidents.exception.AiServiceException;

/**
 * {@link IndexingService} backed by n8n flow 1 ({@code POST /webhook/index-articles}).
 * n8n creates the embeddings and upserts or deletes the Qdrant points.
 *
 * <p>Failures are logged and returned in {@link IndexingResult#errors()}, never thrown,
 * so article CRUD keeps working while the AI services are down.
 */
@Service
public class N8nIndexingService implements IndexingService {

    /** Articles per webhook call: keeps each embeddings request small during a full re-index. */
    static final int BATCH_SIZE = 20;

    private static final Logger log = LoggerFactory.getLogger(N8nIndexingService.class);

    private final N8nClient n8nClient;
    private final String indexingPath;

    public N8nIndexingService(N8nClient n8nClient, N8nProperties props) {
        this.n8nClient = n8nClient;
        this.indexingPath = props.indexingPath();
    }

    @Override
    public IndexingResult syncArticle(ArticleIndexPayload article) {
        return send(List.of(article));
    }

    @Override
    public IndexingResult removeArticle(long articleId) {
        return send(List.of(new ArticleIndexPayload(articleId, null, null, null, null, null, false)));
    }

    @Override
    public IndexingResult reindexAll(List<ArticleIndexPayload> articles) {
        if (articles == null || articles.isEmpty()) {
            return IndexingResult.empty();
        }
        int indexed = 0;
        int deleted = 0;
        List<String> errors = new ArrayList<>();
        // A failed batch does not stop the others: re-indexing is best effort and can be repeated
        for (int from = 0; from < articles.size(); from += BATCH_SIZE) {
            List<ArticleIndexPayload> batch = articles.subList(from, Math.min(from + BATCH_SIZE, articles.size()));
            IndexingResult result = send(batch);
            indexed += result.indexed();
            deleted += result.deleted();
            errors.addAll(result.errors());
        }
        log.info("Re-index finished: {} indexed, {} deleted, {} errors", indexed, deleted, errors.size());
        return new IndexingResult(indexed, deleted, errors);
    }

    private IndexingResult send(List<ArticleIndexPayload> articles) {
        List<Long> ids = articles.stream().map(ArticleIndexPayload::id).toList();
        try {
            IndexingResult result = n8nClient.post(indexingPath, new IndexArticlesRequest(articles), IndexingResult.class);
            if (result == null) {
                log.warn("Indexing flow returned an empty response for articles {}", ids);
                return IndexingResult.failed("Indexing flow returned an empty response for articles " + ids);
            }
            if (!result.successful()) {
                log.warn("Indexing flow reported errors for articles {}: {}", ids, result.errors());
            }
            return result;
        } catch (AiServiceException | RestClientException e) {
            // N8nClient already logged the HTTP details
            log.warn("Could not index articles {}: {}", ids, e.getMessage());
            return IndexingResult.failed("Could not index articles " + ids + ": " + e.getMessage());
        }
    }
}
