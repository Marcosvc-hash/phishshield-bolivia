-- =====================================================================
-- PhishShield Bolivia - Migracion V10: desactivar los modelos de ejemplo
-- =====================================================================
-- Que hace:
--   Pone activo = FALSE en las filas de ejemplo de modelos_ia
--   ('phishshield-url' y 'phishshield-texto'). Esas filas vienen del seed
--   con metricas ilustrativas: no hay ningun modelo entrenado detras de
--   ellas (no existen en ia/ ni en el backend). 'phishshield-texto' 0.1.0
--   ("BERT multilingue") figuraba como activo, y eso daba a entender que
--   la plataforma analiza texto con ese modelo.
--
--   El unico modelo real es 'phishshield-url-ia' 1.0.0 (carpeta ia/), y
--   esta migracion no lo toca: sigue activo.
--
-- Que NO hace:
--   No borra filas ni cambia metricas. Los analisis de ejemplo que apuntan
--   a esos modelos (analisis.id_modelo) quedan igual.
--   El backend no lee la columna activo para decidir nada (solo busca el
--   modelo por nombre y version), asi que el analisis de URL no cambia.
--
-- Cuando aplicarla:
--   En bases creadas antes de este cambio, despues de V2 a V9. Las bases
--   nuevas ya reciben el valor correcto desde initdb/02_seed.sql.
--   Es idempotente: ejecutarla de nuevo no cambia nada.
--
-- Para deshacerla:
--   UPDATE modelos_ia SET activo = TRUE
--   WHERE nombre = 'phishshield-texto' AND version = '0.1.0';
--
-- Como aplicarla (desde la raiz del repositorio):
--   PowerShell:
--     Get-Content database\migraciones\V10__desactivar_modelos_de_ejemplo.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction
--   Git Bash / Linux / macOS:
--     docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction < database/migraciones/V10__desactivar_modelos_de_ejemplo.sql
--
-- Nota: este archivo se mantiene en ASCII a proposito (ver V2).
-- =====================================================================

UPDATE modelos_ia
SET activo = FALSE
WHERE activo
  AND nombre IN ('phishshield-url', 'phishshield-texto');

-- Comprobacion: el unico modelo activo debe ser phishshield-url-ia 1.0.0
SELECT nombre, version, tipo, algoritmo, activo
FROM modelos_ia
ORDER BY nombre, version;
