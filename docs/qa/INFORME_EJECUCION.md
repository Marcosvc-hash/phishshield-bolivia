# Informe de ejecución — PhishShield Bolivia

Ejecución completa de las 9 suites del Centro de Pruebas QA, una tras otra, el **2026-11-24
entre las 10:38 y las 10:41** (hora de Bolivia), contra la base `phishshield_qa`, con el
catálogo de **28 casos** (incluye TC-REG-009, del módulo de reportes por empresa).

## 1. Entorno

| Elemento | Valor |
|---|---|
| Backend | Spring Boot 4.1.1 / Java 25, `http://localhost:8080`, conectado a `phishshield_qa` |
| Frontend | Angular 22, `http://localhost:4200` |
| Servicio de IA | `http://localhost:8000`, modelo `phishshield-url-ia` 1.0.0 |
| Navegador | Chromium headless (Playwright 1.65.1) |
| Usuario de prueba | `qa.admin@phishshield.bo` (admin) |
| Parámetros | Valores por defecto (20 usuarios × 5 peticiones; escalones 10, 20, 40, 80; soak 5 usuarios × 2 min) |
| Correo y asistente de IA | No configurados en esta ejecución (opcionales) |
| Rama | `feature/qa-automation` (fase 9) |

## 2. Resumen

| Indicador | Valor |
|---|---|
| Casos ejecutados | 28 de 28 |
| PASS | **26** |
| FAIL | **2** (ambos funcionales: defectos reales registrados) |
| SKIPPED | 0 |
| NOT_RUN | 0 |
| Porcentaje de éxito | 92,9 % |
| Duración total | 3 min 0 s (el soak ocupa 2 min) |

## 3. Resultados por suite

| Tipo | Suite | PASS | FAIL | SKIPPED | NOT_RUN | Resultado global | Duración | Id de la ejecución |
|---|---|---|---|---|---|---|---|---|
| Funcional | Smoke | 6 | 0 | 0 | 0 | Aprobada | 4,1 s | `2bb1e290-0848-4525-8923-c6a19fb2a969` |
| Funcional | Sanity | 2 | 1 | 0 | 0 | Con fallos | 0,5 s | `89105ff2-7875-4238-b886-29ee93962739` |
| Funcional | Regression | 9 | 0 | 0 | 0 | Aprobada | 6,9 s | `c9d457ac-ab3a-4f3a-946d-fab093dc31bc` |
| Funcional | Integration | 3 | 0 | 0 | 0 | Aprobada | 1,2 s | `d29f9807-6071-4b74-b888-8dee47ccc0d7` |
| Funcional | E2E | 1 | 0 | 0 | 0 | Aprobada | 3,3 s | `fe382618-46c4-4fb0-b72d-bda3dc2f4ac8` |
| No funcional | Performance | 2 | 0 | 0 | 0 | Aprobada | 4,2 s | `82af08d6-05fc-434b-ae56-10149479a35a` |
| No funcional | Estrés | 1 | 0 | 0 | 0 | Aprobada | 20,5 s | `63376df9-4ad5-4508-81a5-5e03220fbf6f` |
| No funcional | Soak | 1 | 0 | 0 | 0 | Aprobada | 2 min 0 s | `311b1d64-3b2f-4969-9ced-94afcc86ab68` |
| No funcional | Robustez IA | 1 | 1 | 0 | 0 | Con fallos | 2,9 s | `580fca52-5260-4a9d-97d7-dddb44881483` |

Los reportes PDF (detallado, con capturas y evidencias) y HTML (resumen) de cada ejecución están
en `backend/qa-evidencias/{id}/` y se descargan desde `/admin/qa`.

## 4. Casos con FAIL

| Caso | Tipo de fallo | Qué pasó | Defecto |
|---|---|---|---|
| TC-SAN-003 | Funcional | `https://bancosoI.com.bo/login` salió **seguro** (36,35 puntos: ruta sensible 10 + IA 26,35); se esperaba phishing (≥ 70) | DEF-001 (alta, abierto) |
| TC-ROB-002 | Funcional | El modelo de IA por sí solo tuvo recall **0,7778** (< 0,80): no detecta 4 de 5 typosquatting y marca como phishing `bg.com.bo` | DEF-002 (media, abierto) |

Detalle, pasos para reproducir y corrección propuesta: `REGISTRO_DEFECTOS.md`. Ahí también está
DEF-003, un defecto del propio Centro de Pruebas encontrado y corregido durante la fase 9.

## 5. Métricas no funcionales

**Performance (20 usuarios concurrentes × 5 peticiones)**

| Caso | Peticiones | p50 | p95 (umbral 2000 ms) | p99 | Throughput | Error |
|---|---|---|---|---|---|---|
| TC-PERF-001 URL nuevas (reglas + IA) | 100 | 528 ms | **1190 ms** ✔ | 1372 ms | 32,7 pet/s | 0 % |
| TC-PERF-002 URL de la caché | 100 | 86 ms | **128 ms** ✔ | 138 ms | 215,5 pet/s | 0 % |

**Estrés (5 peticiones por usuario, URL nuevas)**

| Escalón | Usuarios | p95 | Throughput | Error |
|---|---|---|---|---|
| 1 | 10 | 285 ms | 44,9 pet/s | 0 % |
| 2 | 20 | 838 ms | 36,6 pet/s | 0 % |
| 3 | 40 | 1981 ms | 40,7 pet/s | 0 % |
| 4 | 80 | **2374 ms** ✘ (se degrada) | 42,1 pet/s | 0 % |

Recuperación: una petición normal 2 s después respondió 200 en **46 ms** → el caso pasa.

**Soak (5 usuarios constantes, 2 minutos, pausa de 200 ms)**: 1795 peticiones, 14,9 pet/s, 0 %
de error; p95 del primer tramo 178 ms y del último 169 ms (crecimiento 0 %, umbral 50 %).

**Robustez del detector (80 URL: 18 phishing, 62 legítimas)**

| Caso | Precisión | Recall | F1 | Exactitud | VP | FP | VN | FN |
|---|---|---|---|---|---|---|---|---|
| TC-ROB-001 Sistema completo | 1,0000 | 1,0000 | 1,0000 | 1,0000 | 18 | 0 | 62 | 0 |
| TC-ROB-002 Solo el modelo | 0,8235 | **0,7778** | 0,8000 | 0,9125 | 14 | 3 | 59 | 4 |

## 6. Comparación con la corrida anterior (01:42, catálogo de 27 casos)

| Indicador | 01:42 | 10:38 |
|---|---|---|
| PASS / FAIL | 25 / 2 | 26 / 2 (TC-REG-009 nuevo, en PASS) |
| p95 PERF-001 | 883 ms | 1190 ms |
| Escalón de degradación (estrés) | 3 (40 usuarios, 2178 ms) | 4 (80 usuarios, 2374 ms) |
| Soak: crecimiento del p95 | 0 % | 0 % |

Las métricas de carga varían entre corridas (el generador comparte la máquina con el sistema
probado): la degradación aparece entre 40 y 80 usuarios concurrentes con URL nuevas. Los
resultados funcionales y de robustez son idénticos.

## 7. Análisis

1. **Funcionalidad**: Smoke, Regression (incluido el nuevo módulo de reportes), Integration y E2E
   pasaron completas, con los 7 casos de interfaz en Playwright. La única falla funcional del
   sistema es de datos de detección (DEF-001): BancoSol no está entre las entidades protegidas.
2. **Rendimiento**: con 20 usuarios el análisis completo responde con p95 de 0,9 a 1,2 s, por
   debajo del umbral. Desde la caché es 9 veces más rápido. El throughput con URL nuevas se
   estanca en 33-48 pet/s sin importar los usuarios: el cuello de botella es el cálculo completo
   (servicio de IA de un solo proceso y escritura en la base), no Tomcat ni la red.
3. **Capacidad**: sin errores en ningún escalón; la degradación del tiempo de respuesta aparece
   entre 40 y 80 usuarios y la recuperación es inmediata.
4. **Estabilidad**: 2 minutos de carga constante sin degradación ni errores.
5. **Detector**: el sistema completo acierta el 100 % del lote; el modelo solo no llega al recall
   mínimo y las reglas cubren sus huecos (DEF-002). El 1,0 es optimista porque las reglas se
   ajustaron mirando este mismo lote.

## 8. Conclusión y recomendaciones

Se cumplen los criterios de salida de Smoke, Regression, Integration, E2E y de las suites no
funcionales del sistema completo. **No se cumple** el criterio de Sanity (100 % PASS) por DEF-001,
de severidad alta. Recomendaciones, en orden:

1. Corregir DEF-001 agregando BancoSol a `entidades_suplantadas` y a la lista blanca (migración
   nueva) y volver a ejecutar Sanity.
2. Planificar el reentrenamiento del modelo con typosquatting boliviano (DEF-002) y armar un lote
   de robustez nuevo, que no se haya usado para ajustar las reglas.
3. Si se esperan más de 40 usuarios simultáneos analizando URL nuevas, levantar el servicio de IA
   con varios procesos (`uvicorn --workers`) y volver a medir el estrés.
