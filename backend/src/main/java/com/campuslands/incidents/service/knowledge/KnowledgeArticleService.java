package com.campuslands.incidents.service.knowledge;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuslands.incidents.dto.indexing.ArticleIndexPayload;
import com.campuslands.incidents.dto.knowledge.KnowledgeArticleRequest;
import com.campuslands.incidents.dto.knowledge.KnowledgeArticleResponse;
import com.campuslands.incidents.entity.KnowledgeArticle;
import com.campuslands.incidents.exception.ResourceNotFoundException;
import com.campuslands.incidents.repository.KnowledgeArticleRepository;

/**
 * Knowledge base (RF02): article CRUD, activation and deactivation.
 * <ul>
 *   <li>Articles are deactivated, never deleted: past AI answers keep their sources (test 6).</li>
 *   <li>Only active and verified articles can be RAG sources.</li>
 *   <li>Every change publishes {@link KnowledgeArticleChangedEvent}, so the vector store is synced
 *       after the transaction commits ({@link KnowledgeIndexingListener}).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class KnowledgeArticleService {

    /** Most recently edited first. */
    static final Sort LATEST_FIRST = Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"));

    private final KnowledgeArticleRepository articleRepository;
    private final ApplicationEventPublisher events;

    public KnowledgeArticleService(KnowledgeArticleRepository articleRepository, ApplicationEventPublisher events) {
        this.articleRepository = articleRepository;
        this.events = events;
    }

    public List<KnowledgeArticleResponse> findAll() {
        return articleRepository.findAll(LATEST_FIRST).stream()
                .map(KnowledgeArticleService::toResponse)
                .toList();
    }

    public KnowledgeArticleResponse findById(Long id) {
        return toResponse(getArticle(id));
    }

    /** New articles are active; they are unverified unless the request says otherwise. */
    @Transactional
    public KnowledgeArticleResponse create(KnowledgeArticleRequest request) {
        KnowledgeArticle article = new KnowledgeArticle(
                request.title().trim(), request.problem().trim(), request.symptoms().trim(),
                request.cause().trim(), request.solution().trim());
        article.setVerified(request.verifiedOrDefault());
        KnowledgeArticle saved = articleRepository.save(article);
        publishChange(saved);
        return toResponse(saved);
    }

    /**
     * Updates the texts. When {@code verified} is omitted the current value is kept, so editing a
     * verified article does not silently remove it from the RAG sources.
     */
    @Transactional
    public KnowledgeArticleResponse update(Long id, KnowledgeArticleRequest request) {
        KnowledgeArticle article = getArticle(id);
        article.setTitle(request.title().trim());
        article.setProblem(request.problem().trim());
        article.setSymptoms(request.symptoms().trim());
        article.setCause(request.cause().trim());
        article.setSolution(request.solution().trim());
        if (request.verified() != null) {
            article.setVerified(request.verified());
        }
        publishChange(article);
        return toResponse(article);
    }

    @Transactional
    public KnowledgeArticleResponse activate(Long id) {
        return setActive(id, true);
    }

    /** The article stays in the database (history), but is removed from the vector store. */
    @Transactional
    public KnowledgeArticleResponse deactivate(Long id) {
        return setActive(id, false);
    }

    private KnowledgeArticleResponse setActive(Long id, boolean active) {
        KnowledgeArticle article = getArticle(id);
        article.setActive(active);
        publishChange(article);
        return toResponse(article);
    }

    private void publishChange(KnowledgeArticle article) {
        events.publishEvent(new KnowledgeArticleChangedEvent(ArticleIndexPayload.from(article)));
    }

    private KnowledgeArticle getArticle(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeArticle", id));
    }

    static KnowledgeArticleResponse toResponse(KnowledgeArticle article) {
        return new KnowledgeArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getProblem(),
                article.getSymptoms(),
                article.getCause(),
                article.getSolution(),
                article.isActive(),
                article.isVerified(),
                article.isUsableAsSource(),
                article.getCreatedAt(),
                article.getUpdatedAt());
    }
}
