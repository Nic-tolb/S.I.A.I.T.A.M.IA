# Knowledge base topics

Shared contract between **A** (synthetic incidents) and **C** (knowledge articles and RAG).
Source of truth: [`scripts/catalog.json`](../scripts/catalog.json). If a topic changes, change it there first.

Incidents are generated from the same topics as the articles, so the RAG search finds real matches (tests 1 and 2).

## Article format

Each article is written **in Spanish** and has:

| Field | Content |
|---|---|
| `title` | Suggested title below (can be adjusted) |
| `problem` | What the user experiences, in 1–2 sentences |
| `symptoms` | Observable signs, error messages, when it happens |
| `cause` | Most likely root cause(s) |
| `solution` | Numbered step-by-step procedure |

All 25 are loaded by `02_seed.sql` with `active = true` and `verified = true`, so they can be indexed.
Tip: reuse words from the incident descriptions in `catalog.json` (error codes, symptoms) inside `symptoms`; it raises the similarity of relevant matches.

Delivery: a JSON file `scripts/articles.json` with a list of objects `{ "topic": "<key>", "title", "problem", "symptoms", "cause", "solution" }`. The generator reads it and adds the articles to the seed.

## The 25 articles

| # | Category | Topic key | Suggested title |
|---:|---|---|---|
| 1 | Conectividad | `sin-internet` | Sin acceso a internet en el equipo |
| 2 | Conectividad | `wifi-intermitente` | Conexión Wi-Fi intermitente |
| 3 | Conectividad | `vpn-no-conecta` | La VPN corporativa no conecta |
| 4 | Conectividad | `red-lenta` | Red lenta y alta latencia |
| 5 | Conectividad | `dns-no-resuelve` | Sitios internos no cargan por fallo de DNS |
| 6 | Hardware | `equipo-no-enciende` | El equipo no enciende |
| 7 | Hardware | `impresora-no-imprime` | La impresora de red no imprime |
| 8 | Hardware | `monitor-sin-senal` | Monitor sin señal |
| 9 | Hardware | `perifericos-no-responden` | Teclado o mouse no responden |
| 10 | Hardware | `equipo-sobrecalentamiento` | Sobrecalentamiento y apagados inesperados |
| 11 | Software y sistema operativo | `actualizacion-fallida` | Actualización de Windows fallida |
| 12 | Software y sistema operativo | `equipo-lento` | Equipo lento al iniciar y al trabajar |
| 13 | Software y sistema operativo | `pantalla-azul` | Pantalla azul (BSOD) |
| 14 | Software y sistema operativo | `instalacion-bloqueada` | No se puede instalar software |
| 15 | Software y sistema operativo | `alerta-antivirus` | Alerta de antivirus o archivo en cuarentena |
| 16 | Aplicaciones | `error-ejecucion-app` | Error de ejecución en la aplicación de facturación |
| 17 | Aplicaciones | `app-web-error-500` | Error 500 en aplicación web interna |
| 18 | Aplicaciones | `correo-no-sincroniza` | El correo no sincroniza |
| 19 | Aplicaciones | `archivo-office-corrupto` | Archivo de Office dañado o que no abre |
| 20 | Aplicaciones | `licencia-office` | Licencia de Office expirada o sin activar |
| 21 | Cuentas y accesos | `cuenta-bloqueada` | Cuenta bloqueada o contraseña olvidada |
| 22 | Cuentas y accesos | `mfa-no-llega` | No llega el código de verificación (MFA) |
| 23 | Cuentas y accesos | `permisos-carpeta` | Sin permisos en carpeta compartida |
| 24 | Cuentas y accesos | `acceso-sistema-denegado` | Usuario sin acceso a un sistema interno |
| 25 | Cuentas y accesos | `sesion-expira` | La sesión se cierra constantemente |

## Topics without an article (test 3)

These topics appear in incidents but **must not** have an article. The assistant should answer `NO_RESULTS` for them instead of inventing a solution.

| Category | Topic key | Example incident |
|---|---|---|
| Hardware | `impresora-3d-no-calibra` | La impresora 3D del laboratorio no completa la calibración de la cama |
| Aplicaciones | `software-cad-licencia-red` | El programa de diseño CAD no encuentra el servidor de licencias flotantes |

Watch out: `impresora-no-imprime` (#7) and `licencia-office` (#20) are close in wording. During threshold calibration, check that they do **not** pass the threshold for these two topics; if they do, that is evidence to raise it.

## Relevant mapping to mandatory tests

| Test | Topic |
|---|---|
| 1. Internet connectivity incident | `sin-internet` |
| 2. Runtime error in an application | `error-ejecucion-app` |
| 3. Not enough information | `impresora-3d-no-calibra`, `software-cad-licencia-red` |
| 6. Deactivated article not used | Any, e.g. deactivate `sin-internet` and repeat test 1 |
