// Incidents view (RF01): register, list, filter and change the status of incidents.
// Uses GET /api/categories, GET /api/users, GET/POST /api/incidents and PATCH /api/incidents/{id}/status.
import { api } from '../api.js';
import { showMessage, showError, clearMessage } from '../ui/messages.js';
import { readForm, showFieldErrors, clearFieldErrors } from '../ui/forms.js';
import { PRIORITY_LABELS, STATUS_LABELS, optionsFrom } from '../ui/format.js';

/** Fired on the section when an incident is created or changed, so the list can reload. */
export const INCIDENTS_CHANGED = 'incidents:changed';

// Static markup only (no API data), so innerHTML is safe here. Names match IncidentRequest.
const FORM_HTML = `
    <h3>Registrar incidente</h3>
    <form class="form-grid" novalidate>
        <div class="field field-wide">
            <label for="incident-title">Título</label>
            <input id="incident-title" name="title" maxlength="200" required>
        </div>
        <div class="field field-wide">
            <label for="incident-description">Descripción</label>
            <textarea id="incident-description" name="description" maxlength="5000" required></textarea>
        </div>
        <div class="field">
            <label for="incident-category">Categoría</label>
            <select id="incident-category" name="categoryId" data-type="number" required></select>
        </div>
        <div class="field">
            <label for="incident-user">Usuario responsable</label>
            <select id="incident-user" name="userId" data-type="number" required></select>
        </div>
        <div class="field">
            <label for="incident-priority">Prioridad</label>
            <select id="incident-priority" name="priority" required></select>
        </div>
        <div class="field">
            <label for="incident-status">Estado</label>
            <select id="incident-status" name="status"></select>
        </div>
        <div class="form-actions field-wide">
            <button type="submit" class="btn">Registrar</button>
            <button type="reset" class="btn btn-secondary">Limpiar</button>
        </div>
    </form>`;

/** Called once, the first time the view is opened. */
export async function init(section) {
    const messages = document.createElement('div');
    messages.className = 'messages';

    const formCard = document.createElement('div');
    formCard.className = 'card';
    formCard.innerHTML = FORM_HTML;

    section.append(messages, formCard);

    const form = formCard.querySelector('form');
    fillStaticOptions(form);
    form.addEventListener('submit', event => submitIncident(event, form, messages, section));
    form.addEventListener('reset', () => clearFieldErrors(form));

    await loadCatalogs(form, messages);
}

/** Priority and status come from the backend enums, shown with Spanish labels. */
function fillStaticOptions(form) {
    form.elements.priority.append(...optionsFrom(PRIORITY_LABELS, 'Selecciona…'));
    form.elements.status.append(...optionsFrom(STATUS_LABELS));
    form.elements.status.value = 'OPEN';
}

/** Categories and users are loaded from the API; an empty choice makes the backend answer 400. */
async function loadCatalogs(form, messages) {
    try {
        const [categories, users] = await Promise.all([api('/categories'), api('/users')]);
        fillSelect(form.elements.categoryId, categories, c => c.name);
        fillSelect(form.elements.userId, users, u => `${u.name} · ${u.area}`);
    } catch (error) {
        showError(messages, error);
    }
}

function fillSelect(select, items, label) {
    select.replaceChildren(new Option('Selecciona…', ''), ...items.map(item => new Option(label(item), item.id)));
}

async function submitIncident(event, form, messages, section) {
    event.preventDefault();
    clearMessage(messages);
    clearFieldErrors(form);

    const button = form.querySelector('button[type="submit"]');
    button.disabled = true; // avoids registering the same incident twice
    try {
        const created = await api('/incidents', { method: 'POST', body: readForm(form) });
        form.reset();
        form.elements.status.value = 'OPEN';
        showMessage(messages, `Incidente #${created.id} registrado`, 'success');
        section.dispatchEvent(new CustomEvent(INCIDENTS_CHANGED, { detail: created }));
    } catch (error) {
        showFormError(form, messages, error);
    } finally {
        button.disabled = false;
    }
}

/**
 * 400: each validation message goes under its field, plus a short summary on top.
 * Anything else (404 unknown category or user, network...) is shown as a general message.
 */
function showFormError(form, messages, error) {
    const unplaced = showFieldErrors(form, error);
    if (unplaced.length > 0) {
        showMessage(messages, unplaced.join(' '), 'error');
    } else if (error?.status === 400) {
        showMessage(messages, 'Revisa los campos marcados.', 'error');
    } else {
        showError(messages, error);
    }
}
