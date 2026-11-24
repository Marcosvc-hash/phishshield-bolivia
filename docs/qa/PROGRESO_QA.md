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
| 3 | Reportes PDF y HTML | Hecha (c7736b7) |
| 4 | Pantalla "Centro de Pruebas" en Angular | Hecha (f0b2001) |
| 5 | 7 casos de interfaz con Playwright | Hecha (0e9f02a) |
| 6 | Pruebas no funcionales (carga, estrés, soak, robustez IA) | Hecha (b6c2dc6) |
| 7 | Correo y asistente de IA (opcionales) | Hecha (d8dca75) |
| 8 | CI, JaCoCo y documentación TQA | Hecha (6b5dcac) |
| 9 | HU-17 Reportes por empresa | **Hecha** |

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

## Fase 4 — Pantalla "Centro de Pruebas" en Angular (2026-11-24)

**Qué se hizo**

- Ruta `/admin/qa` (carga diferida, `rolGuard('ADMIN')`) y entrada "Centro de Pruebas" en el
  grupo *Plataforma* del menú del AdminLayout (solo ADMIN).
- `paginas/admin/qa/`: `qa.service.ts` (tipos de la API y llamadas), `qa.ts` + `qa.html` +
  `qa.css` (página) y `detalle-caso.ts` (pasos, evidencia de API y métricas de un caso; se
  separó para respetar el presupuesto de 4 kB de CSS por componente).
- Catálogo agrupado por tipo (funcional / no funcional) y suite; cada suite plegable con su botón
  "Ejecutar suite", cantidad de casos y de casos programados; los no programados llevan la marca
  "Próxima fase". Selección de casos con casillas (solo de una misma suite: marcar uno de otra
  suite empieza una selección nueva), correo opcional y parámetros `clave=valor`.
- Panel de la ejecución: estado, resultado global, barra de avance, conteos PASS / FAIL /
  SKIPPED / NOT_RUN, botón Detener (con confirmación) mientras está activa y descargas
  "Reporte PDF" / "Resumen HTML" al terminar (con HttpClient y el token, como Blob). Cada caso se
  despliega con su observación, tipo de fallo, pasos, evidencia de API y métricas.
- Avance en vivo: mientras la ejecución está activa se consulta el detalle cada 2 s
  (`interval` + `switchMap`); al terminar se deja de consultar, se refresca el historial y se
  avisa el resultado. Al entrar, si hay una ejecución activa se abre sola.
- Historial paginado (10 por página) con resultado y conteos; un clic abre el detalle.
- Pruebas Vitest: `qa.spec.ts` (11: agrupación, plegado, POST de suite y polling con temporizador
  simulado hasta que termina, selección de una sola suite, cuerpo con códigos/correo/parámetros,
  error 409, apertura automática de la activa, despliegue de pasos y evidencia, detener,
  descarga del PDF como Blob y lectura de parámetros) y una más en `admin-layout.spec.ts`.

**Verificación real (2026-11-24)**

- Frontend en el puerto 4200 (`$env:PORT = "4200"`), backend contra `phishshield_qa`.
  Con Playwright (Chromium headless) se entró como `qa.admin@phishshield.bo`, se abrió
  `/admin/qa`, se eligió la última ejecución del historial y se desplegó el FAIL de TC-SAN-003:
  se ven el catálogo, el panel con conteos y pasos y el historial. También se revisó a 390 px de
  ancho (sin desborde horizontal).
- Frontend: 106 pruebas en verde y `npm run build` sin avisos.

## Fase 5 — Casos de interfaz con Playwright (2026-11-24)

**Qué se hizo**

- Dependencia aprobada: `playwright` 1.65.1 (Java). Chromium se descarga una vez, desde
  `backend\`: `.\mvnw.cmd -q dependency:build-classpath "-Dmdep.outputFile=target\cp.txt"` y
  `java -cp (Get-Content target\cp.txt) com.microsoft.playwright.CLI install chromium`.
  Queda en `%LOCALAPPDATA%\ms-playwright`. El backend arranca Playwright con
  `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1` (no descarga nada al ejecutar); si falta Chromium, los
  casos de interfaz quedan NOT_RUN con ese comando como motivo.
- `NavegadorQA`: un Chromium headless por ejecución (se abre solo si un caso lo pide) y un
  contexto aislado por caso (1366×860, es-BO, hora de La Paz), que se cierra al terminar el caso.
  Si el frontend no responde (`net::ERR_...`), el caso queda FAIL de **infraestructura**.
- `ContextoCaso.pasoUi(...)`: al terminar cada paso de interfaz guarda una captura de página
  completa en `qa-evidencias/{uuid}/capturas/{codigo}-{nn}-{paso}.png`, **también cuando el paso
  falla**. `requiereFrontend(...)` deja el caso NOT_RUN si falta `QA_FRONTEND_URL`.
- Casos en `qa/infrastructure/ejecutor/casos/ui`: `CasosSmokeUi` (SMK-005, SMK-006),
  `CasosRegressionUi` (REG-001, REG-002, REG-004, REG-007) y `CasosE2eUi` (E2E-001). Usan las
  aserciones de Playwright (`assertThat(locator)`), que esperan solas; si no se cumplen dan FAIL
  funcional. TC-REG-007 prepara por API un análisis de bnb.com.bo antes de buscar.
- Endpoint `GET /api/admin/qa/ejecuciones/{id}/evidencias?ruta=...` (`ObtenerEvidenciaUseCase`):
  solo entrega PNG de `{id}/capturas/` de esa ejecución (ni reportes, ni otras ejecuciones, ni
  `..`). En la pantalla, cada paso con captura tiene el botón "Ver captura".
- Pruebas: los 7 casos de interfaz deben estar programados (`RegistroCasosQATest`), protección del
  endpoint de capturas (`ReportesQaTest`) y "Ver captura" en `qa.spec.ts`.

**Verificación real (2026-11-24, backend contra `phishshield_qa`, frontend en el 4200, IA levantada)**

| Suite | PASS | FAIL | NOT_RUN | Resultado global |
|---|---|---|---|---|
| Smoke | 6 | 0 | 0 | aprobada |
| Regression | 8 | 0 | 0 | aprobada |
| E2E | 1 | 0 | 0 | aprobada |

- Se revisaron las capturas: TC-E2E-001 guardó 9 (una por paso); la del paso 5 muestra
  "Phishing detectado" con 100 puntos y la del paso 9 la alerta pendiente de la URL marcada.
- Camino del fallo: con el frontend apagado, TC-SMK-005 quedó **FAIL de infraestructura**
  ("El frontend no responde en http://localhost:4200/login...") y con su captura.
- Al cerrar la fase: backend 335 pruebas en verde, frontend 107, IA 24; regla 4 sin coincidencias.

**Problemas encontrados**

- `Playwright.create()` sin `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1` descarga también Firefox y
  WebKit (pasó una vez al probar a mano; el backend ya no lo hace).
- Las capturas de página completa pesan (100-400 KB cada una): el PDF de una ejecución E2E ronda
  1,3 MB. Es aceptable para la demo.

## Fase 6 — Pruebas no funcionales (2026-11-24)

**Qué se hizo**

- Dominio: `EstadisticasLatencia` (p50/p95/p99 por rango más cercano, media, máxima,
  throughput y tasa de error), `MatrizConfusion` (VP/FP/VN/FN, precisión, recall, F1,
  exactitud), `UrlEtiquetada` y el puerto `LoteRobustezPort` (adaptador `LoteRobustezJdbcAdapter`).
- `carga/GeneradorCarga`: generador propio con `java.net.http.HttpClient` e hilos virtuales; dos
  modos: cantidad fija por usuario y carga constante por tiempo (con pausa y corte si se detiene
  la ejecución). `carga/ClienteJson` recorre el lote sin llenar la evidencia.
- `CasosCarga`: TC-PERF-001 (URL nuevas, motor completo), TC-PERF-002 (5 URL repetidas desde la
  caché), TC-STR-001 (escalones; se detiene en el primero que se degrada y luego comprueba la
  recuperación con una petición normal) y TC-SOAK-001 (carga constante; compara el p95 del primer
  y del último cuarto del tiempo, con piso de 20 ms). Antes de cargar, una petición de
  calentamiento: si el backend no responde, FAIL de infraestructura.
- `CasosRobustez`: TC-ROB-001 (sistema completo por la API; detectada = sospechoso o phishing) y
  TC-ROB-002 (solo el modelo, `POST {IA_URL}/predecir`; detectada = `es_phishing`). Registran la
  matriz y los aciertos por categoría, y listan el phishing no detectado y las falsas alarmas.
- Parámetros (todos opcionales, en "Parámetros de la ejecución"):

| Parámetro | Por defecto | Casos |
|---|---|---|
| `usuarios` | 20 (soak: 5) | PERF, SOAK |
| `peticionesPorUsuario` | 5 | PERF, STR |
| `umbralP95Ms` | 2000 | PERF, STR |
| `umbralErrorPct` | 1 | PERF, SOAK |
| `escalones` | 10,20,40,80 | STR |
| `umbralErrorEstresPct` | 5 | STR |
| `minutos` | 2 (máximo 30) | SOAK |
| `pausaMs` | 200 | SOAK |
| `umbralPrecision` / `umbralRecall` | 0,80 | ROB |

- Las URL de carga llevan la marca de la ejecución y usan https sin ruta sensible: el aporte de la
  IA no llega a 40 puntos y los análisis salen seguros (comprobado: 858 análisis de carga, todos
  seguros, **ninguna alerta**). La caché (PERF-002) no crea filas nuevas.
- Pruebas: `MetricasNoFuncionalesTest` (6), `GeneradorCargaTest` (4, contra un servidor HTTP local
  del JDK) y `RegistroCasosQATest` ahora exige los 27 casos programados.

**Verificación real (2026-11-24, contra `phishshield_qa`, valores por defecto)**

| Caso | Resultado | Métricas principales |
|---|---|---|
| TC-PERF-001 | PASS | 100 peticiones; p50 420 ms, p95 961 ms, p99 1122 ms; 40,8 pet/s; 0 % error |
| TC-PERF-002 | PASS | 100 peticiones; p50 68 ms, p95 114 ms; 286,5 pet/s; 0 % error |
| TC-STR-001 | PASS | p95 por escalón: 10 → 302 ms, 20 → 629 ms, 40 → 1632 ms, 80 → **2370 ms** (se degrada en el escalón 4); throughput estable en ~45 pet/s; recuperación 42 ms |
| TC-SOAK-001 | PASS | 2 min, 1945 peticiones, 16,2 pet/s, 0 % error; p95 130 → 136 ms (+4,6 %) |
| TC-ROB-001 | PASS | precisión 1,0, recall 1,0 (VP 18, FP 0, VN 62, FN 0) |
| TC-ROB-002 | **FAIL** (funcional) | precisión 0,8235, recall **0,7778** (VP 14, FP 3, VN 59, FN 4) → **DEF-002** |

- Lectura: el techo de ~45 pet/s con URL nuevas (contra ~286 pet/s desde la caché) indica que el
  cuello de botella es el cálculo completo (consulta al servicio de IA de un solo proceso y
  escritura en la base), no la red ni Tomcat.
- Al cerrar la fase: backend 346 pruebas en verde, frontend 107, IA 24; regla 4 sin coincidencias.

**Limitaciones a tener en cuenta (van al plan de pruebas)**

- El generador de carga corre dentro del mismo backend que se prueba (comparten CPU): sirve para
  comparar entre ejecuciones y detectar degradación, no como medida absoluta de capacidad.
- TC-ROB-001 da 1,0 en parte porque las reglas y la lista blanca se ajustaron mirando este mismo
  lote boliviano: es una medida optimista; hace falta un lote nuevo para medir generalización.

## Fase 7 — Correo y asistente de IA (opcionales) (2026-11-24)

**Qué se hizo**

- Dependencia aprobada: `spring-boot-starter-mail` (versión de Spring Boot 4.1.1).
- `NotificadorCorreoSmtpAdapter`: arma su propio `JavaMailSenderImpl` con `MAIL_HOST`, `MAIL_PORT`,
  `MAIL_USERNAME`, `MAIL_PASSWORD` (opcionales `MAIL_FROM` y `MAIL_STARTTLS`, por defecto true). No
  se usa `spring.mail.*`: sin variables no existe ningún cliente de correo. `estaConfigurado()` es
  false si falta alguna de las cuatro. Los mensajes de error nunca llevan la contraseña.
- `PublicarResultadosUseCase` ahora, además de los reportes: sin SMTP marca `no_configurado`; con
  SMTP envía al destinatario elegido al lanzar (por defecto, quien lanza) un correo con resumen,
  casos en FAIL y los dos reportes adjuntos; un error queda en `error_correo` y los reportes siguen.
- Asistente de IA (`AsistenteClaudeAdapter`, Messages API de Anthropic por HTTP con `RestClient`):
  solo si existe `ANTHROPIC_API_KEY`; modelo `claude-opus-5-5` (cambiable con `QA_IA_MODELO`),
  `anthropic-version: 2026-10-21`, respuesta en JSON con esquema fijo (`output_config.format`),
  respaldo del servidor ante un rechazo (`fallbacks: "default"`, beta
  `server-side-fallback-2026-07-01`). Si la API falla o rechaza: `log.warn` y vacío. **Nunca decide
  PASS ni FAIL** (el prompt de sistema lo dice y el caso de uso no toca el estado).
- `AsistenteQaUseCase`: capacidades, análisis de un caso en FAIL y resumen de una ejecución terminada.
- Endpoints: `GET /capacidades`, `POST /ejecuciones/{id}/casos/{codigo}/analisis-ia` y
  `POST /ejecuciones/{id}/resumen-ia` (503 si el asistente no está configurado o no responde).
- Pantalla: estado del correo en el panel de la ejecución; aviso en el lanzador si no hay SMTP;
  botones "Analizar con IA" (casos en FAIL) y "Resumen con IA", **ocultos si no hay asistente**,
  siempre con el aviso de que la IA no decide el resultado.
- Pruebas: `NotificadorCorreoSmtpAdapterTest` (manda correos reales con JavaMail a un SMTP falso
  local escrito con sockets del JDK, `dobles/ServidorSmtpFalso`), `AsistenteClaudeAdapterTest` (servidor
  local que imita la Messages API: encabezados, cuerpo, rechazo, JSON inválido y 401),
  `AsistenteQaUseCaseTest` (5) y 3 desenlaces del correo en `ReportesQaTest`; 2 más en `qa.spec.ts`.

**Decisión pendiente para el usuario**

- La guía oficial de Anthropic recomienda el SDK de Java (`com.anthropic:anthropic-java`) en lugar de
  HTTP directo. No estaba en la lista de dependencias aprobadas, así que se usó `RestClient` (ya
  presente). Si se aprueba, el adaptador se puede pasar al SDK sin tocar el resto.

**Verificación real (2026-11-24)**

- En este equipo no hay variables `MAIL_*` ni clave de ningún LLM: `GET /capacidades` respondió
  `asistenteDisponible: false` y el análisis con IA respondió 503, como corresponde.
- Correo de punta a punta: se levantó el SMTP falso en el puerto 2525 y el backend con variables
  `MAIL_*` de prueba (no son credenciales reales). La suite Integration con destinatario
  `docente.testing@ejemplo.bo` quedó `aprobada` y con el correo `enviado`; el SMTP falso recibió el
  mensaje con el asunto "Reporte de automatización – Integration – ...", 2 adjuntos y autenticación
  con `MAIL_USERNAME`.
- Al cerrar la fase: backend 361 pruebas en verde, frontend 109, IA 24; regla 4 sin coincidencias.

## Fase 8 — Calidad del proyecto y documentación TQA (2026-11-24)

**Qué se hizo**

- `.github/workflows/ci.yml`: en cada push y pull request, tres trabajos: backend
  (`mvnw -B test -Dtest='*Test'` con Java 25; el patrón deja fuera `contextLoads`, que necesita
  PostgreSQL, y los casos falsos del ejecutor; sube el reporte de JaCoCo como artefacto),
  frontend (`npm ci`, `npm test -- --watch=false`, `npm run build` con Node 24) e IA
  (`pip install -r requirements.txt` y `pytest` con Python 3.14; las pruebas del servicio se
  omiten solas porque el modelo no está en git). Sin secretos.
- JaCoCo 0.8.16 (aprobado) en el `pom.xml`: `prepare-agent` + `report` en la fase `test`; reporte en
  `backend/target/site/jacoco/index.html`. Se excluyen los casos del Centro de Pruebas
  (`qa/infrastructure/ejecutor/casos/**`): son pruebas que viven en `src/main` y corren contra el
  sistema levantado.
- Documentos en `docs/qa/`: `PLAN_DE_PRUEBAS.md`, `MATRIZ_TRAZABILIDAD.md`, `REGISTRO_DEFECTOS.md`
  (DEF-001 y DEF-002) e `INFORME_EJECUCION.md`.
- `README.md` (estructura, stack, Centro de Pruebas y calidad) y `CLAUDE.md` (vertical `qa`,
  variables `QA_*`, `MAIL_*`, `ANTHROPIC_API_KEY`, CI, cobertura, V13, Playwright, defectos).

**Cobertura obtenida (JaCoCo, 2026-11-24)**

| Alcance | Líneas | Ramas |
|---|---|---|
| Backend completo | **63,9 %** (3266 de 5108) | **60,6 %** (1061 de 1752) |
| Capa de dominio (`domain/`) | 90,5 % | — |
| Casos de uso (`application/`) | 88,5 % | — |

Lo menos cubierto son los adaptadores web y JDBC de `panel`, `historial` y `auditoria` (30-40 %):
se prueban en vivo con el Centro de Pruebas, no con pruebas de Maven.

**Verificación**

- El comando de la CI se probó localmente sin base de datos (sin `DB_PASSWORD`): 360 pruebas, 0
  fallos. La primera versión usaba `-Dtest='!BackendApplicationTests'`, que hace que Surefire tome
  todas las clases (incluidos los casos falsos que solo usa el launcher) y daba 5 errores: se
  cambió a `-Dtest='*Test'`.
- **Ejecución completa real** de las 9 suites (2026-11-24, 01:42-01:45, 2 min 48 s): 27 casos,
  **25 PASS, 2 FAIL** (TC-SAN-003 → DEF-001; TC-ROB-002 → DEF-002), 0 SKIPPED, 0 NOT_RUN. Detalle
  en `INFORME_EJECUCION.md`.
- Pendiente de comprobar en GitHub: la primera corrida real de la CI se ve en la pestaña Actions
  después del push.

**Sobre la matriz**: el repositorio no tiene backlog con números de historia; HU-17 es la del
enunciado y HU-01 a HU-10 son una propuesta (está explicado en el documento) para reemplazar por
los números oficiales.

## Fase 9 — HU-17 "Generar reportes por empresa" (2026-11-24)

La sesión se cortó a mitad de esta fase (límite de uso); se retomó revisando `git status`, se
compiló lo que había y se completó. La CI del push de la fase 8 (6b5dcac) **pasó** (47 s).

**Qué se hizo**

- **Migración V14** (`V14__uuid_reportes.sql` + `01_schema.sql`): `uuid_publico` con índice único
  en `reportes` (la API nunca expone el BIGINT). Cambio de esquema: **aplicada solo en `phishshield_qa`**.
- **Migración V15** (`V15__caso_qa_reportes.sql` + `04_qa.sql`, ASCII con `U&'...'`): caso
  **TC-REG-009** (reporte por API) y objetivo de TC-E2E-001 actualizado (decía que no había módulo
  de reportes). **Aplicada solo en `phishshield_qa`**. El catálogo pasa a **28 casos**.
- Vertical `reporte` (hexagonal):
  - Dominio: `PeriodoReporte` (días en hora de Bolivia, ambos incluidos, máximo 366, sin fechas
    futuras), `TotalesReporte` (phishing ≤ análisis), `ReporteEmpresa`, `EmpresaDelReporte`,
    excepciones; puertos `ReporteRepositoryPort` y `ReportePdfPort`.
  - Casos de uso sin Spring: `GenerarReporteUseCase` (alcance con `AlcanceDatos`: ADMIN elige la
    empresa, ADMIN_EMPRESA siempre la suya y otra responde 404, USUARIO 403; auditoría
    `REPORTE_GENERADO`), `ListarReportesUseCase`, `DescargarReportePdfUseCase`.
  - `ReporteJdbcAdapter`: totales con una sola consulta sobre `analisis` + `alertas`; guardado con
    upsert sobre `uq_reportes_periodo` (regenerar el mismo período recalcula y conserva el id).
  - `ReporteEmpresaPdfAdapter` (OpenPDF), `ReporteConfig`, `ReporteController`
    (`POST /api/admin/reportes` 201, `GET /api/admin/reportes`, `GET /api/admin/reportes/{id}/pdf`),
    `ReporteExceptionHandler` (404, 400) y `backend/reportes.http`.
- Frontend: página `/admin/reportes` (ADMIN y ADMIN_EMPRESA, entrada "Reportes" en Monitoreo):
  formulario con empresa (solo ADMIN), período (por defecto los últimos 30 días), resumen del
  reporte recién generado, tabla paginada y descarga del PDF con el token.
- Caso **TC-REG-009** en `CasosRegressionApi`: busca Importadora Andina, genera el reporte de los
  últimos 365 días, lo regenera (mismo id), lo encuentra en el listado y descarga el PDF.
- Pruebas: `ReporteDominioTest` (4), `ReportesPorEmpresaTest` (7, doble en memoria),
  `ReporteEmpresaPdfAdapterTest` (2), ruta de reportes en `SeguridadRutasTest`, `reportes.spec.ts` (4).

**Defecto encontrado durante la fase: DEF-003 (corregido y verificado)**

La primera corrida de TC-REG-009 dejó la ejecución **atascada en `en_curso`**: el último paso
descarga un PDF, el cliente de API copiaba el cuerpo binario a la evidencia y PostgreSQL rechazó
el byte `0x00`; falló el guardado del avance y también el final. Corrección: las respuestas
binarias se resumen como `[contenido binario: N bytes, tipo]`, el dominio quita el carácter nulo
de todo texto, y si el guardado final falla se fuerza FALLIDA (`EjecucionRepositoryPort.marcarFallida`).
Al reiniciar, `MarcarInterrumpidasAlArrancar` destrabó la ejecución atascada (comportamiento
previsto). Detalle en `REGISTRO_DEFECTOS.md`.

**Verificación real (2026-11-24, contra `phishshield_qa`)**

- Regression: 9/9 PASS. Totales del reporte comprobados con una consulta SQL independiente
  (3 análisis, 2 phishing, 2 alertas en Importadora Andina para los últimos 365 días).
- API: 400 sin empresa (ADMIN) y con período futuro; PDF 200 con `Content-Disposition`.
- Pantalla revisada con Playwright: genera el reporte, muestra el resumen y la tabla (incluye el
  reporte de ejemplo de `02_seed.sql`, que ya tiene uuid por V14).
- **Ejecución completa** (10:38-10:41, 3 min): 28 casos, **26 PASS, 2 FAIL** (DEF-001 y DEF-002),
  0 SKIPPED, 0 NOT_RUN. `INFORME_EJECUCION.md` se rehízo con esta corrida.
- Al cerrar la fase: backend 376 pruebas en verde (375 con el comando de la CI, sin base),
  frontend 113, IA 24; regla 4 sin coincidencias. Cobertura: **64,4 %** de líneas y **60,9 %** de
  ramas (dominio 90,6 %, casos de uso 89,2 %, vertical `reporte` 84,8 %).
- No se pudo probar en vivo con un ADMIN_EMPRESA (no hay contraseña conocida para
  `maria.quispe@andina.bo` en `phishshield_qa`): el alcance por empresa está cubierto por las
  pruebas de casos de uso.

## Cierre de pendientes (2026-11-24, después de la fase 9)

- **Base real:** respaldo `respaldo_antes_v13.dump` y luego V13, V14 y V15. Esquema comparado
  contra `phishshield_qa`: iguales, salvo las tablas de educación (la real no las tiene desde antes).
- **DEF-001 corregido** con `V16__entidad_bancosol.sql` (+ `02_seed.sql`): BancoSol en
  `entidades_suplantadas` y en la lista blanca. La migración escribe un `DOMINIO_AGREGADO` en
  `auditoria` para que la caché de 24 h no devuelva el "seguro" ya calculado. Medición previa
  con el motor real sobre 235 448 URL: solo cambian `bancosoI.com.bo` y `xangosol.com`
  (legítimo en PhiUSIIL; queda a decisión del usuario pasarlo a la lista blanca). Aplicada en
  `prueba_v16` (temporal, borrada), `phishshield_qa` y la real (respaldo `respaldo_antes_v16.dump`).
- **Verificación:** TC-SAN-003 solo, PASS (phishing, 81,35). Suite completa 11:50-11:53:
  **27 PASS, 1 FAIL** (TC-ROB-002 = DEF-002, mismas métricas). `INFORME_EJECUCION.md` rehecho.
- **DEF-002:** no se corrige (entrenar con los únicos ejemplos bolivianos sería fuga de datos);
  plan de corrección en `REGISTRO_DEFECTOS.md`.
- Backend 376, frontend 113 + build, IA 24, regla 4 sin coincidencias; cobertura sin cambios.

## Cierre final del módulo (2026-11-24, tarde)

- **DEF-004** (nuevo, encontrado al medir la regresión de V16): `xangosol.com`, legítimo, sumaba 45
  por parecerse a "bancosol". Corregido con `V17__lista_blanca_xangosol.sql` (+ `02_seed.sql`),
  probada en `prueba_v17` (temporal, borrada), aplicada en `phishshield_qa` y en la real
  (respaldo `respaldo_antes_v17.dump`). En vivo: `xangosol.com` → seguro (0) y
  `bancosoI.com.bo` → phishing (81,35).
- **Suite completa 14:10-14:13:** 27 PASS, 1 FAIL (TC-ROB-002 = DEF-002). `INFORME_EJECUCION.md`
  rehecho con esta corrida.
- **DEF-002:** sigue abierto, con plan para el modelo 1.1.0.
- **Secretos:** `backend/.env` ignorado y nunca commiteado (revisado en todas las ramas y en `origin`).
  Respaldos `.dump` copiados a `C:\respaldos-phishshield` (mismo SHA-256).
- **ADMIN_EMPRESA en vivo:** `maria.quispe@andina.bo` con una contraseña de prueba solo en
  `phishshield_qa`; 16 de 16 comprobaciones de la API de reportes bien.
- **Asistente de IA:** implementado y con 10 pruebas automáticas; la prueba con la API real queda
  pendiente por falta de `ANTHROPIC_API_KEY`.
- Backend 376, frontend 113 + build, IA 24; PR de `feature/qa-automation` a `main` abierto, sin fusionar.

## Cómo continuar

Pendiente de decisión del usuario:

1. Ejecutar el plan de **DEF-002** (modelo 1.1.0, ver `REGISTRO_DEFECTOS.md`).
2. Probar el asistente de IA con la API real cuando haya clave (`INFORME_EJECUCION.md`, sección 6).
3. ¿Pasar el asistente de IA al SDK oficial de Java de Anthropic? (dependencia no aprobada todavía).
4. Revisar y fusionar el PR de `feature/qa-automation` a `main` (no se hizo).
