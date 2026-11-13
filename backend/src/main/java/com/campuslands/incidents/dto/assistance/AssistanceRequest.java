package com.campuslands.incidents.dto.assistance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code POST /api/incidents/{id}/assistance} (RF03). The description is validated here,
 * so an empty one never reaches the AI flow (mandatory test 7). Messages are in Spanish because the
 * frontend shows them.
 */
public record AssistanceRequest(
        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 5000, message = "La descripción no puede superar 5000 caracteres")
        String description) {
}
