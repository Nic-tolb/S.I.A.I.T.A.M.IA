package com.campuslands.incidents.service.assistance;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

import com.campuslands.incidents.client.N8nClient;
import com.campuslands.incidents.config.N8nProperties;
import com.campuslands.incidents.config.RagProperties;
import com.campuslands.incidents.dto.assistance.AssistanceAnswer;
import com.campuslands.incidents.dto.assistance.AssistanceResponse;
import com.campuslands.incidents.dto.assistance.AssistanceSource;
import com.campuslands.incidents.dto.rag.RagRequest;
import com.campuslands.incidents.dto.rag.RagResponse;
import com.campuslands.incidents.entity.AiQuery;
import com.campuslands.incidents.entity.AiResponse;
import com.campuslands.incidents.entity.Incident;
import com.campuslands.incidents.entity.KnowledgeArticle;
import com.campuslands.incidents.exception.AiServiceException;
import com.campuslands.incidents.exception.ResourceNotFoundException;
import com.campuslands.incidents.repository.AiQueryRepository;
import com.campuslands.incidents.repository.AiResponseRepository;
import com.campuslands.incidents.repository.IncidentRepository;
import com.campuslands.incidents.repository.KnowledgeArticleRepository;

import tools.jackson.databind.ObjectMapper;

/**
 * RAG assistant (RF03): asks n8n flow 2 for a solution to an incident and records the outcome.
 *
 * <ol>
 *   <li>The query is saved as {@code PENDING} before calling n8n, so the history keeps every attempt.</li>
 *   <li>Threshold and top-k are sent on every call from the Spring Boot configuration.</li>
 *   <li>Every source returned by n8n is re-validated against the database: it must exist and be active
 *       and verified. Unusable sources are dropped; if none remains, the query ends as {@code NO_RESULTS}
 *       because the answer was built on articles the system may not cite.</li>
 *   <li>AI failures never throw: the query ends as {@code ERROR} with the message, and the caller gets
 *       that status (mandatory test 8).</li>
 * </ol>
 */
@Service
public class AiQueryService {

    static final String EMPTY_RESPONSE = "AI service returned an empty response";
    static final String UNEXPECTED_STATUS = "AI service returned an unexpected status: ";

    private static final Logger log = LoggerFactory.getLogger(AiQueryService.class);

    private final IncidentRepository incidentRepository;
    private final AiQueryRepository aiQueryRepository;
    private final AiResponseRepository aiResponseRepository;
    private final KnowledgeArticleRepository articleRepository;
    private final N8nClient n8nClient;
    private final N8nProperties n8nProperties;
    private final RagProperties ragProperties;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public AiQueryService(IncidentRepository incidentRepository,
                          AiQueryRepository aiQueryRepository,
                          AiResponseRepository aiResponseRepository,
                          KnowledgeArticleRepository articleRepository,
                          N8nClient n8nClient,
                          N8nProperties n8nProperties,
                          RagProperties ragProperties,
                          TransactionTemplate transactionTemplate,
                          ObjectMapper objectMapper) {
        this.incidentRepository = incidentRepository;
        this.aiQueryRepository = aiQueryRepository;
        this.aiResponseRepository = aiResponseRepository;
        this.articleRepository = articleRepository;
        this.n8nClient = n8nClient;
        this.n8nProperties = n8nProperties;
        this.ragProperties = ragProperties;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Not transactional as a whole on purpose: the {@code PENDING} query is committed before the
     * (slow) n8n call, and no database connection is held while the model answers.
     */
    public AssistanceResponse assist(Long incidentId, String description) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", incidentId));
        AiQuery query = aiQueryRepository.save(new AiQuery(incident, description.trim()));

        RagResponse rag;
        try {
            rag = n8nClient.post(n8nProperties.ragPath(),
                    new RagRequest(query.getId(), query.getQuestion(),
                            ragProperties.similarityThreshold(), ragProperties.topK()),
                    RagResponse.class);
        } catch (AiServiceException | RestClientException e) {
            // N8nClient already logged the HTTP details
            return fail(query, e.getMessage());
        }

        if (rag == null || rag.status() == null) {
            return fail(query, EMPTY_RESPONSE);
        }
        return switch (rag.status()) {
            case "ANSWERED" -> complete(query, rag);
            case "NO_RESULTS" -> noResults(query);
            case "ERROR" -> fail(query, rag.error() != null ? rag.error() : "AI service reported an error");
            default -> fail(query, UNEXPECTED_STATUS + rag.status());
        };
    }

    private AssistanceResponse complete(AiQuery query, RagResponse rag) {
        List<ValidatedSource> valid = new ArrayList<>();
        for (AssistanceSource source : rag.sources()) {
            articleRepository.findById(source.articleId())
                    .filter(KnowledgeArticle::isUsableAsSource)
                    .ifPresentOrElse(
                            article -> valid.add(new ValidatedSource(article, source.similarity())),
                            () -> log.warn("Query {}: dropping source {} (missing, inactive or unverified)",
                                    query.getId(), source.articleId()));
        }
        if (valid.isEmpty() || rag.answer() == null) {
            log.warn("Query {}: answer discarded, no usable source left", query.getId());
            return noResults(query);
        }

        String answerJson = objectMapper.writeValueAsString(rag.answer());
        return transactionTemplate.execute(status -> {
            AiQuery managed = aiQueryRepository.findById(query.getId()).orElseThrow();
            AiResponse response = new AiResponse(managed, answerJson);
            valid.forEach(v -> response.addSource(v.article(), v.similarity()));
            aiResponseRepository.save(response);
            managed.markAnswered();
            return toResponse(managed, rag.answer(), valid.stream().map(ValidatedSource::toDto).toList());
        });
    }

    private AssistanceResponse noResults(AiQuery query) {
        query.markNoResults();
        return toResponse(aiQueryRepository.save(query), null, List.of());
    }

    private AssistanceResponse fail(AiQuery query, String message) {
        log.warn("Query {} failed: {}", query.getId(), message);
        query.markError(message);
        return toResponse(aiQueryRepository.save(query), null, List.of());
    }

    private static AssistanceResponse toResponse(AiQuery query, AssistanceAnswer answer, List<AssistanceSource> sources) {
        return new AssistanceResponse(query.getId(), query.getIncident().getId(), query.getStatus(),
                answer, sources, query.getErrorMessage(), query.getCreatedAt());
    }

    private record ValidatedSource(KnowledgeArticle article, double similarity) {
        AssistanceSource toDto() {
            return new AssistanceSource(article.getId(), article.getTitle(), similarity);
        }
    }
}
