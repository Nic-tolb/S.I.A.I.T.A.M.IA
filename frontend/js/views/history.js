// History & rating view (RF08, tests 9 and 10): the AI queries of an incident with their answer,
// sources and similarities, and a 1-5 rating with comment per answer.
// Uses GET /api/incidents, GET /api/incidents/{id}/queries and POST /api/responses/{id}/rating.
// API data is always inserted with textContent.
import { api } from '../api.js';
import { showMessage, showError, clearMessage } from '../ui/messages.js';
import { readForm, showFieldErrors, clearFieldErrors } from '../ui/forms.js';
import { renderTable } from '../ui/table.js';
import { badge, queryStatusLabel, statusLabel, formatDate } from '../ui/format.js';

const SCORE_LABELS = { 5: '5 · Muy útil', 4: '4 · Útil', 3: '3 · Regular', 2: '2 · Poco útil', 1: '1 · Nada útil' };

const PICKER_HTML = `
    <h3>Historial de asistencias</h3>
    <div class="form-grid">
        <div class="field field-wide">
            <label for="history-incident">Incidente</label>
            <select id="history-incident"></select>
        </div>
    </div>
    <div class="form-actions">
        <button type="button" class="btn btn-secondary" data-role="refresh">Actualizar</button>
    </div>`;

/** Called once, the first time the view is opened. */
export async function init(section) {
    const messages = document.createElement('div');
    messages.className = 'messages';

    const pickerCard = document.createElement('div');
    pickerCard.className = 'card';
    pickerCard.innerHTML = PICKER_HTML; // static markup only

    const list = document.createElement('div');
    list.className = 'history-list';

    section.append(messages, pickerCard, list);

    const select = pickerCard.querySelector('select');
    const showHistory = () => loadHistory(select.value, list, messages);

    select.addEventListener('change', showHistory);
    pickerCard.querySelector('[data-role="refresh"]').addEventListener('click', async () => {
        await loadIncidents(select, messages);
        await showHistory();
    });

    await loadIncidents(select, messages);
    await showHistory();
}

/** Keeps the current choice when the list is refreshed. */
async function loadIncidents(select, messages) {
    try {
        const incidents = await api('/incidents');
        const current = select.value;
        select.replaceChildren(new Option('Selecciona un incidente…', ''),
            ...incidents.map(i => new Option(`#${i.id} · ${i.title} (${statusLabel(i.status)})`, i.id)));
        select.value = incidents.some(i => String(i.id) === current) ? current : '';
    } catch (error) {
        showError(messages, error);
    }
}

async function loadHistory(incidentId, list, messages) {
    clearMessage(messages);
    list.replaceChildren();
    if (!incidentId) return;
    try {
        const history = await api(`/incidents/${incidentId}/queries`);
        if (history.length === 0) {
            list.append(text('p', 'table-empty', 'Este incidente todavía no tiene consultas al asistente IA.'));
            return;
        }
        history.forEach(query => list.append(queryCard(query, messages)));
    } catch (error) {
        showError(messages, error);
    }
}

/** One AI query: question, status and date, then the content that depends on the status. */
function queryCard(query, messages) {
    const card = el('article', 'card history-item');

    const header = el('div', 'history-header');
    header.append(
        badge(queryStatusLabel(query.status), `query-${query.status}`),
        text('span', 'muted', `Consulta #${query.queryId} · ${formatDate(query.createdAt)}`));

    card.append(header, labeled('Pregunta', query.question));

    if (query.status === 'ANSWERED' && query.answer) {
        card.append(answerBlock(query.answer), sourcesBlock(query.sources));
        card.append(query.rating ? ratingBlock(query.rating) : ratingForm(query, card, messages));
    } else if (query.status === 'NO_RESULTS') {
        card.append(text('p', 'muted',
            'No se encontraron artículos relevantes en la base de conocimiento, así que la IA no generó una respuesta.'));
    } else if (query.status === 'ERROR') {
        card.append(text('p', 'field-error', `La consulta falló: ${query.errorMessage ?? 'error desconocido'}`));
    } else {
        card.append(text('p', 'muted', 'La consulta todavía está en proceso.'));
    }
    return card;
}

function answerBlock(answer) {
    const block = el('div', 'history-answer');
    block.append(labeled('Solución sugerida', answer.solution));
    if (answer.steps.length > 0) {
        block.append(text('h4', null, 'Pasos recomendados'));
        const steps = el('ol');
        answer.steps.forEach(step => steps.append(text('li', null, step)));
        block.append(steps);
    }
    return block;
}

function sourcesBlock(sources) {
    const block = el('div');
    block.append(text('h4', null, 'Fuentes'));
    const table = el('div');
    renderTable(table, [
        { header: 'Artículo', value: s => `#${s.articleId} · ${s.title}` },
        { header: 'Similitud', value: s => `${(s.similarity * 100).toFixed(1)} %` },
    ], sources, { emptyText: 'Sin fuentes.' });
    block.append(table);
    return block;
}

function ratingBlock(rating) {
    const block = el('div', 'history-rating');
    block.append(text('h4', null, 'Calificación'),
        text('p', null, `${SCORE_LABELS[rating.score] ?? rating.score} · ${formatDate(rating.createdAt)}`));
    if (rating.comment) block.append(text('p', 'muted', rating.comment));
    return block;
}

/** One rating per answer: after rating, the form is replaced by the stored rating. */
function ratingForm(query, card, messages) {
    const form = el('form', 'form-grid history-rating');
    form.noValidate = true;

    const scoreField = field('Calificación', el('select'));
    const score = scoreField.querySelector('select');
    score.name = 'score';
    score.dataset.type = 'number';
    score.append(new Option('Selecciona…', ''),
        ...Object.keys(SCORE_LABELS).reverse().map(value => new Option(SCORE_LABELS[value], value)));

    const commentField = field('Comentario (opcional)', el('textarea'));
    const comment = commentField.querySelector('textarea');
    comment.name = 'comment';
    comment.maxLength = 500;
    commentField.classList.add('field-wide');

    const actions = el('div', 'form-actions field-wide');
    const button = text('button', 'btn', 'Calificar');
    button.type = 'submit';
    actions.append(button);

    form.append(text('h4', 'field-wide', '¿Qué tan útil fue esta respuesta?'), scoreField, commentField, actions);

    form.addEventListener('submit', async event => {
        event.preventDefault();
        clearMessage(messages);
        clearFieldErrors(form);
        button.disabled = true;
        try {
            const rating = await api(`/responses/${query.responseId}/rating`, { method: 'POST', body: readForm(form) });
            form.replaceWith(ratingBlock(rating));
            showMessage(messages, `Calificación guardada para la consulta #${query.queryId}`, 'success');
        } catch (error) {
            const unplaced = showFieldErrors(form, error);
            if (unplaced.length > 0) showMessage(messages, unplaced.join(' '), 'error');
            else if (error?.status !== 400) showError(messages, error);
            button.disabled = false;
        }
    });
    return form;
}

// --- small DOM helpers (text is always set with textContent) ---

function el(tag, className) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    return node;
}

function text(tag, className, content) {
    const node = el(tag, className);
    node.textContent = content;
    return node;
}

function labeled(label, content) {
    const block = el('div', 'history-block');
    block.append(text('h4', null, label), text('p', null, content));
    return block;
}

function field(label, input) {
    const wrapper = el('div', 'field');
    const id = `rating-${crypto.randomUUID()}`;
    input.id = id;
    const labelNode = text('label', null, label);
    labelNode.htmlFor = id;
    wrapper.append(labelNode, input);
    return wrapper;
}
