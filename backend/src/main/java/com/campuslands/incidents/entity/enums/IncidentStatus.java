package com.campuslands.incidents.entity.enums;

/**
 * Incident lifecycle. Values must match {@code chk_incidents_status} in 01_schema.sql.
 */
public enum IncidentStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    /**
     * RESOLVED and CLOSED count as resolved: they have {@code resolved_at}
     * and are included in the resolution rate and average resolution time.
     */
    public boolean isResolved() {
        return this == RESOLVED || this == CLOSED;
    }
}
