-- =====================================================================
-- PhishShield Bolivia - Migracion V4: ajuste de reglas y lista blanca
-- =====================================================================
-- Que hace:
--   1. Fija el peso de URL_IP_LITERAL en 30 (el seed tenia 18).
--   2. Registra la regla URL_RUTA_SENSIBLE (peso 10).
--   3. Agrega visa.com a la lista blanca, para que typosquatting no lo
--      confunda con bisa.com (Banco BISA).
--
-- Cuando aplicarla:
--   En bases creadas antes de este cambio, despues de V2 y V3. Las bases
--   nuevas ya reciben todo desde initdb/02_seed.sql. Es idempotente:
--   ejecutarla de nuevo no cambia nada. No borra datos.
--
-- Como aplicarla (desde la raiz del repositorio):
--   PowerShell:
--     Get-Content database\migraciones\V4__ajuste_reglas_y_lista_blanca.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
--   Git Bash / Linux / macOS:
--     docker exec -i phishshield-db psql -U phishshield -d phishshield < database/migraciones/V4__ajuste_reglas_y_lista_blanca.sql
--
-- Nota: este archivo se mantiene en ASCII a proposito (ver V2).
-- =====================================================================

UPDATE reglas_deteccion
SET peso = 30.00
WHERE codigo = 'URL_IP_LITERAL'
  AND peso <> 30.00;

INSERT INTO reglas_deteccion (codigo, descripcion, categoria, peso)
VALUES ('URL_RUTA_SENSIBLE', 'La ruta contiene palabras sensibles (login, cuenta, clave, pin...)', 'url', 10.00)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO dominios_lista (dominio, lista, id_entidad, fuente, motivo)
VALUES ('visa.com', 'blanca', NULL, 'manual', 'Sitio oficial de Visa; se parece a bisa.com (Banco BISA)')
ON CONFLICT (dominio, lista) DO NOTHING;

-- Comprobacion: URL_IP_LITERAL 30.00, URL_RUTA_SENSIBLE 10.00 y visa.com activo
SELECT codigo, categoria, peso, activa
FROM reglas_deteccion
WHERE codigo IN ('URL_IP_LITERAL', 'URL_RUTA_SENSIBLE')
ORDER BY codigo;

SELECT dominio, lista, activo
FROM dominios_lista
WHERE dominio = 'visa.com';
