package com.campuslands.incidents.entity.enums;

/**
 * Lifecycle of an incident (RF01). The assignment requires at least 4 statuses.
 * Must match the CHECK constraint on {@code incidents.status} in 01_schema.sql.
 */
public enum IncidentStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    /** Resolved or closed incidents have a {@code resolved_at} date (used to compute resolution times). */
    public boolean isFinished() {
        return this == RESOLVED || this == CLOSED;
    }
}
