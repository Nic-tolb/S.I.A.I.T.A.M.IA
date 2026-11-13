// Incidents view (RF01): register, list, filter and change the status of incidents.
// Uses GET /api/categories, GET /api/users, GET/POST /api/incidents and PATCH /api/incidents/{id}/status.
import { api } from '../api.js';
import { showMessage, showError, clearMessage } from '../ui/messages.js';
import { readForm, showFieldErrors, clearFieldErrors } from '../ui/forms.js';
import { renderTable } from '../ui/table.js';
import {
    PRIORITY_LABELS, STATUS_LABELS, optionsFrom, badge, priorityLabel, statusLabel, formatDate,
} from '../ui/format.js';

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

// Names match IncidentFilter (query parameters of GET /api/incidents).
const FILTERS_HTML = `
    <h3>Incidentes registrados</h3>
    <form class="form-grid" novalidate>
        <div class="field">
            <label for="filter-category">Categoría</label>
            <select id="filter-category" name="categoryId" data-type="number"></select>
        </div>
        <div class="field">
            <label for="filter-priority">Prioridad</label>
            <select id="filter-priority" name="priority"></select>
        </div>
        <div class="field">
            <label for="filter-status">Estado</label>
            <select id="filter-status" name="status"></select>
        </div>
        <div class="field">
            <label for="filter-from">Desde</label>
            <input id="filter-from" name="from" type="date">
        </div>
        <div class="field">
            <label for="filter-to">Hasta</label>
            <input id="filter-to" name="to" type="date">
        </div>
        <div class="form-actions field-wide">
            <button type="submit" class="btn">Filtrar</button>
            <button type="reset" class="btn btn-secondary">Limpiar</button>
        </div>
    </form>
    <div class="table-container"></div>`;

/** Status transitions offered in the table, per current status. */
const STATUS_ACTIONS = [
    { label: 'Iniciar', status: 'IN_PROGRESS', from: ['OPEN'] },
    { label: 'Resolver', status: 'RESOLVED', from: ['OPEN', 'IN_PROGRESS'] },
    { label: 'Cerrar', status: 'CLOSED', from: ['RESOLVED'] },
    { label: 'Reabrir', status: 'OPEN', from: ['RESOLVED', 'CLOSED'] },
];

/** Called once, the first time the view is opened. */
export async function init(section) {
    const messages = document.createElement('div');
    messages.className = 'messages';

    const formCard = document.createElement('div');
    formCard.className = 'card';
    formCard.innerHTML = FORM_HTML;

    const listCard = document.createElement('div');
    listCard.className = 'card';
    listCard.innerHTML = FILTERS_HTML;

    section.append(messages, formCard, listCard);

    const form = formCard.querySelector('form');
    const filters = listCard.querySelector('form');
    const table = listCard.querySelector('.table-container');

    fillStaticOptions(form, filters);
    form.addEventListener('submit', event => submitIncident(event, form, messages, section));
    form.addEventListener('reset', () => clearFieldErrors(form));

    const reload = () => loadIncidents(filters, table, messages, section);
    filters.addEventListener('submit', event => { event.preventDefault(); reload(); });
    filters.addEventListener('reset', () => setTimeout(reload)); // after the browser clears the fields
    section.addEventListener(INCIDENTS_CHANGED, reload);

    await loadCatalogs(form, filters, messages);
    await reload();
}

/** Priority and status come from the backend enums, shown with Spanish labels. */
function fillStaticOptions(form, filters) {
    form.elements.priority.append(...optionsFrom(PRIORITY_LABELS, 'Selecciona…'));
    form.elements.status.append(...optionsFrom(STATUS_LABELS));
    form.elements.status.value = 'OPEN';
    filters.elements.priority.append(...optionsFrom(PRIORITY_LABELS, 'Todas'));
    filters.elements.status.append(...optionsFrom(STATUS_LABELS, 'Todos'));
}

/** Categories and users are loaded from the API; an empty choice makes the backend answer 400. */
async function loadCatalogs(form, filters, messages) {
    try {
        const [categories, users] = await Promise.all([api('/categories'), api('/users')]);
        fillSelect(form.elements.categoryId, categories, c => c.name, 'Selecciona…');
        fillSelect(form.elements.userId, users, u => `${u.name} · ${u.area}`, 'Selecciona…');
        fillSelect(filters.elements.categoryId, categories, c => c.name, 'Todas');
    } catch (error) {
        showError(messages, error);
    }
}

function fillSelect(select, items, label, emptyLabel) {
    select.replaceChildren(new Option(emptyLabel, ''), ...items.map(item => new Option(label(item), item.id)));
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

/** GET /api/incidents with the current filters; empty filters are omitted by api(). */
async function loadIncidents(filters, table, messages, section) {
    clearFieldErrors(filters);
    try {
        const incidents = await api('/incidents', { params: readForm(filters) });
        renderIncidents(table, incidents, messages, section);
    } catch (error) {
        // e.g. "from" after "to" has no field of its own, so it is shown on top
        showFormError(filters, messages, error);
    }
}

function renderIncidents(table, incidents, messages, section) {
    renderTable(table, [
        { header: '#', value: i => i.id },
        { header: 'Título', value: i => i.title },
        { header: 'Categoría', value: i => i.categoryName },
        { header: 'Prioridad', value: i => badge(priorityLabel(i.priority), i.priority) },
        { header: 'Estado', value: i => badge(statusLabel(i.status), `status-${i.status}`) },
        { header: 'Responsable', value: i => i.userName },
        { header: 'Creado', value: i => formatDate(i.createdAt) },
        { header: 'Resuelto', value: i => formatDate(i.resolvedAt) },
        {
            header: 'Acciones',
            actions: STATUS_ACTIONS.map(action => ({
                label: action.label,
                visible: i => action.from.includes(i.status),
                onClick: i => changeStatus(i, action.status, messages, section),
            })),
        },
    ], incidents, { emptyText: 'No hay incidentes que coincidan con los filtros.' });
}

/** PATCH /api/incidents/{id}/status; the list reloads through INCIDENTS_CHANGED. */
async function changeStatus(incident, status, messages, section) {
    clearMessage(messages);
    try {
        const updated = await api(`/incidents/${incident.id}/status`, { method: 'PATCH', body: { status } });
        showMessage(messages, `Incidente #${updated.id} ahora está "${statusLabel(updated.status)}"`, 'success');
        section.dispatchEvent(new CustomEvent(INCIDENTS_CHANGED, { detail: updated }));
    } catch (error) {
        showError(messages, error);
    }
}
