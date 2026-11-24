# Progreso del Centro de Pruebas QA

Bitácora del módulo QA (materia Testing y Aseguramiento de la Calidad). Una sesión nueva
debe poder continuar leyendo solo este archivo y `CLAUDE.md`.

- Rama: `feature/qa-automation` (se sube a `origin` al cerrar cada fase; **nunca** se
  fusiona ni se hace push a `main` sin preguntar).
- Todo lo que corre en vivo va contra la base **`phishshield_qa`**, nunca contra la real.
- Los casos se tipifican solo por **tipo** (funcional / no funcional) y **suite** (smoke,
  sanity, regression, integration, e2e, performance, estrés, soak, robustez IA).

## Estado por fase

| Fase | Qué | Estado |
|---|---|---|
| 1 | Base `qa_*` (04_qa.sql / V12), dominio y puertos | Hecha (d60d8ba) |
| 2 | Motor de ejecución y 14 casos de API | Hecha (5a67d2b) |
| 3 | Reportes PDF y HTML | **Hecha** |
| 4 | Pantalla "Centro de Pruebas" en Angular | Pendiente |
| 5 | 7 casos de interfaz con Playwright | Pendiente |
| 6 | Pruebas no funcionales (carga, estrés, soak, robustez IA) | Pendiente |
| 7 | Correo y asistente de IA (opcionales) | Pendiente |
| 8 | CI, JaCoCo y documentación TQA | Pendiente |
| 9 | HU-17 Reportes por empresa | Pendiente |

## Cómo levantar el entorno de pruebas

1. Base: contenedor `phishshield-db` levantado (`docker ps`).
2. Administrador de prueba (solo la primera vez): `backend\herramientas\crear-usuario-qa.ps1`.
   Crea `qa.admin@phishshield.bo` en `phishshield_qa` con una contraseña aleatoria y la
   guarda en `backend\.env` (ignorado por git). Nunca se muestra.
3. Servicio de IA: dentro de `ia\`, `.\.venv\Scripts\python.exe -m uvicorn servicio:app --port 8000`.
4. Backend contra `phishshield_qa`: `backend\herramientas\levantar-backend-qa.ps1`
   (lee `backend\.env`, toma `DB_PASSWORD` del contenedor y genera `JWT_SECRET`).
5. Probar: `backend\qa.http` (o la pantalla, desde la fase 4).

Variables del Centro de Pruebas (ver `backend\.env.example`):

| Variable | Para qué | Si falta |
|---|---|---|
| `QA_BASE_URL` | Backend que se prueba (`http://localhost:8080`) | Todos los casos de API quedan NOT_RUN |
| `QA_TEST_USER` / `QA_TEST_PASSWORD` | Administrador de prueba | Los casos que necesitan sesión quedan NOT_RUN |
| `QA_FRONTEND_URL` | Frontend que se prueba (fase 5) | Los casos de interfaz quedan NOT_RUN |
| `QA_AMBIENTE` | Nombre del ambiente en los reportes | `local` |
| `QA_EVIDENCIAS_DIR` | Carpeta de evidencias y reportes | `backend\qa-evidencias` |
| `QA_PERMITIR_BASE_REAL` | Permite correr con el backend conectado a la base real | `false`: si la base no es `*_qa` o `prueba_*`, ningún caso corre |
| `IA_URL` | Servicio de IA (TC-INT-001 y robustez) | `http://localhost:8000` |

## Fase 2 — Motor de ejecución y casos de API (2026-11-24)

**Qué se hizo**

- Dependencias (aprobadas): `junit-jupiter` y `junit-platform-launcher` en scope compile
  (versión 6.0.3, la que fija Spring Boot 4.1.1, para no chocar con sus pruebas) y
  `rest-assured` 6.3.0 (última estable).
- Dominio: `OyenteEjecucion.casoNoEjecutado(codigo, motivo)`, `Ejecucion.anotarNoEjecutado`,
  `FinEjecucion` (COMPLETA / DETENIDA), `CasoCatalogo` y `EjecutorPruebasPort.codigosImplementados()`.
  `Ejecucion.fallar` ya no pisa el motivo propio de un caso NOT_RUN.
- `qa/application`: `IniciarEjecucion`, `CorrerEjecucion`, `DetenerEjecucion`,
  `ListarEjecuciones`, `ObtenerDetalleEjecucion`, `ListarCatalogo`,
  `MarcarInterrumpidasAlArrancar` y la interfaz `TrasEjecucion` (pasos posteriores:
  reportes en la fase 3, correo en la 7). Sin anotaciones de Spring.
- `qa/infrastructure/persistence`: `CatalogoCasosJdbcAdapter` y `EjecucionJdbcAdapter`
  (JdbcClient, una transacción por guardado; una ejecución terminada en la base no vuelve a
  quedar activa; pasos y métricas se insertan una vez y nunca se borran).
- `qa/infrastructure/ejecutor`: `JUnitEjecutorPruebasAdapter` (JUnit Platform Launcher, un
  caso por vez en el orden del plan), `@CasoQA`, `ContextoCaso`, `BitacoraCaso`,
  `ClienteApiQA` (Rest Assured; enmascara contraseña, tokens y `Authorization` en la
  evidencia), `ExtensionQA` (entrega el contexto a cada método), `RegistroCasosQA` (lista
  explícita de clases de casos) y `PlanificadorEjecucionAsync` (`@Async` con pool propio de
  un hilo, `qaPoolEjecuciones`).
- Casos de API en `qa/infrastructure/ejecutor/casos/api`: `CasosSmokeApi` (SMK-001..004),
  `CasosSanityApi` (SAN-001..003), `CasosRegressionApi` (REG-003/005/006/008) y
  `CasosIntegrationApi` (INT-001..003).
- Traducción de JUnit: éxito → PASS; `AssertionError` → FAIL funcional; sin conexión /
  timeout / `FalloInfraestructuraException` → FAIL de infraestructura;
  `CasoNoEjecutableException` (falta configuración) → NOT_RUN con el motivo; otra suposición
  no cumplida → SKIPPED; caso sin programar → NOT_RUN "Se implementa en una fase posterior".
- Protección: si el backend está conectado a una base que no es `*_qa` ni `prueba_*`, ningún
  caso corre (NOT_RUN con el motivo), salvo `QA_PERMITIR_BASE_REAL=true`.
- Web: `QaController` (`GET /casos`, `POST /ejecuciones`, `GET /ejecuciones`,
  `GET /ejecuciones/{id}`, `POST /ejecuciones/{id}/detener`), DTOs propios, uuid público como
  id y `QaExceptionHandler` (404, 400, 409). `SeguridadConfig`: `/api/admin/qa/**` solo ADMIN.
- `backend/qa.http`, `backend/.env.example`, `herramientas/crear-usuario-qa.ps1`.
- Migración **V13** (`V13__ruta_catalogo_qa.sql` + `04_qa.sql`): corrige el texto de
  TC-REG-008 (la ruta del catálogo es `/api/admin/qa/casos`). **Aplicada solo en
  `phishshield_qa`**; falta aplicarla en la base real cuando el usuario lo decida.
- Pruebas nuevas: `CasosDeUsoQaTest` (22, dobles en memoria), `JUnitEjecutorPruebasAdapterTest`
  (lanza casos falsos con el Launcher real), `RegistroCasosQATest` (los `@CasoQA` existen en
  `04_qa.sql` y los 14 de API están programados) y la ruta QA en `SeguridadRutasTest`.
- Al cerrar la fase: backend 324 pruebas en verde (antes 293), frontend 94, IA 24; el
  `git grep` de la regla 4 no devuelve nada.

**Verificación real (2026-11-24, contra `phishshield_qa`, backend + IA levantados)**

| Suite | PASS | FAIL | NOT_RUN | Resultado global |
|---|---|---|---|---|
| Smoke | 4 | 0 | 2 (UI, fase 5) | incompleta |
| Sanity | 2 | 1 (TC-SAN-003, funcional) | 0 | con_fallos |
| Regression | 4 | 0 | 4 (UI, fase 5) | incompleta |
| Integration | 3 | 0 | 0 | aprobada |

- Los 14 casos de API corrieron: **13 PASS y 1 FAIL**. El FAIL es un defecto real:
  **DEF-001** (BancoSol no está en `entidades_suplantadas`, ver `REGISTRO_DEFECTOS.md`). No se
  corrigió, a propósito.
- Comprobado en vivo: 409 al lanzar una segunda ejecución con otra activa; detener deja la
  ejecución FALLIDA y los casos restantes NOT_RUN con "La ejecución se detuvo antes de llegar
  a este caso"; 400 con suite inválida o casos de dos suites; 404 con un id inexistente.
- Comprobado en la base: ningún paso guarda la contraseña de prueba ni un JWT.

**Problemas encontrados**

- PowerShell 5.1 muestra mal las tildes de las respuestas JSON (`Invoke-RestMethod` asume
  ISO-8859-1 cuando la respuesta no trae charset). Solo afecta a la consola; los datos están bien.

## Fase 3 — Reportes de la ejecución (2026-11-24)

**Qué se hizo**

- Dependencia aprobada: `openpdf` 3.0.6 (paquete `org.openpdf.text`, licencia LGPL/MPL).
- Dominio: `FormatoReporte` (PDF/HTML), `ArchivoEvidencia`, `ReporteNoDisponibleException` y el
  puerto `EvidenciasPort` (lee archivos de la carpeta de evidencias; una ruta con `../` se trata
  como inexistente).
- `qa/application`: `PublicarResultadosUseCase` (es el `TrasEjecucion`: genera los dos reportes al
  terminar cada ejecución y guarda sus rutas; nunca lanza excepciones) y `ObtenerReporteUseCase`
  (descarga; si el archivo falta lo genera en ese momento; 409 si la ejecución sigue activa).
- `qa/infrastructure/reporte`:
  - `ReportePdf` (detallado): portada con resultado global y conteos, resumen por tipo y suite,
    por herramienta y por tipo de fallo, tabla de casos, métricas no funcionales y una página por
    caso con sus metadatos del catálogo, pasos, evidencia de API y capturas. Si una captura no
    está en disco, escribe "Captura no disponible: ..." y el reporte se genera igual.
  - `ReporteHtml` (resumido): un solo archivo con CSS en línea, sin scripts ni recursos externos;
    escapa todo el texto (un mensaje con `<script>` se muestra, no se ejecuta).
  - `GeneradorReporteAdapter` (escribe en `qa-evidencias/{uuid}/reporte-detallado.pdf` y
    `reporte-resumen.html`, primero en un `.tmp` y luego renombra), `EvidenciasArchivoAdapter` y
    `CarpetaEvidencias`.
- Endpoints: `GET /api/admin/qa/ejecuciones/{id}/reporte.pdf` y `/reporte.html` (descarga con
  `Content-Disposition: attachment`).
- Pruebas nuevas: `ReportesQaTest` (6, dobles en memoria) y `GeneradorReporteAdapterTest` (genera
  los archivos de verdad en una carpeta temporal, lee el texto del PDF y revisa el HTML; incluye
  una captura presente y otra faltante, una métrica que no cumple y la protección contra `../`).

**Verificación real (2026-11-24, contra `phishshield_qa`)**

- Suite Sanity: al terminar se generaron solos los dos reportes; las descargas por la API
  respondieron 200 con `application/pdf` (11,9 KB, 5 páginas) y `text/html` (5,2 KB). Se revisó
  el PDF página por página: portada, resumen, tabla y detalle del FAIL de TC-SAN-003 con su
  evidencia.
- Al cerrar la fase: backend 332 pruebas en verde, frontend 94, IA 24; regla 4 sin coincidencias.

## Cómo continuar

Siguiente fase: **4 — Pantalla "Centro de Pruebas"** en Angular (`/admin/qa`, solo ADMIN, entrada
en el menú del AdminLayout, polling cada 2 s mientras la ejecución está activa, descargas PDF/HTML,
pruebas con Vitest).
