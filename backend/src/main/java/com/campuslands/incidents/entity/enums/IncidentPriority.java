package com.campuslands.incidents.entity.enums;

/**
 * Incident urgency, from most to least urgent.
 * Values must match {@code chk_incidents_priority} in 01_schema.sql.
 */
public enum IncidentPriority {
    /** Critical: service down for many users. */
    P1,
    /** High: important service degraded. */
    P2,
    /** Medium: a user or a non-critical function is affected. */
    P3,
    /** Low: minor issue or request. */
    P4
}
