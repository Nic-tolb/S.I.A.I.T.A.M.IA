import { api } from './api.js';

// Simple hash navigation: each <section class="view"> is a screen.
function showView() {
    const id = (location.hash || '#incidents').slice(1);
    document.querySelectorAll('.view').forEach(s => s.classList.toggle('active', s.id === id));
    document.querySelectorAll('.topbar nav a').forEach(a => a.classList.toggle('active', a.hash === `#${id}`));
}

async function checkApi() {
    const badge = document.getElementById('api-status');
    try {
        const info = await api('/info');
        // User-visible text stays in Spanish
        badge.textContent = `API OK · umbral ${info.similarityThreshold}`;
        badge.className = 'badge ok';
    } catch {
        badge.textContent = 'API sin conexión';
        badge.className = 'badge error';
    }
}

window.addEventListener('hashchange', showView);
showView();
checkApi();
