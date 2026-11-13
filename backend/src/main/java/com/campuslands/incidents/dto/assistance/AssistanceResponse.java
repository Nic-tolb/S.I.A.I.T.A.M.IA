package com.campuslands.incidents.dto.assistance;

import java.time.Instant;
import java.util.List;

import com.campuslands.incidents.entity.enums.QueryStatus;

/**
 * Result of asking the assistant about an incident (RF03).
 *
 * @param status       {@code ANSWERED}: answer and sources are present. {@code NO_RESULTS}: no usable
 *                     knowledge article matched, so no answer was generated. {@code ERROR}: the AI
 *                     services failed; {@code errorMessage} says why.
 * @param answer       {@code null} unless {@code status} is {@code ANSWERED}
 * @param sources      articles validated against the database; empty unless {@code ANSWERED}
 * @param errorMessage {@code null} unless {@code status} is {@code ERROR}
 */
public record AssistanceResponse(
        Long queryId,
        Long incidentId,
        QueryStatus status,
        AssistanceAnswer answer,
        List<AssistanceSource> sources,
        String errorMessage,
        Instant createdAt) {

    public AssistanceResponse {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
