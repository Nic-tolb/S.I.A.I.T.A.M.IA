# Sistema inteligente de análisis de incidentes tecnológicos y asistencia mediante IA

> Proyecto #1 · Curso: Inteligencia Artificial 2 (Campuslands)

## 1. Contexto del proyecto

Una organización cuenta con un departamento de soporte tecnológico que recibe diariamente solicitudes relacionadas con problemas de software, hardware, conectividad, aplicaciones y servicios informáticos.

Actualmente, los incidentes se registran, pero la información histórica no se aprovecha adecuadamente. Los técnicos resuelven repetidamente problemas similares, la organización desconoce las principales causas de fallas y no dispone de herramientas inteligentes para consultar soluciones previamente documentadas.

La organización necesita una plataforma capaz de centralizar los incidentes, analizar la información histórica e identificar soluciones relevantes mediante inteligencia artificial.

## 2. Objetivo general

Desarrollar una aplicación web que permita gestionar incidentes tecnológicos, analizar datos históricos, identificar tendencias y proporcionar asistencia inteligente mediante embeddings, búsqueda semántica y arquitectura RAG.

## 3. Tecnologías obligatorias

Se conserva la arquitectura tecnológica del proyecto aprobado.

| Componente | Tecnología | Responsabilidad |
|---|---|---|
| Frontend | HTML, CSS, JavaScript | Formularios, reportes y visualización |
| Backend | Según ruta | API REST y lógica de negocio |
| Base relacional | PostgreSQL o MySQL | Incidentes, usuarios y registros |
| Base vectorial | Qdrant | Embeddings de soluciones |
| Automatización | n8n | Orquestación e indexación |
| Modelos de IA | OpenRouter o indicación del trainer | Embeddings y generación |
| Arquitectura inteligente | RAG | Respuestas fundamentadas |
| Analítica | SQL y gráficos JS | Indicadores y tendencias |
| Infraestructura | Docker Compose | Ejecución de servicios |

## 4. Arquitectura propuesta

```
                 Frontend
          (HTML · CSS · JavaScript)
                    │
                    ▼
                Spring Boot
       (API REST · Validaciones · DTO)
                    │
          ┌─────────┴─────────┐
          ▼                   ▼
   Base relacional           n8n
 (Datos + análisis SQL)   (Flujos RAG)
                              │
                              ▼
                       Orquestación IA
                              │
                     ┌────────┴────────┐
                     ▼                 ▼
                  Qdrant           OpenRouter
```

## 5. Requerimientos funcionales

### RF01 – Gestión de incidentes
Registrar incidentes con título, descripción, categoría, prioridad, estado, fecha de creación y usuario responsable. Permitir consultar, filtrar y actualizar su estado.

### RF02 – Base de conocimiento
Administrar artículos técnicos con título, descripción del problema, síntomas, causa y procedimiento de solución. Solo las soluciones activas y verificadas podrán emplearse como fuentes.

### RF03 – Generación de embeddings
Crear un flujo en n8n que consulte los artículos técnicos, genere sus representaciones vectoriales con OpenRouter y las almacene en Qdrant. Evitar duplicados y permitir actualización.

### RF04 – Búsqueda semántica
A partir de la descripción de un incidente, generar su embedding, consultar Qdrant y recuperar entre tres y cinco soluciones relevantes con similitud y metadatos. Aplicar un umbral configurable y justificado.

### RF05 – Asistente RAG
Generar orientaciones técnicas utilizando exclusivamente los documentos recuperados. La respuesta debe incluir la solución sugerida, pasos recomendados, fuentes y similitudes. Cuando no existan fuentes relevantes, deberá reconocerlo.

### RF06 – Análisis de datos
Implementar consultas analíticas sobre los incidentes históricos. Calcular tendencias, distribuciones, porcentajes de resolución, tiempos promedio de atención y categorías con mayores problemas.

### RF07 – Dashboard analítico
Mostrar indicadores y gráficos interactivos filtrados por fechas, categorías y prioridades. Los resultados deben provenir de datos almacenados y cálculos verificables, no de cifras inventadas por la IA.

### RF08 – Historial y evaluación
Guardar cada consulta inteligente, la respuesta, las fuentes utilizadas, el estado y la fecha. Permitir calificar la utilidad de la respuesta de uno a cinco.

## 6. Módulo obligatorio de análisis de datos

Esta es la principal diferencia frente a RutaIA: el análisis de datos no será únicamente un conjunto de estadísticas básicas, sino un módulo central del proyecto.

Cada equipo deberá implementar al menos los siguientes indicadores:

| Indicador | Análisis esperado |
|---|---|
| Total de incidentes | Distribución por periodo |
| Incidentes por categoría | Frecuencia y porcentajes |
| Tendencia temporal | Evolución diaria o semanal |
| Tasa de resolución | Incidentes resueltos sobre total |
| Tiempo promedio de resolución | Diferencias por categoría |
| Categoría más recurrente | Identificación de problemas frecuentes |
| Incidentes por prioridad | Distribución del nivel de urgencia |

## 7. Modelo de datos mínimo

Se proponen las siguientes entidades relacionales:

- **Usuario:** id, nombre, correo y área.
- **Categoria:** id, nombre y descripción.
- **Incidente:** id, título, descripción, categoría, prioridad, estado, fechas y usuario.
- **ArticuloConocimiento:** id, título, problema, síntomas, causa, solución, fecha y estado.
- **ConsultaIA:** id, incidente, pregunta, fecha y estado.
- **RespuestaIA:** id, consulta, respuesta y fecha.
- **FuenteRespuesta:** id, respuesta, artículo y similitud.
- **Calificacion:** id, respuesta, puntuación y comentario.

El modelo deberá definir relaciones JPA, restricciones de integridad, índices y claves foráneas.

## 8. Flujos obligatorios de n8n

### Flujo 1 — Indexación de la base de conocimiento

```
Spring Boot → n8n → Embeddings → Qdrant
```

### Flujo 2 — Asistencia inteligente RAG

```
Incidente recibido desde Spring Boot
        ↓
Embedding de la descripción
        ↓
Consulta vectorial en Qdrant
        ↓
Filtrado y construcción del contexto
        ↓
Generación fundamentada con OpenRouter
        ↓
Respuesta, fuentes y similitudes
```

## 9. Datos iniciales obligatorios

Para garantizar un análisis real, cada equipo deberá cargar como mínimo:

| Conjunto | Cantidad mínima |
|---|---:|
| Incidentes históricos | 150 |
| Artículos técnicos documentados | 25 |
| Categorías de incidentes | 5 |
| Usuarios | 10 |
| Estados de incidente | 4 |
| Niveles de prioridad | 4 |

Los incidentes deberán distribuirse en diferentes fechas, categorías, prioridades y estados. Los registros resueltos deberán tener fechas coherentes que permitan calcular tiempos de resolución.

Se permite utilizar datos sintéticos preparados por el equipo, siempre que sean consistentes, estén documentados y permitan reproducir los cálculos.

## 10. Pruebas funcionales obligatorias

1. Registrar un incidente de conectividad a internet y obtener soluciones relevantes mediante RAG.
2. Consultar un error de ejecución en una aplicación y verificar los artículos recuperados.
3. Registrar un incidente sin información suficiente en la base de conocimiento y comprobar que el modelo no invente soluciones.
4. Consultar incidentes de una categoría específica y verificar que los resultados coincidan con la base relacional.
5. Visualizar la evolución semanal y comprobar los valores mediante consultas SQL.
6. Desactivar un artículo y verificar que no vuelva a utilizarse como fuente.
7. Enviar una descripción vacía y comprobar que no se ejecute el flujo inteligente.
8. Simular un fallo en el servicio de embeddings y verificar el manejo de errores.
9. Consultar el historial de asistencias generadas para un incidente.
10. Calificar una respuesta y verificar la persistencia del registro.

## 11. Reglas de negocio esenciales

La aplicación deberá garantizar que todas las consultas inteligentes pasen primero por Spring Boot y que el frontend no consuma directamente n8n, Qdrant ni OpenRouter.

Los artículos utilizados como fuentes deben existir en la base relacional y estar activos. Si no existen resultados semánticos suficientemente relevantes, el sistema deberá informar la ausencia de información en lugar de generar soluciones sin respaldo.

Los datos analíticos se calcularán mediante consultas reales a la base de datos. La información mostrada en el dashboard deberá coincidir con esos resultados.

Las consultas deberán tener estados **Pendiente**, **Respondida**, **Sin resultados** y **Error**. Las claves de API y las credenciales se almacenarán mediante variables de entorno.

## Resultado esperado

Se conservarán los entregables del proyecto original:

- Repositorio GitHub
- Frontend
- Backend Spring Boot
- Script SQL con datos iniciales
- Diagramas entidad-relación y de arquitectura
- Workflows JSON de n8n
- Docker Compose
- `.env.example`
- Evidencias de Swagger o Postman
- Documento de pruebas
- README
- Presentación final

Adicionalmente, cada equipo deberá entregar un informe de análisis de datos que incluya los indicadores calculados, consultas SQL utilizadas, interpretación de resultados y al menos tres conclusiones respaldadas por evidencia.

### Trazabilidad del desarrollo y control de versiones

Cada equipo deberá evidenciar el proceso de construcción del proyecto mediante un historial de commits progresivo, verificable y coherente con las etapas de implementación. El repositorio deberá reflejar el uso de ramas de trabajo (*branches*) para el desarrollo de funcionalidades, corrección de errores e integración de componentes, incluyendo los respectivos *merges* o *pull requests*. Se evaluará la participación individual de los integrantes, la distribución del trabajo, la claridad de los mensajes de commit y la evolución técnica del proyecto. No se considerará suficiente una entrega concentrada en uno o pocos commits realizados al finalizar el desarrollo.
