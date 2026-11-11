package com.campuslands.incidents.dto.incident;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;

import org.springframework.format.annotation.DateTimeFormat;

import com.campuslands.incidents.entity.enums.IncidentPriority;
import com.campuslands.incidents.entity.enums.IncidentStatus;

/**
 * Query parameters of {@code GET /api/incidents} (RF01). Every field is optional; {@code null} means "no filter".
 * Dates filter on the creation date and are inclusive, e.g. {@code ?from=2026-10-01&to=2026-10-31}.
 */
public record IncidentFilter(

        @Positive(message = "La categoría no es válida")
        Long categoryId,

        IncidentPriority priority,

        IncidentStatus status,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to) {

    @AssertTrue(message = "La fecha inicial no puede ser posterior a la final")
    public boolean isDateRangeValid() {
        return from == null || to == null || !from.isAfter(to);
    }
}
