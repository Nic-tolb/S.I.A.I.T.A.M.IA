package com.campuslands.incidents.entity.enums;

/**
 * Urgency level of an incident (RF01). The assignment requires at least 4 priority levels.
 * Must match the CHECK constraint on {@code incidents.priority} in 01_schema.sql.
 */
public enum IncidentPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
