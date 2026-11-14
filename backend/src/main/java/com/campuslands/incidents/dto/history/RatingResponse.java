package com.campuslands.incidents.dto.history;

import java.time.Instant;

/** Rating of an AI answer (RF08). {@code comment} is {@code null} when none was given. */
public record RatingResponse(
        Long id,
        Long responseId,
        Long queryId,
        Integer score,
        String comment,
        Instant createdAt) {
}
