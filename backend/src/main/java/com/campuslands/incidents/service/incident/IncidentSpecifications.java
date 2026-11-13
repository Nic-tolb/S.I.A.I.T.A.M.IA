package com.campuslands.incidents.service.incident;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.campuslands.incidents.dto.incident.IncidentFilter;
import com.campuslands.incidents.entity.Incident;
import com.campuslands.incidents.entity.enums.IncidentPriority;
import com.campuslands.incidents.entity.enums.IncidentStatus;

/**
 * Turns the optional filters of {@code GET /api/incidents} into a JPA {@link Specification}.
 * Only the filters the client sent are added; with no filters every incident matches.
 */
final class IncidentSpecifications {

    private IncidentSpecifications() {
    }

    /**
     * @param zone time zone that defines what a "day" is. It must be the same zone the analytics
     *             SQL uses (TZ in .env, America/Bogota), so the list and the dashboard agree.
     */
    static Specification<Incident> fromFilter(IncidentFilter filter, ZoneId zone) {
        List<Specification<Incident>> specs = new ArrayList<>();
        if (filter.categoryId() != null) specs.add(hasCategory(filter.categoryId()));
        if (filter.priority() != null) specs.add(hasPriority(filter.priority()));
        if (filter.status() != null) specs.add(hasStatus(filter.status()));
        if (filter.from() != null) specs.add(createdFrom(filter.from(), zone));
        if (filter.to() != null) specs.add(createdUntil(filter.to(), zone));
        return Specification.allOf(specs);
    }

    static Specification<Incident> hasCategory(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    static Specification<Incident> hasPriority(IncidentPriority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    static Specification<Incident> hasStatus(IncidentStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** From the start of {@code day} (inclusive). */
    static Specification<Incident> createdFrom(LocalDate day, ZoneId zone) {
        Instant start = startOfDay(day, zone);
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), start);
    }

    /** Until the end of {@code day} (inclusive): before the start of the next day. */
    static Specification<Incident> createdUntil(LocalDate day, ZoneId zone) {
        Instant end = startOfNextDay(day, zone);
        return (root, query, cb) -> cb.lessThan(root.get("createdAt"), end);
    }

    /** First instant of {@code day} in {@code zone}, e.g. 2026-10-01 in Bogotá → 2026-10-01T05:00:00Z. */
    static Instant startOfDay(LocalDate day, ZoneId zone) {
        return day.atStartOfDay(zone).toInstant();
    }

    /** Exclusive upper bound of {@code day}: the first instant of the next day in {@code zone}. */
    static Instant startOfNextDay(LocalDate day, ZoneId zone) {
        return startOfDay(day.plusDays(1), zone);
    }
}
