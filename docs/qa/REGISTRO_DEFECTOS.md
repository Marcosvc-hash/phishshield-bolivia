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
