-- =====================================================================
-- PhishShield Bolivia - Migracion V2: regla URL_LISTA_NEGRA
-- =====================================================================
-- Que hace:
--   Registra la regla URL_LISTA_NEGRA (peso 70) que usa el backend para
--   marcar como phishing los dominios de dominios_lista con lista = 'negra'.
--
-- Cuando aplicarla:
--   Solo en bases creadas ANTES de este cambio. Las bases nuevas ya la
--   reciben desde initdb/02_seed.sql. Es idempotente: si la regla ya
--   existe, no hace nada (ON CONFLICT DO NOTHING) y no borra datos.
--
-- Como aplicarla (desde la raiz del repositorio):
--   PowerShell:
--     Get-Content database\migraciones\V2__regla_lista_negra.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
--   Git Bash / Linux / macOS:
--     docker exec -i phishshield-db psql -U phishshield -d phishshield < database/migraciones/V2__regla_lista_negra.sql
--
-- Nota: este archivo se mantiene en ASCII a proposito. Windows PowerShell 5.1
-- convierte los acentos en '?' al enviar texto por la tuberia a docker.
-- =====================================================================

INSERT INTO reglas_deteccion (codigo, descripcion, categoria, peso)
VALUES ('URL_LISTA_NEGRA', 'El dominio figura en la lista negra de PhishShield', 'url', 70.00)
ON CONFLICT (codigo) DO NOTHING;

-- Comprobacion: debe mostrar una fila con peso 70.00 y activa = t
SELECT codigo, categoria, peso, activa
FROM reglas_deteccion
WHERE codigo = 'URL_LISTA_NEGRA';
