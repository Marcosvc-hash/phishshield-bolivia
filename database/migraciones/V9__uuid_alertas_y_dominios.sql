-- =====================================================================
-- V9: identificador publico (uuid_publico) para alertas y dominios_lista
--
-- La API nunca expone el BIGINT interno, y estas dos tablas no tenian un
-- uuid. Los modulos de gestion de alertas y de listas de dominios las
-- identifican por esta columna nueva.
--
-- Solo AGREGA dos columnas y dos indices unicos: no borra ni cambia datos.
-- ADD COLUMN con DEFAULT gen_random_uuid() le pone un uuid distinto a cada
-- fila que ya existe. Es idempotente (IF NOT EXISTS): se puede ejecutar mas
-- de una vez.
--
-- Para deshacerla (se pierden solo los uuid generados):
--   DROP INDEX IF EXISTS uq_alertas_uuid;
--   DROP INDEX IF EXISTS uq_dominios_lista_uuid;
--   ALTER TABLE alertas        DROP COLUMN IF EXISTS uuid_publico;
--   ALTER TABLE dominios_lista DROP COLUMN IF EXISTS uuid_publico;
--
-- Archivo en ASCII a proposito: PowerShell 5.1 cambia los acentos por "?".
-- =====================================================================

ALTER TABLE alertas
    ADD COLUMN IF NOT EXISTS uuid_publico UUID NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE dominios_lista
    ADD COLUMN IF NOT EXISTS uuid_publico UUID NOT NULL DEFAULT gen_random_uuid();

CREATE UNIQUE INDEX IF NOT EXISTS uq_alertas_uuid        ON alertas(uuid_publico);
CREATE UNIQUE INDEX IF NOT EXISTS uq_dominios_lista_uuid ON dominios_lista(uuid_publico);

COMMENT ON COLUMN alertas.uuid_publico        IS 'Identificador expuesto en la API (V9).';
COMMENT ON COLUMN dominios_lista.uuid_publico IS 'Identificador expuesto en la API (V9).';

-- Comprobacion: ninguna fila sin uuid y ningun uuid repetido
SELECT 'alertas' AS tabla, COUNT(*) AS filas, COUNT(DISTINCT uuid_publico) AS uuids FROM alertas
UNION ALL
SELECT 'dominios_lista', COUNT(*), COUNT(DISTINCT uuid_publico) FROM dominios_lista;
