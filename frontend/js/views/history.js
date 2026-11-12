// History & rating view (RF08): AI queries of an incident with answer, sources and similarities,
// and a 1-5 rating with comment per answer.
// Phase 2: uses GET /api/incidents/{id}/queries and the rating endpoint.

/** Called once, the first time the view is opened. */
export function init(section) {
    const messages = document.createElement('div');
    messages.className = 'messages';

    const placeholder = document.createElement('p');
    placeholder.className = 'muted';
    placeholder.textContent = 'Vista en construcción.';

    section.append(messages, placeholder);
}
