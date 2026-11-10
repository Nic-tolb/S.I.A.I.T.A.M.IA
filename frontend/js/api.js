// Single HTTP client for the frontend. It only talks to Spring Boot (/api), never to n8n, Qdrant or OpenRouter.
// With nginx (Docker) /api is same-origin. For Live Server, change to 'http://localhost:8080/api'.
const API_BASE = '/api';

export class ApiError extends Error {
    constructor(status, body) {
        super(body?.message || `HTTP error ${status}`);
        this.status = status;
        this.body = body;
    }
}

export async function api(path, { method = 'GET', body, params } = {}) {
    const url = new URL(API_BASE + path, window.location.origin);
    if (params) {
        Object.entries(params)
            .filter(([, v]) => v !== undefined && v !== null && v !== '')
            .forEach(([k, v]) => url.searchParams.append(k, v));
    }

    const resp = await fetch(url, {
        method,
        headers: body ? { 'Content-Type': 'application/json' } : undefined,
        body: body ? JSON.stringify(body) : undefined,
    });

    const data = resp.status === 204 ? null : await resp.json().catch(() => null);
    if (!resp.ok) throw new ApiError(resp.status, data);
    return data;
}
