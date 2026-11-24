# Registro de defectos — PhishShield Bolivia

Defectos reales del sistema encontrados por el Centro de Pruebas QA. Un defecto se
registra cuando un caso da **FAIL funcional** y, al revisar la evidencia, la causa es
PhishShield (no la prueba ni el entorno). Los fallos de infraestructura (servicio caído,
sin conexión) no son defectos y no se registran aquí.

Severidad: **Crítica** (el sistema no se puede usar o expone datos), **Alta** (una
función principal da un resultado equivocado), **Media** (una función secundaria falla
o hay un rodeo), **Baja** (detalle de presentación o de texto).

Estado: **Abierto** → **En corrección** → **Corregido** → **Verificado** (el caso vuelve a
dar PASS en una ejecución posterior). También **Rechazado** (no era un defecto).

| Id | Caso | Severidad | Estado | Resumen |
|---|---|---|---|---|
| DEF-001 | TC-SAN-003 | Alta | Abierto | El typosquatting de BancoSol no se detecta: `bancosoI.com.bo` sale "seguro" |
| DEF-002 | TC-ROB-002 | Media | Abierto | El modelo de IA por sí solo no alcanza el recall mínimo (0,78 < 0,80): no reconoce typosquatting |
| DEF-003 | TC-REG-009 | Alta | Verificado | El Centro de Pruebas quedaba con una ejecución atascada "en curso" si la evidencia traía bytes nulos (PDF) |

---

## DEF-001 — El typosquatting de BancoSol no se detecta

| Campo | Valor |
|---|---|
| Caso que lo encontró | TC-SAN-003 (suite Sanity, tipo funcional) |
| Encontrado | 2026-11-24, ejecución de la suite Sanity contra `phishshield_qa` |
| Severidad | Alta: un phishing dirigido a clientes de un banco boliviano pasa como seguro (falso negativo) |
| Estado | Abierto (no se corrige durante la construcción del Centro de Pruebas, a propósito) |
| Componente | Motor de URL, regla `URL_TYPOSQUATTING`, datos de `entidades_suplantadas` |

**Pasos para reproducirlo**

1. Levantar el backend contra `phishshield_qa` y el servicio de IA.
2. Enviar `POST /api/analisis` con `{"url": "https://bancosoI.com.bo/login"}` (la "l" final
   de *bancosol* está cambiada por una "I" mayúscula, que en muchas fuentes se ve igual).
3. Ver el resultado.

**Resultado esperado:** `phishing`, con nivel de riesgo de 70 o más, porque el dominio
imita a `bancosol.com.bo` (Banco Solidario, BancoSol).

**Resultado obtenido:** `seguro`, nivel de riesgo **36,35**. Indicadores:
`URL_RUTA_SENSIBLE` (10) e `IA_MODELO_URL` (26,35: el modelo da 66 % de probabilidad de
phishing). No aparece `URL_TYPOSQUATTING`.

**Causa:** BancoSol no está en `entidades_suplantadas` (ni en `02_seed.sql` ni en ninguna
migración). La regla de typosquatting solo compara el dominio contra los dominios oficiales
de esa tabla (BNB, Banco Unión, Mercantil Santa Cruz, BISA, Ganadero, Tigo, Impuestos,
Entel, Segip y Ende), así que no tiene contra qué medir la distancia de `bancosoi.com.bo`.

**Corrección propuesta (pendiente de aprobación):** una migración nueva que agregue
BancoSol (`bancosol.com.bo`, tipo `banco`) a `entidades_suplantadas` y su dominio a la
lista blanca, más la misma fila en `02_seed.sql`. Después, volver a ejecutar la suite
Sanity: TC-SAN-003 debería dar PASS y el defecto pasaría a *Verificado*.

---

## DEF-002 — El modelo de IA por sí solo no alcanza el recall mínimo

| Campo | Valor |
|---|---|
| Caso que lo encontró | TC-ROB-002 (suite Robustez del modelo IA, tipo no funcional) |
| Encontrado | 2026-11-24, ejecución de la suite Robustez IA contra `phishshield_qa` |
| Severidad | Media: el modelo es un componente del motor; el sistema completo (TC-ROB-001) sí cumple porque las reglas compensan |
| Estado | Abierto |
| Componente | Servicio de IA (`ia/`, modelo `phishshield-url-ia` 1.0.0) |

**Pasos para reproducirlo**

1. Levantar el servicio de IA (`ia\.venv\Scripts\python.exe -m uvicorn servicio:app --port 8000`).
2. Ejecutar la suite *Robustez del modelo IA* desde el Centro de Pruebas (o enviar cada URL de
   `qa_lote_robustez` a `POST /predecir`).
3. Revisar la matriz de confusión de TC-ROB-002.

**Resultado esperado:** precisión y recall de 0,80 o más sobre las 80 URL del lote boliviano
(18 de phishing, 62 legítimas).

**Resultado obtenido:** precisión **0,8235**, recall **0,7778** (VP 14, FP 3, VN 59, FN 4), F1 0,80.

- Phishing no detectado (todos de la categoría *typosquatting*, 1 de 5 acertados):
  `bancounlon.com.bo` (p = 0,36), `www.bmcs.com.bo` (0,37), `blsa.com/banca` (0,19),
  `entel-bo.com/recarga` (0,20). El umbral del modelo es 0,5527.
- Legítimas marcadas como phishing: las tres URL de `bg.com.bo` (Banco Ganadero, p = 0,63).

**Causa probable:** el modelo solo mira caracteres del host (ver `ia/FICHA_MODELO.md`); un
typosquatting de una letra se parece demasiado al dominio real para que lo distinga, y el
dominio corto `bg.com.bo` se parece a los patrones de phishing del dataset de entrenamiento.
En el sistema completo, la regla `URL_TYPOSQUATTING` y la lista blanca cubren estos casos
(TC-ROB-001: precisión y recall 1,0).

**Corrección propuesta:** reentrenar con ejemplos de typosquatting de marcas bolivianas o
agregar al modelo una característica de distancia a los dominios oficiales; mientras tanto,
documentar en la ficha del modelo que no debe usarse sin las reglas.

---

## DEF-003 — Una evidencia binaria dejaba la ejecución atascada "en curso"

| Campo | Valor |
|---|---|
| Caso que lo encontró | TC-REG-009 (suite Regression), en su primera ejecución real |
| Encontrado | 2026-11-24, fase 9, contra `phishshield_qa` |
| Severidad | Alta: una ejecución que queda activa bloquea todas las siguientes (solo se admite una a la vez) |
| Estado | **Verificado** (corregido en la fase 9 y la suite volvió a dar 9/9 PASS) |
| Componente | Centro de Pruebas QA: `ClienteApiQA` (evidencia de API) y `CorrerEjecucionUseCase` |

**Pasos para reproducirlo (antes de la corrección)**

1. Ejecutar la suite Regression con TC-REG-009, cuyo último paso descarga un PDF.
2. El cliente de API copiaba el cuerpo de la respuesta a la evidencia del paso; el PDF trae bytes
   nulos (`0x00`).

**Resultado esperado:** la ejecución termina (PASS o FAIL) y se puede lanzar otra.

**Resultado obtenido:** PostgreSQL rechazó el texto (`invalid byte sequence for encoding "UTF8":
0x00`), falló el guardado del avance y también el guardado final; la ejecución quedó `en_curso`
para siempre y la pantalla no dejaba lanzar otra hasta reiniciar el backend (al reiniciar,
`MarcarInterrumpidasAlArrancar` la marcó FALLIDA, como estaba previsto).

**Corrección:**

1. Causa: una respuesta binaria (PDF, imagen) se resume en la evidencia como
   `[contenido binario: N bytes, tipo]`, y el dominio quita el carácter nulo de cualquier texto
   antes de guardarlo (`Textos.opcional`).
2. Robustez: si el guardado final de una ejecución falla por cualquier motivo, se fuerza el estado
   FALLIDA con el motivo (`EjecucionRepositoryPort.marcarFallida`), para que nunca quede activa.
3. Pruebas de regresión: `CasosDeUsoQaTest.si_no_se_puede_guardar_el_final_la_ejecucion_queda_fallida_y_no_bloquea`
   y `JUnitEjecutorPruebasAdapterTest.las_respuestas_binarias_no_van_a_la_evidencia_y_el_caracter_nulo_se_limpia`.
