package com.campuslands.incidents.dto.incident;

import jakarta.validation.constraints.NotNull;

import com.campuslands.incidents.entity.enums.IncidentStatus;

/**
 * Body for {@code PATCH /api/incidents/{id}/status} (RF01).
 * Moving to RESOLVED or CLOSED sets {@code resolvedAt}; moving back to OPEN or IN_PROGRESS clears it.
 */
public record IncidentStatusUpdateRequest(

        @NotNull(message = "El estado es obligatorio")
        IncidentStatus status) {
}
