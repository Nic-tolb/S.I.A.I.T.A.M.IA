package com.campuslands.incidents.service.indexing;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.campuslands.incidents.dto.indexing.ArticleIndexPayload;
import com.campuslands.incidents.dto.indexing.IndexingResult;
import com.campuslands.incidents.repository.KnowledgeArticleRepository;

/**
 * Full re-index of the knowledge base (RF03): sends every article to the vector store,
 * upserting the usable ones (active and verified) and removing the rest.
 * Used to load the vector store the first time and to recover after indexing failures.
 */
@Service
public class KnowledgeReindexService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeReindexService.class);

    private final KnowledgeArticleRepository articleRepository;
    private final IndexingService indexingService;

    public KnowledgeReindexService(KnowledgeArticleRepository articleRepository, IndexingService indexingService) {
        this.articleRepository = articleRepository;
        this.indexingService = indexingService;
    }

    /**
     * Not transactional on purpose: the articles are read and mapped first, so no database
     * connection is held while n8n creates the embeddings.
     */
    public IndexingResult reindexAll() {
        List<ArticleIndexPayload> articles = articleRepository.findAll(Sort.by("id")).stream()
                .map(ArticleIndexPayload::from)
                .toList();
        log.info("Re-indexing {} knowledge articles", articles.size());
        return indexingService.reindexAll(articles);
    }
}
