# Progreso TQA — Centro de Pruebas QA y HU-17

Resumen de cierre del trabajo de la materia **Testing y Aseguramiento de la Calidad** en la rama
`feature/qa-automation` (al 2026-11-24). La bitácora detallada, fase por fase, está en
[`docs/qa/PROGRESO_QA.md`](qa/PROGRESO_QA.md); este archivo es la vista de conjunto.

- Rama: `feature/qa-automation`, subida a `origin`. **No está fusionada con `main`.**
- Todo lo que corrió en vivo fue contra la base de pruebas **`phishshield_qa`**, nunca la real.
  En la base real solo se aplicaron migraciones (V13 a V16), con respaldo previo (ver §5).
- Los casos se clasifican solo por **tipo** (funcional / no funcional) y **suite**.

---

## 1. Fases y commits

| Fase | Commit | Qué se hizo |
|---|---|---|
| 1 | `d60d8ba` | Base de datos `qa_*`, dominio de la vertical `qa` y sus puertos |
| 2 | `5a67d2b` | Motor de ejecución (JUnit Platform Launcher) y 14 casos de API |
| 3 | `c7736b7` | Reportes PDF detallado y HTML resumido de cada ejecución |
| 4 | `f0b2001` | Pantalla "Centro de Pruebas" en Angular (`/admin/qa`) |
| 5 | `0e9f02a` | 7 casos de interfaz con Playwright, con capturas por paso |
| 6 | `b6c2dc6` | Pruebas no funcionales: carga, estrés, soak y robustez del modelo |
| 7 | `d8dca75` | Envío de reportes por correo y asistente de IA (ambos opcionales) |
| 8 | `6b5dcac` | CI en GitHub Actions, cobertura con JaCoCo y documentación TQA |
| 9 | `5ae2712` | HU-17 "Generar reportes por empresa" y su caso TC-REG-009 |

### Fase 1 — Base de datos, dominio y puertos (`d60d8ba`)

- **Migración V12** (`database/migraciones/V12__modulo_qa.sql`) y `database/initdb/04_qa.sql`: 6 tablas
  (`qa_casos`, `qa_ejecuciones`, `qa_resultados`, `qa_pasos`, `qa_metricas`, `qa_lote_robustez`),
  catálogo de 27 casos y lote de 80 URL etiquetadas. Aplicada en la base real y en `phishshield_qa`.
- Dominio en `backend/.../qa/domain/model`: `Ejecucion` (raíz del agregado y su ciclo de vida),
  `ResultadoCaso`, `CasoPrueba`, `DesenlaceCaso`, enums de tipo, suite y estados.
- Puertos en `qa/domain/port/out`: `EjecutorPruebasPort`, `EjecucionRepositoryPort`,
  `GeneradorReportePort`, `NotificadorCorreoPort`, `AsistenteIAPort`, entre otros.
- Pruebas: `EjecucionTest`, `ValoresDominioQaTest`, `DominioQaLimpioTest`.

### Fase 2 — Motor de ejecución y casos de API (`5a67d2b`)

- Casos de uso en `qa/application`: `IniciarEjecucion`, `CorrerEjecucion`, `DetenerEjecucion`,
  `ListarEjecuciones`, `ObtenerDetalleEjecucion`, `ListarCatalogo`, `MarcarInterrumpidasAlArrancar`.
- Adaptadores: `EjecucionJdbcAdapter`, `CatalogoCasosJdbcAdapter`, `JUnitEjecutorPruebasAdapter`,
  `PlanificadorEjecucionAsync` (`@Async`, pool de un hilo), `ClienteApiQA` (enmascara contraseña,
  tokens y `Authorization`).
- Casos en `qa/infrastructure/ejecutor/casos/api`: Smoke, Sanity, Regression e Integration.
- `QaController` (`/api/admin/qa`, solo ADMIN), `backend/qa.http`, `backend/.env.example`,
  `backend/herramientas/crear-usuario-qa.ps1`.
- **Migración V13**: corrige el texto de TC-REG-008 (ruta `/api/admin/qa/casos`).
- Pruebas: `CasosDeUsoQaTest`, `JUnitEjecutorPruebasAdapterTest`, `RegistroCasosQATest`.

### Fase 3 — Reportes de la ejecución (`c7736b7`)

- `qa/infrastructure/reporte`: `ReportePdf` (OpenPDF), `ReporteHtml` (autocontenido, texto escapado),
  `GeneradorReporteAdapter`, `EvidenciasArchivoAdapter`, `CarpetaEvidencias`.
- Casos de uso `PublicarResultadosUseCase` y `ObtenerReporteUseCase`; endpoints
  `/reporte.pdf` y `/reporte.html`. Archivos en `backend/qa-evidencias/` (ignorada por git).
- Pruebas: `ReportesQaTest`, `GeneradorReporteAdapterTest` (lee el texto del PDF generado).

### Fase 4 — Pantalla Centro de Pruebas (`f0b2001`)

- `frontend/src/app/paginas/admin/qa/`: `qa.ts`, `qa.html`, `qa.css`, `qa.service.ts`,
  `detalle-caso.ts`. Ruta `/admin/qa` con `rolGuard('ADMIN')` y entrada en el menú.
- Pruebas: `qa.spec.ts` (incluye el sondeo cada 2 s con temporizador simulado).

### Fase 5 — Casos de interfaz con Playwright (`0e9f02a`)

- `NavegadorQA` (Chromium headless, un contexto aislado por caso), `ContextoCaso.pasoUi(...)`
  (captura al final de cada paso, también si falla).
- Casos en `qa/infrastructure/ejecutor/casos/ui`: TC-SMK-005, TC-SMK-006, TC-REG-001/002/004/007, TC-E2E-001.
- Endpoint de capturas (`/evidencias?ruta=...`, solo PNG de esa ejecución) y botón "Ver captura".

### Fase 6 — Pruebas no funcionales (`b6c2dc6`)

- `carga/GeneradorCarga` (HttpClient + hilos virtuales), `EstadisticasLatencia`, `MatrizConfusion`,
  `LoteRobustezJdbcAdapter`.
- `CasosCarga` (TC-PERF-001/002, TC-STR-001, TC-SOAK-001) y `CasosRobustez` (TC-ROB-001/002).
- Pruebas: `MetricasNoFuncionalesTest`, `GeneradorCargaTest` (contra un servidor HTTP local).

### Fase 7 — Correo y asistente de IA (`d8dca75`)

- `NotificadorCorreoSmtpAdapter` (variables `MAIL_*`), `AsistenteClaudeAdapter` (Messages API por HTTP,
  solo con `ANTHROPIC_API_KEY`), `AsistenteQaUseCase`, endpoints `/capacidades`, `/analisis-ia`, `/resumen-ia`.
- Pruebas: `NotificadorCorreoSmtpAdapterTest` (con `dobles/ServidorSmtpFalso`), `AsistenteClaudeAdapterTest`,
  `AsistenteQaUseCaseTest`.

### Fase 8 — Calidad y documentación TQA (`6b5dcac`)

- `.github/workflows/ci.yml` y `jacoco-maven-plugin` en `backend/pom.xml`.
- `docs/qa/PLAN_DE_PRUEBAS.md`, `MATRIZ_TRAZABILIDAD.md`, `REGISTRO_DEFECTOS.md`, `INFORME_EJECUCION.md`.
- `README.md` y `CLAUDE.md` actualizados.

### Fase 9 — HU-17 Reportes por empresa (`5ae2712`)

- **Migración V14** (`V14__uuid_reportes.sql` + `01_schema.sql`): `uuid_publico` en `reportes`.
- **Migración V15** (`V15__caso_qa_reportes.sql` + `04_qa.sql`): caso TC-REG-009 (catálogo de 28 casos)
  y objetivo de TC-E2E-001 al día.
- Vertical `backend/.../reporte`: `PeriodoReporte`, `TotalesReporte`, `ReporteEmpresa`,
  `GenerarReporteUseCase`, `ListarReportesUseCase`, `DescargarReportePdfUseCase`, `ReporteJdbcAdapter`,
  `ReporteEmpresaPdfAdapter`, `ReporteController`, `backend/reportes.http`.
- Frontend `frontend/src/app/paginas/admin/reportes/` (ruta `/admin/reportes`, ADMIN y ADMIN_EMPRESA).
- Corrección de **DEF-003** (ver §4).
- Pruebas: `ReporteDominioTest`, `ReportesPorEmpresaTest`, `ReporteEmpresaPdfAdapterTest`, `reportes.spec.ts`,
  y dos pruebas de regresión de DEF-003.

---

## 2. Estado de la CI y de las pruebas

| Qué | Resultado |
|---|---|
| CI de GitHub Actions, push de la fase 8 (`6b5dcac`) | **Pasó** (run 45336072169, 47 s) |
| CI de GitHub Actions, push de la fase 9 (`5ae2712`) | **Pasó** (run 45417714209, 42 s) |
| CI de GitHub Actions, push del cierre de pendientes (`a1a10ed`) | **Pasó** (run 45431743717, 40 s; backend 375 pruebas) |
| Backend local, `mvnw test` con la base (cierre, 2026-11-24) | **376 pruebas, 0 fallos** |
| Backend con el comando de la CI, sin base (`-Dtest='*Test'`) | **375 pruebas, 0 fallos** (fase 9) |
| Frontend, `npm test -- --watch=false` (cierre) | **113 pruebas, 0 fallos**; `npm run build` sin avisos |
| IA, `pytest` (cierre) | **24 pruebas, 0 fallos** |
| Regla 4 (dominio sin Spring ni JPA, `git grep`) | Sin coincidencias |
| Cobertura (JaCoCo, cierre) | 64,4 % de líneas y 60,9 % de ramas; dominio 90,6 %, casos de uso 89,2 % (igual que en la fase 9: el cierre no cambió código Java) |

**Centro de Pruebas, última ejecución completa** (2026-11-24, 11:50, 28 casos, contra `phishshield_qa`,
después de corregir DEF-001): **27 PASS, 1 FAIL**, 0 SKIPPED, 0 NOT_RUN.

| Caso | Estado | Defecto |
|---|---|---|
| TC-SAN-003 | **PASS** (antes FAIL): `https://bancosoI.com.bo/login` sale phishing con 81,35 | DEF-001 (alta) **Verificado** |
| TC-ROB-002 | FAIL: el modelo de IA solo tiene recall 0,78 (< 0,80), no reconoce typosquatting | DEF-002 (media) abierto, con plan |

La corrida anterior (10:38, antes de la corrección) dio 26 PASS y 2 FAIL; ningún caso que pasaba
dejó de pasar. Detalle en `docs/qa/INFORME_EJECUCION.md` y `docs/qa/REGISTRO_DEFECTOS.md`.

---

## 3. Pendiente y siguiente paso

Hechos el 2026-11-24 (ver §5): V13, V14 y V15 en la base real, corrección de DEF-001 (V16) y
plan de DEF-002. Queda lo que depende de una decisión del usuario o de credenciales que no hay en
este equipo:

1. **Ejecutar el plan de DEF-002** (reentrenar el modelo como 1.1.0 con typosquatting boliviano
   generado, sin tocar el lote de robustez). Plan completo en `docs/qa/REGISTRO_DEFECTOS.md`.
2. **Decidir si `xangosol.com` va a la lista blanca**: es legítimo en PhiUSIIL y desde V16 suma
   45 puntos por parecerse a "bancosol" (único efecto colateral medido de V16).
3. **Probar el asistente de IA contra la API real**: no se pudo porque no hay `ANTHROPIC_API_KEY`
   (se probó con un servidor que imita la API).
4. **Probar en vivo como ADMIN_EMPRESA**: no hay contraseña conocida para `maria.quispe@andina.bo` en
   `phishshield_qa`; el alcance por empresa está cubierto por pruebas de casos de uso.
5. **Fusionar `feature/qa-automation` a `main`** cuando el usuario lo apruebe.

**Siguiente paso recomendado:** revisar la rama y fusionarla a `main` (punto 5); DEF-002 no
bloquea, porque afecta al modelo aislado y no al veredicto del sistema.

---

## 4. Decisiones técnicas y por qué

| Decisión | Por qué |
|---|---|
| **`uuid_publico` en `reportes` (V14)** | La API del proyecto nunca expone el BIGINT interno (regla de `CLAUDE.md`). Se siguió el mismo patrón que V9 para `alertas` y `dominios_lista`: columna con valor por defecto e índice único, sin tocar filas existentes. |
| **Migraciones separadas: V14 (esquema) y V15 (catálogo QA)** | Son cambios de distinta naturaleza y riesgo: V14 altera una tabla del producto; V15 solo agrega datos al catálogo de pruebas. Separadas se pueden aplicar, revisar o revertir por separado, y el historial queda claro. |
| **Migraciones primero en `phishshield_qa`** | Durante las fases la consigna prohibía tocar la base real. El 2026-11-24 el usuario pidió aplicarlas: se hizo con respaldo previo y comparando el esquema contra `phishshield_qa`. |
| **DEF-001 con una entidad nueva (V16), no aflojando la regla** | Es un hueco de datos: la regla funciona, pero no tenía contra qué comparar. Antes se midió con el motor real sobre 235 448 URL: solo cambian `bancosoI.com.bo` y `xangosol.com`. |
| **V16 escribe un `DOMINIO_AGREGADO` en `auditoria`** | La caché de 24 h solo se invalida con esas filas; sin ella, el "seguro" ya guardado se seguía devolviendo. Solo se escribe si la migración inserta el dominio (idempotente). |
| **DEF-002 planificado, no corregido** | Los únicos ejemplos de typosquatting boliviano están en los conjuntos de evaluación: entrenar con ellos haría pasar TC-ROB-002 por fuga de datos. |
| **Base de pruebas aparte y bloqueo del ejecutor** | Las pruebas crean empresas, análisis, alertas y miles de filas de carga. Si el backend está conectado a una base que no sea `*_qa` ni `prueba_*`, ningún caso corre (salvo `QA_PERMITIR_BASE_REAL=true`). |
| **Casos de prueba en `src/main` con JUnit en scope compile** | El Centro de Pruebas ejecuta los casos en caliente desde el backend, con el JUnit Platform Launcher; en `src/test` no estarían disponibles en ejecución. |
| **Un caso por vez con el Launcher** | Respeta el orden del catálogo, permite guardar el avance caso por caso y que "detener" actúe entre un caso y el siguiente. |
| **Clasificación de fallos** | `AssertionError` = FAIL funcional (posible defecto); sin conexión = FAIL de infraestructura (no dice si el sistema funciona); falta de configuración = NOT_RUN. Así un servicio caído no se confunde con un defecto. |
| **Evidencia enmascarada** | La contraseña de prueba, los tokens y `Authorization` nunca se guardan; se comprobó en la base. |
| **Reporte por empresa con upsert** | La tabla ya tenía `UNIQUE (id_empresa, periodo_inicio, periodo_fin)`: generar dos veces el mismo período recalcula los totales y conserva el id, en vez de fallar o duplicar. |
| **Totales calculados al generar** | Se cuentan desde `analisis` y `alertas` en el momento (días en hora de Bolivia, como el panel), y se verificaron contra una consulta SQL independiente. |
| **ADMIN_EMPRESA recibe 404 por otra empresa** | Igual que el resto del panel: un dato fuera de su alcance responde lo mismo que uno inexistente. |
| **Generador de carga propio** | No requería dependencias nuevas; con hilos virtuales simula muchos usuarios con muy poco costo. Se documentó que comparte la máquina con el sistema probado. |
| **Asistente de IA por HTTP (sin SDK)** | La guía de Anthropic recomienda su SDK de Java, pero no estaba en la lista de dependencias aprobadas; se usó `RestClient`, que ya estaba en el proyecto. Si se aprueba el SDK, el cambio queda dentro del adaptador. |
| **Corrección de DEF-003 en dos niveles** | Causa (las respuestas binarias no se copian a la evidencia y se quita el carácter nulo) y robustez (si el guardado final falla, la ejecución queda FALLIDA y no bloquea a las siguientes). |
| **CSS del Centro de Pruebas dividido en componentes** | Angular avisa si el CSS de un componente pasa de 4 kB; se separó `detalle-caso.ts` en lugar de subir el límite del proyecto. |
| **CI con `-Dtest='*Test'`** | Deja fuera `contextLoads` (necesita PostgreSQL) y los casos falsos que solo usa el launcher; con `!BackendApplicationTests` Surefire tomaba todas las clases y daba 5 errores. |

---

## 5. Cierre de pendientes (2026-11-24)

Pedido del usuario: cerrar todo lo que se pudiera hacer desde este equipo, en orden.

| Paso | Qué se hizo | Comprobación |
|---|---|---|
| 1. Respaldo | `pg_dump -Fc` de la base real → `respaldo_antes_v13.dump` (raíz del repo, ignorado por git) | 93 710 bytes; `pg_restore -l` lo lee y trae los datos de las 19 tablas |
| 2. V13, V14, V15 en la base real | Aplicadas en orden con `ON_ERROR_STOP=1 --single-transaction` | Columnas e índices iguales a `phishshield_qa` (salvo las tablas de educación, que la real no tiene desde antes); `reportes.uuid_publico` con `uq_reportes_uuid`; catálogo QA de 28 casos |
| 3. DEF-001 | `V16__entidad_bancosol.sql` + `02_seed.sql`: BancoSol en `entidades_suplantadas` y en la lista blanca, con `DOMINIO_AGREGADO` en `auditoria` | Probada en dos bases temporales `prueba_v16` (ya borradas); aplicada en `phishshield_qa` y en la real (respaldo previo `respaldo_antes_v16.dump`, 94 585 bytes). TC-SAN-003: PASS (phishing, 81,35). Suite completa: 27 PASS, 1 FAIL (DEF-002) |
| 4. DEF-002 | No se corrigió (ver §4); plan de corrección en `docs/qa/REGISTRO_DEFECTOS.md` | — |
| 5. Pruebas | Backend 376, frontend 113 + build, IA 24, regla 4, JaCoCo | Todo en verde (ver §2); CI del push `a1a10ed`: pasó (run 45431743717) |
| 6. Documentación | `REGISTRO_DEFECTOS.md`, `INFORME_EJECUCION.md`, `MATRIZ_TRAZABILIDAD.md`, `PROGRESO_QA.md`, `database/README.md`, `CLAUDE.md` y este archivo | — |

Para volver atrás la base real (solo si hiciera falta, borra lo posterior al respaldo):
`docker exec phishshield-db pg_restore -U phishshield -d phishshield --clean --if-exists /tmp/respaldo_antes_v13.dump`
(copiar antes el archivo al contenedor con `docker cp`).
