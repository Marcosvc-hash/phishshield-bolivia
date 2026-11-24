-- =====================================================================
-- PhishShield Bolivia - Migracion V16: BancoSol como entidad oficial (DEF-001)
-- =====================================================================
-- Que hace:
--   1. Agrega BancoSol (Banco Solidario S.A.) a entidades_suplantadas, con
--      dominio oficial bancosol.com.bo y tipo 'banco'. Desde ahora
--      URL_TYPOSQUATTING detecta imitaciones como bancosoI.com.bo (la "l"
--      cambiada por una "I" mayuscula), que antes salia "seguro".
--   2. Agrega bancosol.com.bo a la lista blanca, vinculado a la entidad,
--      como las demas entidades (en bases nuevas lo hace el INSERT ... SELECT
--      de dominios oficiales de initdb/02_seed.sql).
--   3. Si el paso 2 inserto la fila, registra en auditoria un evento
--      DOMINIO_AGREGADO (sin usuario: lo hizo la migracion). Es lo mismo que
--      escribe el panel al agregar un dominio, y sirve para que la cache de
--      analisis de 24 h no siga devolviendo resultados calculados antes de
--      este cambio (CambiosListasPort lee esa fecha).
--
-- Por que:
--   Defecto DEF-001, encontrado por el caso TC-SAN-003 del Centro de
--   Pruebas: la regla de typosquatting solo compara contra los dominios de
--   entidades_suplantadas, y BancoSol no estaba.
--
-- Medicion previa (2026-11-24, motor real, ReglaTyposquatting con y sin
-- BancoSol): sobre 235 448 URL (ia/datos_evaluacion, qa_lote_robustez y
-- PhiUSIIL) cambian dos. Una es bancosoI.com.bo (lo buscado). La otra es
-- www.xangosol.com, legitima en PhiUSIIL, que pasa a sumar 45 puntos
-- ("xangosol" esta a distancia 2 de la marca "bancosol"). No esta en
-- ningun conjunto boliviano; si molesta, se corrige agregandola a la lista
-- blanca, como se hizo con visa.com.
--
-- Que NO hace:
--   No borra filas ni cambia el esquema.
--
-- Cuando aplicarla:
--   Despues de V15. Las bases nuevas ya traen la entidad desde
--   initdb/02_seed.sql (la fila de auditoria no hace falta ahi: no hay
--   cache previa). Es idempotente: ON CONFLICT DO NOTHING, y la fila de
--   auditoria solo se escribe si se inserto el dominio.
--
-- Como aplicarla (desde la raiz del repositorio):
--   PowerShell:
--     Get-Content database\migraciones\V16__entidad_bancosol.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction
--   Git Bash / Linux / macOS:
--     docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction < database/migraciones/V16__entidad_bancosol.sql
--
-- Nota: este archivo se mantiene en ASCII a proposito (ver V2).
-- =====================================================================

INSERT INTO entidades_suplantadas (nombre, tipo, dominio_oficial)
VALUES ('BancoSol', 'banco', 'bancosol.com.bo')
ON CONFLICT (nombre) DO NOTHING;

-- Si el dominio ya estaba en la lista blanca sin entidad, se vincula
UPDATE dominios_lista
SET id_entidad = (SELECT id_entidad FROM entidades_suplantadas WHERE nombre = 'BancoSol')
WHERE dominio = 'bancosol.com.bo' AND lista = 'blanca' AND id_entidad IS NULL;

WITH nuevo AS (
    INSERT INTO dominios_lista (dominio, lista, id_entidad, fuente, motivo)
    SELECT 'bancosol.com.bo', 'blanca', id_entidad, 'semilla', 'Dominio oficial verificado'
    FROM entidades_suplantadas WHERE nombre = 'BancoSol'
    ON CONFLICT (dominio, lista) DO NOTHING
    RETURNING id_dominio, dominio, lista, motivo
)
INSERT INTO auditoria (id_usuario, accion, entidad, id_entidad_afectada, detalle)
SELECT NULL, 'DOMINIO_AGREGADO', 'dominios_lista', id_dominio,
       jsonb_build_object('dominio', dominio, 'lista', lista, 'motivo', motivo,
                          'entidad', 'BancoSol', 'origen', 'migracion V16 (DEF-001)')
FROM nuevo;

-- Comprobacion: BancoSol con bancosol.com.bo, vinculado en la lista blanca
SELECT e.nombre, e.tipo, e.dominio_oficial, d.lista, d.activo,
       d.id_entidad = e.id_entidad AS vinculado
FROM entidades_suplantadas e
JOIN dominios_lista d ON d.dominio = e.dominio_oficial
WHERE e.nombre = 'BancoSol';
