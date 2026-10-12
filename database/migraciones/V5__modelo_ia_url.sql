-- =====================================================================
-- PhishShield Bolivia - Migracion V5: modelo de IA de URL real
-- =====================================================================
-- Que hace:
--   1. Registra la regla IA_MODELO_URL (peso 40). Aporta probabilidad x 40
--      solo cuando la probabilidad alcanza el umbral del modelo.
--   2. Registra el modelo phishshield-url-ia 1.0.0 con sus metricas REALES
--      de prueba (ia/reporte_metricas.md, evaluacion unica sobre 32 856 hosts
--      con division agrupada por dominio). Las columnas *_val guardan las
--      metricas de prueba: el esquema no tiene columnas separadas.
--   3. Lo deja como el unico modelo activo de tipo 'url': primero desactiva
--      los de ejemplo del seed (respeta uq_modelos_activo_por_tipo).
--
-- Ojo al leer las metricas: son por fila. Ponderadas por dominio (cada
-- dominio pesa lo mismo) el recall baja a 0.48; ver ia/FICHA_MODELO.md.
--
-- Cuando aplicarla:
--   En bases creadas antes de este cambio, despues de V2 a V4. Las bases
--   nuevas ya reciben todo desde initdb/02_seed.sql. Es idempotente:
--   ejecutarla de nuevo no cambia nada. No borra datos.
--
-- Como aplicarla (desde la raiz del repositorio):
--   PowerShell:
--     Get-Content database\migraciones\V5__modelo_ia_url.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
--   Git Bash / Linux / macOS:
--     docker exec -i phishshield-db psql -U phishshield -d phishshield < database/migraciones/V5__modelo_ia_url.sql
--
-- Nota: este archivo se mantiene en ASCII a proposito (ver V2).
-- =====================================================================

BEGIN;

INSERT INTO reglas_deteccion (codigo, descripcion, categoria, peso)
VALUES ('IA_MODELO_URL', 'El modelo de IA estima phishing por el dominio (aporte = probabilidad x peso)', 'url', 40.00)
ON CONFLICT (codigo) DO NOTHING;

-- Desactivar cualquier otro modelo de URL activo (los de ejemplo del seed)
UPDATE modelos_ia
SET activo = FALSE
WHERE tipo = 'url'
  AND activo
  AND NOT (nombre = 'phishshield-url-ia' AND version = '1.0.0');

INSERT INTO modelos_ia (nombre, version, tipo, algoritmo, exactitud, precision_val, recall_val, f1_val,
                        umbral_phishing, activo, entrenado_en)
VALUES ('phishshield-url-ia', '1.0.0', 'url', 'Regresion Logistica + Platt (solo host)',
        89.55, 96.10, 80.57, 87.66, 55.27, TRUE, DATE '2026-10-12')
ON CONFLICT (nombre, version) DO NOTHING;

-- Por si la fila ya existia inactiva
UPDATE modelos_ia
SET activo = TRUE
WHERE nombre = 'phishshield-url-ia' AND version = '1.0.0' AND NOT activo;

COMMIT;

-- Comprobacion: IA_MODELO_URL 40.00 y un solo modelo url activo (phishshield-url-ia 1.0.0)
SELECT codigo, categoria, peso, activa
FROM reglas_deteccion
WHERE codigo = 'IA_MODELO_URL';

SELECT nombre, version, tipo, exactitud, precision_val, recall_val, f1_val, umbral_phishing, activo
FROM modelos_ia
WHERE tipo = 'url'
ORDER BY nombre, version;
