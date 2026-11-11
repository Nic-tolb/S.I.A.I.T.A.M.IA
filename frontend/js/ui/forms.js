// Shared form helpers: read a form as an API body and show validation errors next to each field.
import { ApiError } from '../api.js';

/**
 * Reads every named field of `form` into a plain object ready to send to the API.
 * - Text is trimmed; empty fields become null (so optional fields are omitted on purpose).
 * - <input type="number"> and fields with data-type="number" (e.g. a <select> of ids) become numbers.
 * - Checkboxes become true/false.
 */
export function readForm(form) {
    const data = {};
    for (const field of form.elements) {
        if (!field.name || field.disabled) continue;

        if (field.type === 'checkbox') {
            data[field.name] = field.checked;
            continue;
        }

        const raw = field.value.trim();
        if (raw === '') {
            data[field.name] = null;
        } else if (field.type === 'number' || field.dataset.type === 'number') {
            data[field.name] = Number(raw);
        } else {
            data[field.name] = raw;
        }
    }
    return data;
}

/**
 * Fills `form` with the values of `data` (e.g. to edit an existing article).
 * Fields without a matching key are left empty.
 */
export function fillForm(form, data) {
    for (const field of form.elements) {
        if (!field.name) continue;
        const value = data?.[field.name];
        if (field.type === 'checkbox') {
            field.checked = Boolean(value);
        } else {
            field.value = value ?? '';
        }
    }
}

/**
 * Shows the backend validation messages (ApiError.body.fields, already in Spanish) under each field.
 * Returns the messages that do not belong to any field of the form (e.g. "dateRangeValid"),
 * so the view can show them as a general message.
 *
 * @returns {string[]} messages that could not be placed next to a field
 */
export function showFieldErrors(form, error) {
    clearFieldErrors(form);

    const fields = error instanceof ApiError ? error.body?.fields : null;
    if (!fields) return [];

    const unplaced = [];
    Object.entries(fields).forEach(([name, message]) => {
        const field = form.elements.namedItem(name);
        if (!field || !(field instanceof HTMLElement)) {
            unplaced.push(message);
            return;
        }
        field.setAttribute('aria-invalid', 'true');
        const hint = document.createElement('small');
        hint.className = 'field-error';
        hint.textContent = message;
        field.insertAdjacentElement('afterend', hint);
    });
    return unplaced;
}

export function clearFieldErrors(form) {
    form.querySelectorAll('.field-error').forEach(hint => hint.remove());
    form.querySelectorAll('[aria-invalid]').forEach(field => field.removeAttribute('aria-invalid'));
}
