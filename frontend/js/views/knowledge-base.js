// Knowledge base view (RF02): article CRUD, activate and deactivate.
// Phase 1: form and table, using /api/knowledge-articles; shows which articles the AI may cite.

/** Called once, the first time the view is opened. */
export function init(section) {
    const messages = document.createElement('div');
    messages.className = 'messages';

    const placeholder = document.createElement('p');
    placeholder.className = 'muted';
    placeholder.textContent = 'Vista en construcción.';

    section.append(messages, placeholder);
}
