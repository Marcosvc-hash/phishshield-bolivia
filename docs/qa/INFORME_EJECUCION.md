# Informe de ejecución — PhishShield Bolivia

Ejecución completa de las 9 suites del Centro de Pruebas QA, una tras otra, el **2026-11-24
entre las 14:10 y las 14:13** (hora de Bolivia), contra la base `phishshield_qa`, con el
catálogo de **28 casos**. Es la corrida de regresión final del módulo TQA, **después de V16
(DEF-001, BancoSol) y V17 (DEF-004, `xangosol.com` en la lista blanca)**. Las corridas
anteriores del mismo día se comparan en la sección 7.

## 1. Entorno

| Elemento | Valor |
|---|---|
| Backend | Spring Boot 4.1.1 / Java 25, `http://localhost:8080`, conectado a `phishshield_qa` |
| Frontend | Angular 22, `http://localhost:4200` |
| Servicio de IA | `http://localhost:8000`, modelo `phishshield-url-ia` 1.0.0 |
| Navegador | Chromium headless (Playwright 1.65.1) |
| Usuario de prueba | `qa.admin@phishshield.bo` (admin) |
| Parámetros | Valores por defecto (20 usuarios × 5 peticiones; escalones 10, 20, 40, 80; soak 5 usuarios × 2 min) |
| Correo | No configurado en esta ejecución (opcional) |
| Asistente de IA | No configurado: no hay `ANTHROPIC_API_KEY` (ver sección 6) |
| Base | `phishshield_qa` con V13 a V17 aplicadas |
| Rama | `feature/qa-automation` (cierre del módulo) |

## 2. Resumen

| Indicador | Valor |
|---|---|
| Casos ejecutados | 28 de 28 |
| PASS | **27** |
| FAIL | **1** (funcional: DEF-002, defecto real abierto con plan para el modelo 1.1.0) |
| SKIPPED | 0 |
| NOT_RUN | 0 |
| Porcentaje de éxito | 96,4 % |
| Duración total | 2 min 46 s (suma de las suites; el soak ocupa 2 min) |

## 3. Resultados por suite

| Tipo | Suite | PASS | FAIL | SKIPPED | NOT_RUN | Resultado global | Duración | Id de la ejecución |
|---|---|---|---|---|---|---|---|---|
| Funcional | Smoke | 6 | 0 | 0 | 0 | Aprobada | 7,0 s | `fbd32f77-2135-41e1-8fdf-bb94203fdc29` |
| Funcional | Sanity | 3 | 0 | 0 | 0 | Aprobada | 0,5 s | `788a9144-bab1-4009-84c6-590e2d3213a8` |
| Funcional | Regression | 9 | 0 | 0 | 0 | Aprobada | 7,3 s | `6c953c95-419a-424a-833c-a76347a32527` |
| Funcional | Integration | 3 | 0 | 0 | 0 | Aprobada | 0,7 s | `4d22bf90-4c81-4615-83e0-5f7372991f03` |
| Funcional | E2E | 1 | 0 | 0 | 0 | Aprobada | 3,4 s | `d5895757-6e1b-4f28-8e78-a97f4ce438d5` |
| No funcional | Performance | 2 | 0 | 0 | 0 | Aprobada | 3,2 s | `718ae307-480c-4fe9-8c92-d0bc3ef128a9` |
| No funcional | Estrés | 1 | 0 | 0 | 0 | Aprobada | 18,8 s | `c13d8ed9-874a-4bcf-b1bc-b95073c7b014` |
| No funcional | Soak | 1 | 0 | 0 | 0 | Aprobada | 2 min 0 s | `d73e56b9-0b62-47b3-82bb-618a0039454a` |
| No funcional | Robustez IA | 1 | 1 | 0 | 0 | Con fallos | 5,1 s | `521f3a4b-282b-43d4-b8c5-bdb403eff5f3` |

Los reportes PDF (detallado, con capturas y evidencias) y HTML (resumen) de cada ejecución están
en `backend/qa-evidencias/{id}/` y se descargan desde `/admin/qa`.

## 4. Casos con FAIL

| Caso | Tipo de fallo | Qué pasó | Defecto |
|---|---|---|---|
| TC-ROB-002 | Funcional | El modelo de IA por sí solo tuvo recall **0,7778** (< 0,80): no detecta 4 de 5 typosquatting y marca como phishing `bg.com.bo` | DEF-002 (media, abierto; plan para el modelo 1.1.0) |

TC-SAN-003 sigue en **PASS**: `https://bancosoI.com.bo/login` sale phishing con 81,35 puntos.
Detalle, pasos para reproducir y plan de DEF-002: `REGISTRO_DEFECTOS.md`.

## 5. Verificaciones en vivo fuera del catálogo

Hechas el mismo día contra `phishshield_qa`, con el backend de la sección 1.

**Corrección de DEF-004** (`POST /api/analisis`, sin sesión):

| URL | Resultado | Riesgo | Indicadores |
|---|---|---|---|
| `https://www.xangosol.com` | **seguro** | 0 | ninguno (está en la lista blanca desde V17; antes `URL_TYPOSQUATTING` 45) |
| `https://bancosoI.com.bo/login` | **phishing** | 81,35 | `URL_TYPOSQUATTING` 45, `IA_MODELO_URL` 26,35, `URL_RUTA_SENSIBLE` 10 |

**Alcance de ADMIN_EMPRESA en los reportes (HU-17)**, con `maria.quispe@andina.bo` (Importadora
Andina S.R.L.) y una contraseña de prueba puesta solo en `phishshield_qa`: **16 de 16 bien**.

| Comprobación | Esperado | Obtenido |
|---|---|---|
| Login por la API / `GET /api/auth/yo` | 200, rol ADMIN_EMPRESA | 200, ADMIN_EMPRESA |
| Login con contraseña equivocada | 401 | 401 |
| `POST /api/admin/reportes` sin `idEmpresa` (últimos 30 días) | 201, su empresa | 201, Importadora Andina: 3 análisis, 2 phishing, 2 alertas |
| El mismo período otra vez (upsert) | 201, mismo id | 201, mismo id |
| `POST` con el `idEmpresa` de su empresa | 201 | 201 |
| `POST` con el `idEmpresa` de otra empresa (Clínica San Rafael) | 404 | 404 |
| `POST` con una fecha futura | 400 | 400 |
| `GET /api/admin/reportes` | 200, solo su empresa | 200, 4 reportes, todos de Importadora Andina |
| `GET /api/admin/reportes?idEmpresa=<otra empresa>` | 404 | 404 |
| `GET /api/admin/reportes/{id}/pdf` de un reporte suyo | 200, PDF | 200, `application/pdf` |
| `GET .../pdf` de un reporte de otra empresa (generado por el ADMIN) | 404 | 404 |
| `GET /api/admin/auditoria` y `GET /api/admin/qa/casos` (solo ADMIN) | 403 | 403 |
| `GET /api/admin/reportes` sin token | 401 | 401 |

## 6. Asistente de IA del Centro de Pruebas

**Implementado y cubierto con pruebas automáticas; la prueba contra la API real de Anthropic
queda pendiente porque en este equipo no hay `ANTHROPIC_API_KEY`.** Sin la clave, la pantalla
oculta los botones del asistente y el resto del Centro de Pruebas funciona igual.

| Prueba automática | Qué comprueba |
|---|---|
| `AsistenteClaudeAdapterTest` (3) | Contra un servidor HTTP local que imita la Messages API: sin clave no llama a la API; manda la clave, la versión y un esquema JSON y lee la respuesta; un rechazo o un error devuelven vacío sin lanzar excepción |
| `AsistenteQaUseCaseTest` (5) | Informa las capacidades opcionales; analiza un FAIL sin cambiar su estado; solo analiza casos en FAIL; avisa si no está disponible o no responde; solo el ADMIN lo usa |
| `qa.spec.ts` (2) | Sin asistente no muestra sus botones; con asistente analiza un FAIL y resume la ejecución mostrando el aviso de que la IA no decide PASS/FAIL |

Para probarlo con la API real: poner `ANTHROPIC_API_KEY` en `backend\.env`, levantar el backend
con `levantar-backend-qa.ps1` y, en `/admin/qa`, abrir una ejecución con un FAIL y usar
"Analizar con IA" (en el detalle del caso) y "Resumen con IA" (en la ejecución).

## 7. Métricas no funcionales

**Performance (20 usuarios concurrentes × 5 peticiones)**

| Caso | Peticiones | p50 | p95 (umbral 2000 ms) | p99 | Throughput | Error |
|---|---|---|---|---|---|---|
| TC-PERF-001 URL nuevas (reglas + IA) | 100 | 443 ms | **906 ms** ✔ | 1080 ms | 39,4 pet/s | 0 % |
| TC-PERF-002 URL de la caché | 100 | 32 ms | **49 ms** ✔ | 63 ms | 549,5 pet/s | 0 % |

**Estrés (5 peticiones por usuario, URL nuevas)**

| Escalón | Usuarios | p95 | Throughput | Error |
|---|---|---|---|---|
| 1 | 10 | 305 ms | 43,7 pet/s | 0 % |
| 2 | 20 | 727 ms | 44,8 pet/s | 0 % |
| 3 | 40 | 1531 ms | 46,4 pet/s | 0 % |
| 4 | 80 | **2192 ms** ✘ (se degrada) | 44,6 pet/s | 0 % |

Recuperación: una petición normal después del último escalón respondió en **51 ms** → el caso pasa.

**Soak (5 usuarios constantes, 2 minutos, pausa de 200 ms)**: 1961 peticiones, 16,3 pet/s, 0 %
de error; p95 del primer tramo 152 ms y del último 129 ms (crecimiento 0 %, umbral 50 %).

**Robustez del detector (80 URL: 18 phishing, 62 legítimas)**

| Caso | Precisión | Recall | F1 | Exactitud |
|---|---|---|---|---|
| TC-ROB-001 Sistema completo | 1,0000 | 1,0000 | 1,0000 | 1,0000 |
| TC-ROB-002 Solo el modelo | 0,8235 | **0,7778** | 0,8000 | 0,9125 |

## 8. Comparación con las corridas anteriores del 2026-11-24

| Indicador | 10:38 (antes de V16) | 11:50 (con V16) | 14:10 (con V16 y V17) |
|---|---|---|---|
| PASS / FAIL | 26 / 2 | 27 / 1 | **27 / 1** |
| TC-SAN-003 | FAIL, seguro 36,35 | PASS, phishing 81,35 | PASS, phishing 81,35 |
| p95 PERF-001 | 1190 ms | 801 ms | 906 ms |
| Escalón de degradación (estrés) | 4 (2374 ms) | 4 (2200 ms) | 4 (2192 ms) |
| Soak: crecimiento del p95 | 0 % | 0 % | 0 % |
| Robustez (ROB-001 / recall ROB-002) | 1,0 / 0,7778 | 1,0 / 0,7778 | 1,0 / 0,7778 |

Ningún caso que pasaba dejó de pasar en las dos correcciones. Las métricas de carga varían entre
corridas (el generador comparte la máquina con el sistema probado); la degradación aparece
siempre entre 40 y 80 usuarios concurrentes con URL nuevas.

## 9. Análisis

1. **Funcionalidad**: las 5 suites funcionales pasaron completas (22 de 22), incluidos los 7
   casos de interfaz en Playwright. El alcance por empresa de los reportes se comprobó también en
   vivo con un ADMIN_EMPRESA real.
2. **Rendimiento**: con 20 usuarios el análisis completo responde con p95 de 0,8 a 1,2 s según la
   corrida, por debajo del umbral; desde la caché, más de 10 veces más rápido. El throughput con URL
   nuevas se estanca en 36-46 pet/s sin importar los usuarios: el cuello de botella es el cálculo
   completo (servicio de IA de un solo proceso y escritura en la base), no Tomcat ni la red.
3. **Capacidad**: sin errores en ningún escalón; la degradación aparece entre 40 y 80 usuarios y la
   recuperación es inmediata.
4. **Estabilidad**: 2 minutos de carga constante sin degradación ni errores.
5. **Detector**: el sistema completo acierta el 100 % del lote; el modelo solo no llega al recall
   mínimo y las reglas cubren sus huecos (DEF-002). El 1,0 es optimista porque las reglas se
   ajustaron mirando este mismo lote.

## 10. Conclusión y recomendaciones

Se cumplen los criterios de salida de **todas las suites funcionales** (100 % PASS) y de las no
funcionales del sistema completo. DEF-001, DEF-003 y DEF-004 están verificados; solo queda
abierto DEF-002 (severidad media), que afecta al modelo de IA aislado y no al veredicto del
sistema. Recomendaciones:

1. Ejecutar el plan de DEF-002 (`REGISTRO_DEFECTOS.md`) para el modelo 1.1.0, sin tocar el lote
   de robustez.
2. Probar el asistente de IA con la API real cuando haya una clave (sección 6).
3. Si se esperan más de 40 usuarios simultáneos analizando URL nuevas, levantar el servicio de IA
   con varios procesos (`uvicorn --workers`) y volver a medir el estrés.
