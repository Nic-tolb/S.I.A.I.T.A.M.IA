package com.campuslands.incidents.dto.history;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for rating an AI answer (RF08): usefulness from 1 to 5 and an optional comment.
 * Limits match ratings.score (CHECK 1..5) and ratings.comment VARCHAR(500).
 */
public record RatingRequest(

        @NotNull(message = "La calificación es obligatoria")
        @Min(value = 1, message = "La calificación mínima es 1")
        @Max(value = 5, message = "La calificación máxima es 5")
        Integer score,

        @Size(max = 500, message = "El comentario no puede superar 500 caracteres")
        String comment) {
}
