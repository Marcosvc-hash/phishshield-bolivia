-- =====================================================================
-- PhishShield Bolivia - Migracion V3: peso de URL_TYPOSQUATTING = 45
-- =====================================================================
-- Que hace:
--   Sube el peso de URL_TYPOSQUATTING de 25 a 45. Con 25, un dominio que
--   imita a un banco y no esta en la lista negra quedaba como "seguro";
--   con 45 llega al menos a "sospechoso" (umbral 40).
--
-- Cuando aplicarla:
--   En bases creadas antes de este cambio. Las bases nuevas ya reciben 45
--   desde initdb/02_seed.sql. Es idempotente: si el peso ya es 45, el
--   UPDATE no modifica ninguna fila. No borra datos.
--
-- Como aplicarla (desde la raiz del repositorio):
--   PowerShell:
--     Get-Content database\migraciones\V3__ajustar_peso_typosquatting.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
--   Git Bash / Linux / macOS:
--     docker exec -i phishshield-db psql -U phishshield -d phishshield < database/migraciones/V3__ajustar_peso_typosquatting.sql
--
-- Nota: este archivo se mantiene en ASCII a proposito (ver V2).
-- =====================================================================

UPDATE reglas_deteccion
SET peso = 45.00
WHERE codigo = 'URL_TYPOSQUATTING'
  AND peso <> 45.00;

-- Comprobacion: debe mostrar una fila con peso 45.00
SELECT codigo, categoria, peso, activa
FROM reglas_deteccion
WHERE codigo = 'URL_TYPOSQUATTING';
