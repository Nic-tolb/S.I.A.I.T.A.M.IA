package com.campuslands.incidents.dto.history;

import java.time.Instant;
import java.util.List;

import com.campuslands.incidents.dto.assistance.AssistanceAnswer;
import com.campuslands.incidents.dto.assistance.AssistanceSource;
import com.campuslands.incidents.entity.enums.QueryStatus;

/**
 * One AI query of an incident, as shown in the history (RF08): the question, its status, the
 * answer with its sources and similarities, and the rating when there is one.
 *
 * @param responseId   id to rate; {@code null} unless the query was ANSWERED
 * @param answer       {@code null} unless the query was ANSWERED
 * @param sources      articles cited by the answer; empty unless ANSWERED
 * @param errorMessage {@code null} unless the query ended in ERROR
 * @param rating       {@code null} until the answer is rated
 */
public record QueryHistoryResponse(
        Long queryId,
        Long incidentId,
        String question,
        QueryStatus status,
        Instant createdAt,
        Long responseId,
        AssistanceAnswer answer,
        List<AssistanceSource> sources,
        String errorMessage,
        RatingResponse rating) {

    public QueryHistoryResponse {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
