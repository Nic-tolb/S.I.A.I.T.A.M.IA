// Shared feedback messages for every view (success, error, info).
// Text is always set with textContent, never innerHTML, so API data cannot inject HTML.
import { ApiError } from '../api.js';

const DEFAULT_TIMEOUT_MS = 4000;

/**
 * Shows a message inside `container`, replacing the previous one.
 * Success and info messages disappear after `timeout` ms; errors stay until replaced or cleared.
 *
 * @param {HTMLElement} container element that holds the message (e.g. <div class="messages">)
 * @param {string} text user-visible text, in Spanish
 * @param {'success'|'error'|'info'} [type='info']
 * @param {{ timeout?: number }} [options] use timeout: 0 to keep the message visible
 */
export function showMessage(container, text, type = 'info', { timeout } = {}) {
    clearMessage(container);

    const box = document.createElement('div');
    box.className = `message message-${type}`;
    box.setAttribute('role', type === 'error' ? 'alert' : 'status');
    box.textContent = text;
    container.append(box);

    const ms = timeout ?? (type === 'error' ? 0 : DEFAULT_TIMEOUT_MS);
    if (ms > 0) {
        container._messageTimer = setTimeout(() => box.remove(), ms);
    }
}

export function clearMessage(container) {
    clearTimeout(container._messageTimer);
    container.querySelectorAll('.message').forEach(m => m.remove());
}

/**
 * Turns any error thrown by api() into a Spanish sentence for the user.
 * The backend's own messages are in English (logs, Swagger), so they are not shown directly.
 */
export function errorText(error) {
    if (!(error instanceof ApiError)) {
        return 'No se pudo conectar con el servidor. Intenta de nuevo.';
    }
    switch (error.status) {
        case 400: return 'Revisa los datos ingresados.';
        case 404: return 'El registro solicitado no existe.';
        case 409: return 'La operación entra en conflicto con datos existentes.';
        case 502: return 'El servicio de IA no está disponible en este momento.';
        default: return 'Ocurrió un error inesperado. Intenta de nuevo.';
    }
}

/** Shortcut for the common case: show the Spanish text of an error. */
export function showError(container, error) {
    showMessage(container, errorText(error), 'error');
}
