# Plan de pruebas — PhishShield Bolivia

Materia: Testing y Aseguramiento de la Calidad. Versión 1.0 (2026-11-24).
Estructura basada en IEEE 829 / ISO/IEC/IEEE 29119-3, simplificada para un proyecto académico.

## 1. Objetivo

Comprobar, con pruebas automatizadas y repetibles, que PhishShield Bolivia:

1. hace lo que promete (detectar phishing dirigido a Bolivia, registrar análisis y alertas,
   administrar empresas y usuarios, controlar el acceso por rol), y
2. lo hace con una calidad aceptable de rendimiento, estabilidad y precisión del detector.

Las pruebas se ejecutan desde el **Centro de Pruebas QA** del propio sistema (`/admin/qa`), que
guarda cada ejecución con sus pasos, evidencias, métricas y reportes (PDF y HTML).

## 2. Alcance

**Dentro del alcance**

| Componente | Qué se prueba |
|---|---|
| API REST (Spring Boot) | Autenticación, análisis de URL, historial, alertas, empresas, panel, control de acceso |
| Interfaz (Angular) | Login, analizador y sus validaciones, historial, alertas, flujo completo |
| Motor de detección | Veredictos de reglas + IA sobre URL bolivianas representativas |
| Servicio de IA (Python) | Disponibilidad, integración con el backend, precisión y recall del modelo solo |
| PostgreSQL | Que lo analizado queda guardado y genera su alerta |
| Atributos de calidad | Tiempo de respuesta, capacidad (estrés), estabilidad (soak), robustez del modelo |
| Reportes por empresa (HU-17) | Pruebas unitarias del dominio y de los casos de uso, y un caso de API |

**Fuera del alcance**: pruebas de seguridad ofensiva (pentesting), pruebas en navegadores
distintos de Chromium, carga distribuida desde varias máquinas, la base de datos real (ver §5),
extensión de navegador y plugin de correo (no existen en el código), y usabilidad con usuarios
reales.

## 3. Tipos de prueba y suites

Los casos se tipifican **solo por tipo de prueba y suite**. Cada suite pertenece a un único tipo
(la base lo garantiza con una restricción CHECK).

| Tipo | Suite | Objetivo | Herramienta | Casos | Cuándo se corre |
|---|---|---|---|---|---|
| Funcional | Smoke | Saber en minutos si vale la pena seguir probando | Rest Assured, Playwright | 6 | Después de cada despliegue o arranque |
| Funcional | Sanity | Que el motor siga dando veredictos razonables en 3 URL clave | Rest Assured | 3 | Después de cambiar reglas, listas o el modelo |
| Funcional | Regression | Que lo que ya funcionaba no se rompa | Rest Assured, Playwright | 9 | Antes de fusionar a `main` |
| Funcional | Integration | El backend con sus vecinos: servicio de IA y PostgreSQL | Rest Assured | 3 | Antes de fusionar a `main` |
| Funcional | E2E | Recorrer como usuario el camino completo de una detección | Playwright | 1 | Antes de una entrega o demo |
| No funcional | Performance | Tiempo de respuesta con carga normal (motor completo y caché) | Generador de carga propio | 2 | Antes de una entrega; tras cambios en el motor |
| No funcional | Estrés | En qué punto se degrada el servicio y si se recupera | Generador de carga propio | 1 | Antes de una entrega |
| No funcional | Soak | Que no se degrade con carga constante en el tiempo | Generador de carga propio | 1 | Antes de una entrega (por defecto 2 min) |
| No funcional | Robustez IA | Precisión, recall y F1 sobre el lote boliviano etiquetado | Lote `qa_lote_robustez` | 2 | Tras reentrenar el modelo o cambiar reglas |

Además del Centro de Pruebas, el proyecto tiene pruebas de desarrollador que corren en cada push
con GitHub Actions: unitarias del dominio y de casos de uso con dobles en memoria (JUnit), de
seguridad de rutas (MockMvc), del frontend (Vitest) y del modelo (pytest).

### 3.1 Cómo se decide el estado de un caso

| Estado | Significado |
|---|---|
| PASS | Todos los pasos y verificaciones se cumplieron |
| FAIL funcional | El sistema respondió, pero no lo esperado: posible **defecto** (se revisa y se registra) |
| FAIL de infraestructura | Algo no respondió (servicio caído, sin conexión): no dice si el sistema funciona |
| SKIPPED | Una precondición de datos no se cumplió en ese momento |
| NOT_RUN | No se ejecutó: falta configuración, la ejecución se detuvo o el caso no está programado |

El asistente de IA (opcional) puede explicar un FAIL, pero **nunca decide** el estado.

## 4. Enfoque y técnicas

- **Caja negra** sobre la API y la interfaz: partición de equivalencia (URL oficial, imitación con
  TLD abusado, typosquatting, URL malformada, URL sin esquema), valores límite en validaciones y
  tablas de decisión del motor (umbrales 40 y 70).
- **Caja blanca** en las pruebas de desarrollador: cobertura de líneas y ramas con JaCoCo.
- **Datos únicos por ejecución**: las URL y empresas que crea una prueba llevan la marca
  `qa-<8 caracteres del id>` para no depender de la caché de 24 h y poder encontrarlas después.
- **Evidencia**: cada paso guarda lo observado; los de API, la petición y la respuesta
  enmascaradas (sin contraseña, tokens ni `Authorization`); los de interfaz, una captura de
  pantalla al final del paso, también cuando falla.
- **Métricas no funcionales**: p50/p95/p99 por rango más cercano, throughput, tasa de error;
  matriz de confusión, precisión, recall, F1 y exactitud.

## 5. Entorno de pruebas

| Elemento | Valor |
|---|---|
| Sistema operativo | Windows 11 Pro |
| Backend | Spring Boot 4.1.1, Java 25 (Temurin), puerto 8080 |
| Frontend | Angular 22, puerto 4200 (`$env:PORT = "4200"`) |
| Servicio de IA | Python 3.14, FastAPI, puerto 8000, modelo `phishshield-url-ia` 1.0.0 |
| Base de datos | PostgreSQL 18 en Docker, **base `phishshield_qa`** (copia de la configuración de detección de la real) |
| Navegador | Chromium headless de Playwright 1.65.1 |
| Usuario de prueba | `qa.admin@phishshield.bo` (rol admin), solo en `phishshield_qa`; contraseña en `backend/.env` |

**Por qué una base aparte**: las pruebas crean empresas, análisis, alertas y miles de filas en las
de carga. El ejecutor se niega a correr si el backend está conectado a una base que no sea `*_qa`
o `prueba_*` (salvo `QA_PERMITIR_BASE_REAL=true`).

Cómo levantar el entorno: ver `docs/qa/PROGRESO_QA.md`, sección "Cómo levantar el entorno de pruebas".

## 6. Criterios

**Criterios de entrada** (para empezar una ejecución)

- Backend, frontend y servicio de IA levantados y respondiendo (`/api/auth/yo` 401, `/salud` 200).
- Backend conectado a `phishshield_qa`; migraciones hasta la última aplicadas.
- Variables `QA_BASE_URL`, `QA_FRONTEND_URL`, `QA_TEST_USER` y `QA_TEST_PASSWORD` definidas.
- Pruebas de desarrollador en verde (`mvnw test`, `npm test`, `pytest`).

**Criterios de salida** (para dar por buena una versión)

- Smoke, Regression, Integration y E2E: 100 % PASS.
- Sanity: 100 % PASS, o cada FAIL registrado como defecto con su severidad y plan.
- No funcionales: los umbrales de cada caso (p95 < 2000 ms y error < 1 % con 20 usuarios;
  recuperación tras el estrés; crecimiento del p95 < 50 % en soak; precisión y recall ≥ 0,80
  del sistema completo).
- Ningún defecto de severidad crítica abierto; los de severidad alta, con decisión documentada.
- Cobertura de líneas del backend ≥ 60 % (JaCoCo).

**Criterios de suspensión**: se detiene la ejecución si un caso Smoke da FAIL de infraestructura
(el entorno no está listo) o si alguien pide detenerla desde la pantalla. Se reanuda cuando el
entorno vuelve a cumplir los criterios de entrada.

## 7. Riesgos y mitigación

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Ensuciar la base real con datos de prueba | Alto | Base `phishshield_qa` y bloqueo del ejecutor si la base no es de pruebas |
| Filtrar la contraseña o tokens en la evidencia | Alto | Enmascarado en el cliente de API; prueba automática que lo comprueba en la base |
| El generador de carga corre en el mismo backend que se prueba | Medio | Se usa para comparar ejecuciones y detectar degradación, no como capacidad absoluta |
| Lote de robustez usado también para ajustar reglas | Medio | TC-ROB-001 es una medida optimista; conviene un lote nuevo para medir generalización |
| Resultados de carga variables entre corridas | Medio | Umbrales con margen; se compara la tendencia entre ejecuciones (el estrés se degradó en el escalón 3 o 4 según la corrida) |
| Caché de análisis de 24 h oculta cambios del motor | Medio | URL marcadas por ejecución en los casos que miden el cálculo |
| Servicio de IA caído | Medio | El caso queda FAIL de infraestructura (no se confunde con un defecto) |
| Sesión de 15 minutos que vence en ejecuciones largas | Bajo | El cliente renueva el token cuando le queda menos de un minuto |
| Variables de correo o de IA no configuradas | Bajo | Son opcionales: la ejecución sigue y la pantalla lo indica |

## 8. Entregables

- Catálogo de 28 casos en `qa_casos` (`database/initdb/04_qa.sql`, migraciones V12, V13 y V15).
- Este plan, la matriz de trazabilidad (`MATRIZ_TRAZABILIDAD.md`), el registro de defectos
  (`REGISTRO_DEFECTOS.md`) y el informe de ejecución (`INFORME_EJECUCION.md`).
- Por cada ejecución: reporte PDF detallado y HTML resumido en `backend/qa-evidencias/{id}/`.
- Integración continua: `.github/workflows/ci.yml`; cobertura en `backend/target/site/jacoco/`.
