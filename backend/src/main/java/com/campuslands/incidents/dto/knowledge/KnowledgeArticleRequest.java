package com.campuslands.incidents.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for creating or updating a knowledge article (RF02).
 * Activation is not part of this body: it has its own endpoints
 * ({@code PATCH /api/knowledge-articles/{id}/activate} and {@code /deactivate}).
 *
 * @param verified optional; {@code null} keeps it unverified. Only active and verified
 *                 articles can be used as RAG sources.
 */
public record KnowledgeArticleRequest(

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 200, message = "El título no puede superar 200 caracteres")
        String title,

        @NotBlank(message = "El problema es obligatorio")
        @Size(max = 5000, message = "El problema no puede superar 5000 caracteres")
        String problem,

        @NotBlank(message = "Los síntomas son obligatorios")
        @Size(max = 5000, message = "Los síntomas no pueden superar 5000 caracteres")
        String symptoms,

        @NotBlank(message = "La causa es obligatoria")
        @Size(max = 5000, message = "La causa no puede superar 5000 caracteres")
        String cause,

        @NotBlank(message = "La solución es obligatoria")
        @Size(max = 5000, message = "La solución no puede superar 5000 caracteres")
        String solution,

        Boolean verified) {

    /** Verification flag to store: the one sent by the client, or false when it was omitted. */
    public boolean verifiedOrDefault() {
        return Boolean.TRUE.equals(verified);
    }
}
