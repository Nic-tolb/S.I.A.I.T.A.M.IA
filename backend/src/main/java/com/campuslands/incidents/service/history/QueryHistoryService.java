package com.campuslands.incidents.service.history;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuslands.incidents.dto.assistance.AssistanceAnswer;
import com.campuslands.incidents.dto.assistance.AssistanceSource;
import com.campuslands.incidents.dto.history.QueryHistoryResponse;
import com.campuslands.incidents.entity.AiQuery;
import com.campuslands.incidents.entity.AiResponse;
import com.campuslands.incidents.entity.Rating;
import com.campuslands.incidents.exception.ResourceNotFoundException;
import com.campuslands.incidents.repository.AiQueryRepository;
import com.campuslands.incidents.repository.AiResponseRepository;
import com.campuslands.incidents.repository.IncidentRepository;
import com.campuslands.incidents.repository.RatingRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * AI history of an incident (RF08, mandatory test 9): every query with its status and date, the
 * answer, the sources with their similarity and the rating. Read-only: queries are written by the
 * assistant (AiQueryService) and ratings by {@link RatingService}.
 *
 * Whatever the number of queries, the history costs three selects: queries, responses with their
 * sources and articles, and ratings.
 */
@Service
@Transactional(readOnly = true)
public class QueryHistoryService {

    private static final Logger log = LoggerFactory.getLogger(QueryHistoryService.class);

    private final IncidentRepository incidentRepository;
    private final AiQueryRepository queryRepository;
    private final AiResponseRepository responseRepository;
    private final RatingRepository ratingRepository;
    private final ObjectMapper objectMapper;

    public QueryHistoryService(IncidentRepository incidentRepository,
                               AiQueryRepository queryRepository,
                               AiResponseRepository responseRepository,
                               RatingRepository ratingRepository,
                               ObjectMapper objectMapper) {
        this.incidentRepository = incidentRepository;
        this.queryRepository = queryRepository;
        this.responseRepository = responseRepository;
        this.ratingRepository = ratingRepository;
        this.objectMapper = objectMapper;
    }

    /** Newest query first; 404 when the incident does not exist. */
    public List<QueryHistoryResponse> findByIncident(Long incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new ResourceNotFoundException("Incident", incidentId);
        }
        List<AiQuery> queries = queryRepository.findByIncidentIdOrderByCreatedAtDescIdDesc(incidentId);
        if (queries.isEmpty()) {
            return List.of();
        }

        Map<Long, AiResponse> responsesByQuery = responseRepository
                .findByQueryIdIn(queries.stream().map(AiQuery::getId).toList()).stream()
                .collect(Collectors.toMap(r -> r.getQuery().getId(), Function.identity()));

        Map<Long, Rating> ratingsByResponse = responsesByQuery.isEmpty() ? Map.of() : ratingRepository
                .findByResponseIdIn(responsesByQuery.values().stream().map(AiResponse::getId).toList()).stream()
                .collect(Collectors.toMap(r -> r.getResponse().getId(), Function.identity()));

        return queries.stream()
                .map(query -> {
                    AiResponse response = responsesByQuery.get(query.getId());
                    Rating rating = response == null ? null : ratingsByResponse.get(response.getId());
                    return toHistory(incidentId, query, response, rating);
                })
                .toList();
    }

    private QueryHistoryResponse toHistory(Long incidentId, AiQuery query, AiResponse response, Rating rating) {
        return new QueryHistoryResponse(
                query.getId(),
                incidentId,
                query.getQuestion(),
                query.getStatus(),
                query.getCreatedAt(),
                response == null ? null : response.getId(),
                response == null ? null : readAnswer(response),
                response == null ? List.of() : sources(response),
                query.getErrorMessage(),
                rating == null ? null : RatingService.toResponse(rating, response));
    }

    /** Most similar source first. */
    private static List<AssistanceSource> sources(AiResponse response) {
        return response.getSources().stream()
                .map(s -> new AssistanceSource(s.getArticle().getId(), s.getArticle().getTitle(), s.getSimilarity()))
                .sorted(Comparator.comparingDouble(AssistanceSource::similarity).reversed())
                .toList();
    }

    /**
     * The assistant stores the answer as JSON ({@code {"solution": ..., "steps": [...]}}).
     * A corrupt answer must not hide the rest of the history, so it is logged and left empty.
     */
    private AssistanceAnswer readAnswer(AiResponse response) {
        try {
            return objectMapper.readValue(response.getAnswer(), AssistanceAnswer.class);
        } catch (JacksonException e) {
            log.warn("AI response {} has an unreadable answer; it is shown without it", response.getId(), e);
            return null;
        }
    }
}
