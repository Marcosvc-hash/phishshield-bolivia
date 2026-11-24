# Informe de ejecución — PhishShield Bolivia

Ejecución completa de las 9 suites del Centro de Pruebas QA, una tras otra, el **2026-11-24
entre las 11:50 y las 11:53** (hora de Bolivia), contra la base `phishshield_qa`, con el
catálogo de **28 casos**. Es la corrida de regresión **después de corregir DEF-001** (migración
V16, BancoSol en `entidades_suplantadas`). La corrida anterior (10:38, antes de la corrección)
queda resumida en la sección 6.

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
| Base | `phishshield_qa` con V13 a V16 aplicadas |
| Rama | `feature/qa-automation` (cierre de pendientes) |

## 2. Resumen

| Indicador | Valor |
|---|---|
| Casos ejecutados | 28 de 28 |
| PASS | **27** |
| FAIL | **1** (funcional: DEF-002, defecto real registrado y planificado) |
| SKIPPED | 0 |
| NOT_RUN | 0 |
| Porcentaje de éxito | 96,4 % |
| Duración total | 2 min 51 s (suma de las suites; el soak ocupa 2 min) |

Antes de la suite completa se ejecutó **TC-SAN-003 solo** (11:49, ejecución
`83c3cd5d-29fb-4e7e-b6da-3f592c1b7d1a`): **PASS**.

## 3. Resultados por suite

| Tipo | Suite | PASS | FAIL | SKIPPED | NOT_RUN | Resultado global | Duración | Id de la ejecución |
|---|---|---|---|---|---|---|---|---|
| Funcional | Smoke | 6 | 0 | 0 | 0 | Aprobada | 6,6 s | `2571a147-061a-48e2-b77f-64ac254259f2` |
| Funcional | Sanity | 3 | 0 | 0 | 0 | Aprobada | 0,8 s | `a62e7147-c106-4b2f-a383-036a323a1229` |
| Funcional | Regression | 9 | 0 | 0 | 0 | Aprobada | 8,3 s | `158ef0d4-7c09-465d-9235-3420d21eae73` |
| Funcional | Integration | 3 | 0 | 0 | 0 | Aprobada | 0,9 s | `b19024b6-74e1-46b4-a29e-a3d6f9f7c017` |
| Funcional | E2E | 1 | 0 | 0 | 0 | Aprobada | 3,5 s | `3220c5fd-a57e-4b44-a95a-2d5e94a6948e` |
| No funcional | Performance | 2 | 0 | 0 | 0 | Aprobada | 3,5 s | `51af8734-f738-400a-80d1-dc4b58ffb91b` |
| No funcional | Estrés | 1 | 0 | 0 | 0 | Aprobada | 20,1 s | `67b0b18e-3a0f-4781-95b4-1308ee9c26ab` |
| No funcional | Soak | 1 | 0 | 0 | 0 | Aprobada | 2 min 0 s | `8f4c1c8b-c0c0-443b-bd05-fd23ff82edc2` |
| No funcional | Robustez IA | 1 | 1 | 0 | 0 | Con fallos | 7,5 s | `c2f2e763-570e-4332-b63b-6e0427f01b13` |

Los reportes PDF (detallado, con capturas y evidencias) y HTML (resumen) de cada ejecución están
en `backend/qa-evidencias/{id}/` y se descargan desde `/admin/qa`.

## 4. Casos con FAIL

| Caso | Tipo de fallo | Qué pasó | Defecto |
|---|---|---|---|
| TC-ROB-002 | Funcional | El modelo de IA por sí solo tuvo recall **0,7778** (< 0,80): no detecta 4 de 5 typosquatting y marca como phishing `bg.com.bo` | DEF-002 (media, abierto con plan de corrección) |

**TC-SAN-003 ahora pasa:** `https://bancosoI.com.bo/login` sale **phishing** con **81,35**
puntos (typosquatting 45 + IA 26,35 + ruta sensible 10); antes salía seguro con 36,35. DEF-001
queda *Verificado*. Detalle, pasos para reproducir y plan de DEF-002: `REGISTRO_DEFECTOS.md`.

## 5. Métricas no funcionales

**Performance (20 usuarios concurrentes × 5 peticiones)**

| Caso | Peticiones | p50 | p95 (umbral 2000 ms) | p99 | Throughput | Error |
|---|---|---|---|---|---|---|
| TC-PERF-001 URL nuevas (reglas + IA) | 100 | 431 ms | **801 ms** ✔ | 912 ms | 40,7 pet/s | 0 % |
| TC-PERF-002 URL de la caché | 100 | 53 ms | **100 ms** ✔ | 141 ms | 332,2 pet/s | 0 % |

**Estrés (5 peticiones por usuario, URL nuevas)**

| Escalón | Usuarios | p95 | Throughput | Error |
|---|---|---|---|---|
| 1 | 10 | 288 ms | 36,3 pet/s | 0 % |
| 2 | 20 | 820 ms | 40,6 pet/s | 0 % |
| 3 | 40 | 1997 ms | 40,1 pet/s | 0 % |
| 4 | 80 | **2200 ms** ✘ (se degrada) | 44,6 pet/s | 0 % |

Recuperación: una petición normal después del último escalón respondió en **91 ms** → el caso pasa.

**Soak (5 usuarios constantes, 2 minutos, pausa de 200 ms)**: 1562 peticiones, 13,0 pet/s, 0 %
de error; p95 del primer tramo 211 ms y del último 206 ms (crecimiento 0 %, umbral 50 %).

**Robustez del detector (80 URL: 18 phishing, 62 legítimas)**

| Caso | Precisión | Recall | F1 | Exactitud | VP | FP | VN | FN |
|---|---|---|---|---|---|---|---|---|
| TC-ROB-001 Sistema completo | 1,0000 | 1,0000 | 1,0000 | 1,0000 | 18 | 0 | 62 | 0 |
| TC-ROB-002 Solo el modelo | 0,8235 | **0,7778** | 0,8000 | 0,9125 | 14 | 3 | 59 | 4 |

## 6. Comparación con la corrida anterior (10:38, antes de corregir DEF-001)

| Indicador | 10:38 | 11:50 |
|---|---|---|
| PASS / FAIL | 26 / 2 | **27 / 1** (TC-SAN-003 pasa a PASS) |
| TC-SAN-003 | FAIL, seguro 36,35 | PASS, phishing 81,35 |
| p95 PERF-001 | 1190 ms | 801 ms |
| Escalón de degradación (estrés) | 4 (80 usuarios, 2374 ms) | 4 (80 usuarios, 2200 ms) |
| Soak: crecimiento del p95 | 0 % | 0 % |
| Robustez (ROB-001 / ROB-002) | 1,0 / recall 0,7778 | 1,0 / recall 0,7778 (sin cambios) |

Ningún caso que pasaba antes falló ahora: la corrección no introdujo regresiones. Las métricas
de carga varían entre corridas (el generador comparte la máquina con el sistema probado); la
degradación sigue apareciendo entre 40 y 80 usuarios concurrentes con URL nuevas.

## 7. Análisis

1. **Funcionalidad**: las 5 suites funcionales pasaron completas (22 de 22), incluidos los 7
   casos de interfaz en Playwright y la Sanity, que ahora detecta el typosquatting de BancoSol.
2. **Rendimiento**: con 20 usuarios el análisis completo responde con p95 de 0,8 a 1,2 s según la
   corrida, por debajo del umbral. Desde la caché es unas 8 veces más rápido. El throughput con URL
   nuevas se estanca en 36-45 pet/s sin importar los usuarios: el cuello de botella es el cálculo
   completo (servicio de IA de un solo proceso y escritura en la base), no Tomcat ni la red.
3. **Capacidad**: sin errores en ningún escalón; la degradación del tiempo de respuesta aparece
   entre 40 y 80 usuarios y la recuperación es inmediata.
4. **Estabilidad**: 2 minutos de carga constante sin degradación ni errores.
5. **Detector**: el sistema completo acierta el 100 % del lote; el modelo solo no llega al recall
   mínimo y las reglas cubren sus huecos (DEF-002). El 1,0 es optimista porque las reglas se
   ajustaron mirando este mismo lote.

## 8. Conclusión y recomendaciones

Se cumplen los criterios de salida de **todas las suites funcionales** (Smoke, Sanity, Regression,
Integration y E2E al 100 %) y de las no funcionales del sistema completo. Solo queda abierto
DEF-002 (severidad media), que afecta al modelo de IA aislado y no al veredicto del sistema.
Recomendaciones, en orden:

1. Ejecutar el plan de DEF-002 (`REGISTRO_DEFECTOS.md`): conjunto de entrenamiento con
   typosquatting boliviano generado, sin tocar el lote de robustez, y modelo 1.1.0.
2. Decidir si `xangosol.com` (legítimo en PhiUSIIL, que desde V16 suma 45 puntos por parecerse a
   "bancosol") va a la lista blanca.
3. Si se esperan más de 40 usuarios simultáneos analizando URL nuevas, levantar el servicio de IA
   con varios procesos (`uvicorn --workers`) y volver a medir el estrés.
