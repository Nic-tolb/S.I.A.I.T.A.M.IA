import { api } from './api.js';

// Hash navigation: each <section class="view" id="..."> is a screen.
// The code of a view lives in js/views/<section id>.js and exports init(section).
// It is loaded the first time the view is opened, so adding a view only needs that file.
const initialized = new Set();

async function initView(section) {
    if (initialized.has(section.id)) return;
    initialized.add(section.id);
    try {
        const module = await import(`./views/${section.id}.js`);
        await module.init?.(section);
    } catch (error) {
        // A view without its file yet (still in another branch) must not break the others
        console.warn(`View "${section.id}" could not be loaded`, error);
    }
}

function showView() {
    const id = (location.hash || '#incidents').slice(1);
    document.querySelectorAll('.view').forEach(section => {
        const active = section.id === id;
        section.classList.toggle('active', active);
        if (active) initView(section);
    });
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
