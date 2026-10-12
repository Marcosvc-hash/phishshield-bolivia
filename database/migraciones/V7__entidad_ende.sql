-- =====================================================================
-- PhishShield Bolivia - Migracion V7: Ende como entidad oficial
-- =====================================================================
-- Que hace:
--   1. Agrega Ende (Empresa Nacional de Electricidad, empresa publica) a
--      entidades_suplantadas con dominio oficial ende.bo y tipo 'gobierno'.
--      El nombre sigue la convencion de Entel y Segip. Desde ahora
--      URL_TYPOSQUATTING detecta imitaciones como ende-bo.com o
--      pagos-ende.xyz.
--   2. Vincula a la entidad la fila de ende.bo en la lista blanca (la creo
--      V6 sin entidad). Si la base no tiene V6, crea la fila ya vinculada.
--
-- Nota sobre la fila de ende.bo en la lista blanca:
--   - En bases con V6, la fila conserva fuente='manual' y su motivo
--     original ("typosquatting lo confundia con entel.bo"); solo se le
--     agrega id_entidad. Se mantiene a proposito: guarda por que se agrego.
--   - En bases nuevas (initdb/02_seed.sql) la crea el INSERT ... SELECT de
--     dominios oficiales, con fuente='semilla' y motivo 'Dominio oficial
--     verificado', como las demas entidades.
--   Dominio, lista, id_entidad y activo quedan iguales en los dos casos.
--
-- Medicion previa (2026-10-12, motor real): agregar Ende no cambio el
-- veredicto de ninguna de las 80 URL de ia/datos_evaluacion ni marco
-- ningun host nuevo entre los 219 036 de PhiUSIIL.
--
-- Cuando aplicarla:
--   En bases creadas antes de este cambio, despues de V2 a V6. Las bases
--   nuevas ya reciben todo desde initdb/02_seed.sql. Es idempotente:
--   ejecutarla de nuevo no cambia nada. No borra datos.
--
-- Como aplicarla (desde la raiz del repositorio):
--   PowerShell:
--     Get-Content database\migraciones\V7__entidad_ende.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
--   Git Bash / Linux / macOS:
--     docker exec -i phishshield-db psql -U phishshield -d phishshield < database/migraciones/V7__entidad_ende.sql
--
-- Nota: este archivo se mantiene en ASCII a proposito (ver V2).
-- =====================================================================

BEGIN;

INSERT INTO entidades_suplantadas (nombre, tipo, dominio_oficial)
VALUES ('Ende', 'gobierno', 'ende.bo')
ON CONFLICT (nombre) DO NOTHING;

-- Vincula a la entidad la fila de la lista blanca que creo V6
UPDATE dominios_lista
SET id_entidad = (SELECT id_entidad FROM entidades_suplantadas WHERE nombre = 'Ende')
WHERE dominio = 'ende.bo' AND lista = 'blanca' AND id_entidad IS NULL;

-- Por si la base no tiene V6: crea la fila ya vinculada
INSERT INTO dominios_lista (dominio, lista, id_entidad, fuente, motivo)
SELECT 'ende.bo', 'blanca', id_entidad, 'manual', 'Dominio oficial de Ende'
FROM entidades_suplantadas WHERE nombre = 'Ende'
ON CONFLICT (dominio, lista) DO NOTHING;

COMMIT;

-- Comprobacion: Ende con ende.bo, y ende.bo en la lista blanca vinculado a Ende
SELECT e.nombre, e.tipo, e.dominio_oficial, d.lista, d.fuente,
       d.id_entidad = e.id_entidad AS vinculado
FROM entidades_suplantadas e
JOIN dominios_lista d ON d.dominio = e.dominio_oficial
WHERE e.nombre = 'Ende';
