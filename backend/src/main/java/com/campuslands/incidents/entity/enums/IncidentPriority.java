package com.campuslands.incidents.entity.enums;

/**
 * Urgency level of an incident (RF01). The assignment requires at least 4 priority levels.
 * Values must match the CHECK constraint on {@code incidents.priority} in 01_schema.sql:
 * P1 is the most urgent and P4 the least. The frontend shows them as Crítica, Alta, Media and Baja.
 */
public enum IncidentPriority {
    /** Critical */
    P1,
    /** High */
    P2,
    /** Medium */
    P3,
    /** Low */
    P4
}
