-- =====================================================================
-- 03: vistas del panel de administracion (bases NUEVAS).
-- Mismo contenido que migraciones/V8__vistas_panel_admin.sql, que es la
-- version para bases que ya existen. Si cambia una, cambiar la otra.
-- =====================================================================

-- Analisis por dia y por empresa. El dia se calcula en hora de Bolivia:
-- un analisis de las 22:00 en La Paz (02:00 UTC del dia siguiente) cuenta
-- para el dia correcto. id_empresa es NULL para los analisis anonimos.
CREATE OR REPLACE VIEW v_analisis_por_dia AS
SELECT
    (a.fecha_analisis AT TIME ZONE 'America/La_Paz')::date    AS dia,
    a.id_empresa,
    COUNT(*)                                                 AS total,
    COUNT(*) FILTER (WHERE a.resultado = 'phishing')         AS phishing,
    COUNT(*) FILTER (WHERE a.resultado = 'sospechoso')       AS sospechosos,
    COUNT(*) FILTER (WHERE a.resultado = 'seguro')           AS seguros
FROM analisis a
GROUP BY 1, 2;


-- Entidades bolivianas suplantadas, por empresa. Ningun campo de analisis
-- guarda la entidad, asi que se deduce de dos fuentes (resultado aproximado):
--   1) el dominio analizado esta en la lista negra vinculado a una entidad;
--   2) el indicador URL_TYPOSQUATTING menciona el dominio oficial o el
--      nombre de la entidad (ReglaTyposquatting siempre incluye uno de los dos).
-- Solo cuentan los analisis sospechosos o de phishing. UNION elimina el
-- doble conteo cuando un mismo analisis coincide por las dos fuentes.
CREATE OR REPLACE VIEW v_top_entidades_suplantadas AS
WITH coincidencias AS (
    SELECT a.id_analisis, a.id_empresa, dl.id_entidad
    FROM analisis a
    JOIN dominios_lista dl
      ON dl.dominio = a.dominio
     AND dl.lista = 'negra'
     AND dl.id_entidad IS NOT NULL
    WHERE a.resultado <> 'seguro'

    UNION

    SELECT a.id_analisis, a.id_empresa, e.id_entidad
    FROM analisis a
    JOIN analisis_indicadores ai ON ai.id_analisis = a.id_analisis
    JOIN reglas_deteccion r      ON r.id_regla = ai.id_regla
                                AND r.codigo = 'URL_TYPOSQUATTING'
    JOIN entidades_suplantadas e
      ON ai.valor LIKE '%' || e.dominio_oficial || '%'
      OR ai.valor LIKE '%' || e.nombre || '%'
    WHERE a.resultado <> 'seguro'
)
SELECT
    e.id_entidad,
    e.nombre                         AS entidad,
    e.tipo,
    c.id_empresa,
    COUNT(DISTINCT c.id_analisis)    AS veces_suplantada
FROM coincidencias c
JOIN entidades_suplantadas e ON e.id_entidad = c.id_entidad
GROUP BY e.id_entidad, e.nombre, e.tipo, c.id_empresa;


-- El panel y los listados ordenan por fecha sin filtrar por usuario ni
-- empresa; los indices existentes empiezan por esas columnas.
CREATE INDEX IF NOT EXISTS idx_analisis_fecha ON analisis (fecha_analisis DESC);
