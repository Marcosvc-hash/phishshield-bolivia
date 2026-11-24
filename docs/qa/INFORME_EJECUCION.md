# Informe de ejecución — PhishShield Bolivia

Ejecución completa de las 9 suites del Centro de Pruebas QA, una tras otra, el **2026-11-24
entre las 01:42 y las 01:45** (hora de Bolivia), contra la base `phishshield_qa`.

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
| Rama / versión | `feature/qa-automation` |

## 2. Resumen

| Indicador | Valor |
|---|---|
| Casos ejecutados | 27 de 27 |
| PASS | **25** |
| FAIL | **2** (ambos funcionales: defectos reales registrados) |
| SKIPPED | 0 |
| NOT_RUN | 0 |
| Porcentaje de éxito | 92,6 % |
| Duración total | 2 min 48 s (el soak ocupa 2 min) |

## 3. Resultados por suite

| Tipo | Suite | PASS | FAIL | SKIPPED | NOT_RUN | Resultado global | Duración | Id de la ejecución |
|---|---|---|---|---|---|---|---|---|
| Funcional | Smoke | 6 | 0 | 0 | 0 | Aprobada | 6,3 s | `c0d3adfa-83f4-445d-8f9f-9f1b419430fa` |
| Funcional | Sanity | 2 | 1 | 0 | 0 | Con fallos | 0,5 s | `a6802912-84eb-4a86-8d6d-c18efcd7d8b7` |
| Funcional | Regression | 8 | 0 | 0 | 0 | Aprobada | 6,1 s | `ef82c4a1-e162-42ff-a8c4-692696944f2c` |
| Funcional | Integration | 3 | 0 | 0 | 0 | Aprobada | 1,1 s | `6f53936f-a5b6-406a-b337-e9dad65a6a42` |
| Funcional | E2E | 1 | 0 | 0 | 0 | Aprobada | 2,9 s | `0cba0b74-df0f-42f9-8c06-0fd5615a5b66` |
| No funcional | Performance | 2 | 0 | 0 | 0 | Aprobada | 3,3 s | `ba2d1e7d-eb5a-4466-a482-5a318d23c5e7` |
| No funcional | Estrés | 1 | 0 | 0 | 0 | Aprobada | 10,3 s | `20c4b8af-54cd-4485-bf74-0dbea9121abf` |
| No funcional | Soak | 1 | 0 | 0 | 0 | Aprobada | 2 min 0 s | `79e7e838-7f62-40dc-a9f4-ad370dac88ca` |
| No funcional | Robustez IA | 1 | 1 | 0 | 0 | Con fallos | 2,7 s | `832a7a1a-6a53-4849-93bb-adcaf111e47b` |

Los reportes PDF (detallado, con capturas y evidencias) y HTML (resumen) de cada ejecución están
en `backend/qa-evidencias/{id}/` y se descargan desde `/admin/qa`.

## 4. Casos con FAIL

| Caso | Tipo de fallo | Qué pasó | Defecto |
|---|---|---|---|
| TC-SAN-003 | Funcional | `https://bancosoI.com.bo/login` salió **seguro** (36,35 puntos: ruta sensible 10 + IA 26,35); se esperaba phishing (≥ 70) | DEF-001 (alta) |
| TC-ROB-002 | Funcional | El modelo de IA por sí solo tuvo recall **0,7778** (< 0,80): no detecta 4 de 5 typosquatting y marca como phishing `bg.com.bo` | DEF-002 (media) |

Detalle, pasos para reproducir y corrección propuesta: `REGISTRO_DEFECTOS.md`.

## 5. Métricas no funcionales

**Performance (20 usuarios concurrentes × 5 peticiones)**

| Caso | Peticiones | p50 | p95 (umbral 2000 ms) | p99 | Throughput | Error |
|---|---|---|---|---|---|---|
| TC-PERF-001 URL nuevas (reglas + IA) | 100 | 448 ms | **883 ms** ✔ | 1061 ms | 39,5 pet/s | 0 % |
| TC-PERF-002 URL de la caché | 100 | 58 ms | **95 ms** ✔ | 109 ms | 316,5 pet/s | 0 % |

**Estrés (5 peticiones por usuario, URL nuevas)**

| Escalón | Usuarios | p95 | Throughput | Error |
|---|---|---|---|---|
| 1 | 10 | 258 ms | 47,6 pet/s | 0 % |
| 2 | 20 | 684 ms | 45,1 pet/s | 0 % |
| 3 | 40 | **2178 ms** ✘ (se degrada) | 41,0 pet/s | 0 % |

Al degradarse en el escalón 3 la prueba no subió a 80 usuarios. Recuperación: una petición normal
2 s después respondió 200 en **46 ms** → el caso pasa (el sistema se recupera).

**Soak (5 usuarios constantes, 2 minutos, pausa de 200 ms)**: 1946 peticiones, 16,2 pet/s, 0 %
de error; p95 del primer tramo 132 ms y del último 131 ms (crecimiento 0 %, umbral 50 %).

**Robustez del detector (80 URL: 18 phishing, 62 legítimas)**

| Caso | Precisión | Recall | F1 | Exactitud | VP | FP | VN | FN |
|---|---|---|---|---|---|---|---|---|
| TC-ROB-001 Sistema completo | 1,0000 | 1,0000 | 1,0000 | 1,0000 | 18 | 0 | 62 | 0 |
| TC-ROB-002 Solo el modelo | 0,8235 | **0,7778** | 0,8000 | 0,9125 | 14 | 3 | 59 | 4 |

## 6. Análisis

1. **Funcionalidad**: las suites Smoke, Regression, Integration y E2E pasaron completas, incluidos
   los 7 casos de interfaz con Playwright. La única falla funcional es de datos de detección
   (DEF-001): BancoSol no está entre las entidades que el motor sabe proteger.
2. **Rendimiento**: con 20 usuarios el análisis completo responde con p95 de 883 ms, menos de la
   mitad del umbral. Desde la caché es 9 veces más rápido. El throughput con URL nuevas se estanca
   en 40-48 pet/s sin importar los usuarios: el cuello de botella es el cálculo completo (consulta
   al servicio de IA de un solo proceso y escritura en la base), no Tomcat ni la red.
3. **Capacidad**: la degradación aparece entre 20 y 40 usuarios concurrentes con URL nuevas. En
   una corrida anterior del mismo día apareció recién con 80 (p95 de 1632 ms con 40): el punto
   exacto varía entre corridas porque el generador comparte la máquina con el sistema probado.
   No hubo errores en ningún escalón y la recuperación fue inmediata.
4. **Estabilidad**: en 2 minutos de carga constante no hubo degradación ni errores.
5. **Detector**: el sistema completo acierta el 100 % del lote, pero el modelo solo no llega al
   umbral de recall; las reglas (typosquatting y lista blanca) son las que cubren sus huecos
   (DEF-002). Como las reglas se ajustaron mirando este mismo lote, el 1,0 es optimista.

## 7. Conclusión y recomendaciones

Se cumplen los criterios de salida de las suites Smoke, Regression, Integration, E2E y de las no
funcionales del sistema completo. **No se cumple** el criterio de Sanity (100 % PASS) por DEF-001,
de severidad alta. Recomendaciones, en orden:

1. Corregir DEF-001 agregando BancoSol a `entidades_suplantadas` y a la lista blanca (migración
   nueva) y volver a ejecutar Sanity.
2. Planificar el reentrenamiento del modelo con typosquatting boliviano (DEF-002) y armar un lote de
   robustez nuevo, que no se haya usado para ajustar las reglas.
3. Si se espera más de 20 usuarios simultáneos analizando URL nuevas, levantar el servicio de IA con
   varios procesos (`uvicorn --workers`) y volver a medir el estrés.
