-- =====================================================================
-- PhishShield Bolivia - Migracion V6: ende.bo en la lista blanca
-- =====================================================================
-- Que hace:
--   Agrega ende.bo (ENDE, Empresa Nacional de Electricidad) a la lista
--   blanca. URL_TYPOSQUATTING lo marcaba como sospechoso (45 puntos)
--   porque ende.bo esta a distancia 2 de entel.bo. Verificado el
--   2026-10-12: https://www.ende.bo/ responde y es el sitio oficial.
--   Ver ia/datos_evaluacion/puntajes_peso_ia.md.
--
-- Cuando aplicarla:
--   En bases creadas antes de este cambio, despues de V2 a V5. Las bases
--   nuevas ya reciben todo desde initdb/02_seed.sql. Es idempotente:
--   ejecutarla de nuevo no cambia nada. No borra datos.
--
-- Como aplicarla (desde la raiz del repositorio):
--   PowerShell:
--     Get-Content database\migraciones\V6__lista_blanca_ende.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
--   Git Bash / Linux / macOS:
--     docker exec -i phishshield-db psql -U phishshield -d phishshield < database/migraciones/V6__lista_blanca_ende.sql
--
-- Nota: este archivo se mantiene en ASCII a proposito (ver V2).
-- =====================================================================

INSERT INTO dominios_lista (dominio, lista, id_entidad, fuente, motivo)
VALUES ('ende.bo', 'blanca', NULL, 'manual', 'Sitio oficial de ENDE; typosquatting lo confundia con entel.bo (Entel)')
ON CONFLICT (dominio, lista) DO NOTHING;

-- Comprobacion: ende.bo activo en la lista blanca
SELECT dominio, lista, activo
FROM dominios_lista
WHERE dominio = 'ende.bo';
