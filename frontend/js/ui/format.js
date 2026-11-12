// Spanish labels for backend enums and date formatting.
// Enum values come from the backend (IncidentStatus, IncidentPriority, QueryStatus) and must match it.

export const PRIORITY_LABELS = Object.freeze({
    P1: 'Crítica',
    P2: 'Alta',
    P3: 'Media',
    P4: 'Baja',
});

export const STATUS_LABELS = Object.freeze({
    OPEN: 'Abierto',
    IN_PROGRESS: 'En progreso',
    RESOLVED: 'Resuelto',
    CLOSED: 'Cerrado',
});

/** Statuses of an AI query (history view, RF08). */
export const QUERY_STATUS_LABELS = Object.freeze({
    PENDING: 'Pendiente',
    ANSWERED: 'Respondida',
    NO_RESULTS: 'Sin resultados',
    ERROR: 'Error',
});

export const priorityLabel = value => PRIORITY_LABELS[value] ?? value ?? '';
export const statusLabel = value => STATUS_LABELS[value] ?? value ?? '';
export const queryStatusLabel = value => QUERY_STATUS_LABELS[value] ?? value ?? '';

/**
 * Builds <option> elements for a <select> from a labels map (e.g. PRIORITY_LABELS).
 *
 * @param {Object<string, string>} labels
 * @param {string} [emptyLabel] when given, adds a first option with an empty value (e.g. "Todas")
 */
export function optionsFrom(labels, emptyLabel) {
    const options = [];
    if (emptyLabel !== undefined) options.push(new Option(emptyLabel, ''));
    Object.entries(labels).forEach(([value, label]) => options.push(new Option(label, value)));
    return options;
}

/**
 * Small colored label for table cells, e.g. badge(statusLabel(s), `status-${s}`).
 * The variant becomes a CSS class: "badge badge-<variant>".
 */
export function badge(text, variant) {
    const span = document.createElement('span');
    span.className = variant ? `badge badge-${variant.toLowerCase()}` : 'badge';
    span.textContent = text;
    return span;
}

const DATE_TIME = new Intl.DateTimeFormat('es-CO', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit', hour12: false,
});

const DATE_ONLY = new Intl.DateTimeFormat('es-CO', {
    day: '2-digit', month: '2-digit', year: 'numeric',
});

/**
 * Formats an ISO date from the API (UTC, e.g. "2026-10-03T09:15:00Z") in the browser's local time.
 * Returns '' for null so the table shows its empty marker.
 */
export function formatDate(iso) {
    return iso ? DATE_TIME.format(new Date(iso)) : '';
}

export function formatDay(iso) {
    return iso ? DATE_ONLY.format(new Date(iso)) : '';
}
