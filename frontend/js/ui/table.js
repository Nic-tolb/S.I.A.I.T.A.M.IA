// Generic table renderer shared by the views (incidents, knowledge base, history...).
// Cell text is set with textContent, so API data cannot inject HTML.

/**
 * @typedef {Object} Column
 * @property {string} header  user-visible header, in Spanish
 * @property {(row: Object) => (string|number|Node|null|undefined)} [value]
 *           cell content; a Node (e.g. a badge) is appended as is, anything else is shown as text
 * @property {Action[]} [actions] when present, the cell contains one button per action
 * @property {string} [className] optional CSS class for the header and the cells
 */

/**
 * @typedef {Object} Action
 * @property {string} label  button text, in Spanish
 * @property {(row: Object) => void} onClick
 * @property {(row: Object) => boolean} [visible] hide the button for some rows
 *           (e.g. "Activar" only for inactive articles)
 * @property {string} [className] optional CSS class for the button
 */

const EMPTY_CELL = '—';

/**
 * Replaces the content of `container` with a table built from `columns` and `rows`.
 *
 * @param {HTMLElement} container
 * @param {Column[]} columns
 * @param {Object[]} rows
 * @param {{ emptyText?: string }} [options]
 */
export function renderTable(container, columns, rows, { emptyText = 'Sin resultados' } = {}) {
    container.replaceChildren();

    if (!rows || rows.length === 0) {
        const empty = document.createElement('p');
        empty.className = 'table-empty';
        empty.textContent = emptyText;
        container.append(empty);
        return;
    }

    const table = document.createElement('table');
    table.className = 'data-table';
    table.append(buildHead(columns), buildBody(columns, rows));
    container.append(table);
}

function buildHead(columns) {
    const thead = document.createElement('thead');
    const tr = document.createElement('tr');
    columns.forEach(col => {
        const th = document.createElement('th');
        th.textContent = col.header;
        if (col.className) th.className = col.className;
        tr.append(th);
    });
    thead.append(tr);
    return thead;
}

function buildBody(columns, rows) {
    const tbody = document.createElement('tbody');
    rows.forEach(row => {
        const tr = document.createElement('tr');
        columns.forEach(col => tr.append(buildCell(col, row)));
        tbody.append(tr);
    });
    return tbody;
}

function buildCell(col, row) {
    const td = document.createElement('td');
    if (col.className) td.className = col.className;

    if (col.actions) {
        col.actions
            .filter(action => !action.visible || action.visible(row))
            .forEach(action => td.append(buildButton(action, row)));
        return td;
    }

    const content = col.value ? col.value(row) : undefined;
    if (content instanceof Node) {
        td.append(content);
    } else {
        td.textContent = content === null || content === undefined || content === '' ? EMPTY_CELL : String(content);
    }
    return td;
}

function buildButton(action, row) {
    const button = document.createElement('button');
    button.type = 'button';
    button.textContent = action.label;
    button.className = action.className ?? 'btn-small';
    button.addEventListener('click', () => action.onClick(row));
    return button;
}
