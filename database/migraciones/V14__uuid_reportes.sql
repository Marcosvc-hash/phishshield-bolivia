-- =====================================================================
-- PhishShield Bolivia - Migracion V14: uuid publico en reportes
-- =====================================================================
-- Que hace:
--   Agrega a reportes la columna uuid_publico (con indice unico), igual que
--   V9 hizo con alertas y dominios_lista.
--
-- Por que:
--   La HU-17 "Generar reportes por empresa" expone los reportes por la API,
--   y la API nunca muestra el BIGINT interno: usa uuid_publico como id.
--
-- Que NO hace:
--   No borra filas ni cambia datos existentes. Cada fila que ya exista recibe
--   un uuid distinto.
--
-- Cuando aplicarla:
--   Despues de V13. El backend con el modulo de reportes la necesita: sin
--   ella, las paginas de reportes responden error. Las bases nuevas ya traen
--   la columna desde initdb/01_schema.sql.
--   Es idempotente: ejecutarla de nuevo no cambia nada.
-- =====================================================================

ALTER TABLE reportes
    ADD COLUMN IF NOT EXISTS uuid_publico UUID NOT NULL DEFAULT gen_random_uuid();

CREATE UNIQUE INDEX IF NOT EXISTS uq_reportes_uuid ON reportes(uuid_publico);

COMMENT ON COLUMN reportes.uuid_publico IS 'Identificador expuesto en la API (V14).';

SELECT COUNT(*) AS filas, COUNT(DISTINCT uuid_publico) AS uuids FROM reportes;
