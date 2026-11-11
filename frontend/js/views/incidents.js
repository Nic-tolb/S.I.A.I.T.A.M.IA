// Incidents view (RF01): register, list, filter and change the status of incidents.
// Phase 1: form, filters and table, using GET/POST /api/incidents and PATCH /api/incidents/{id}/status.

/** Called once, the first time the view is opened. */
export function init(section) {
    const messages = document.createElement('div');
    messages.className = 'messages';

    const placeholder = document.createElement('p');
    placeholder.className = 'muted';
    placeholder.textContent = 'Vista en construcción.';

    section.append(messages, placeholder);
}
