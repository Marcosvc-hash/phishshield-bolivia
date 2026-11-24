-- =====================================================================
-- PhishShield Bolivia - Migracion V13: ruta real del catalogo QA en TC-REG-008
-- =====================================================================
-- Que hace:
--   Corrige los datos de prueba del caso TC-REG-008 en qa_casos: la ruta del
--   catalogo del Centro de Pruebas es GET /api/admin/qa/casos (no
--   /api/admin/qa/catalogo, como decia V12).
--
-- Por que:
--   El caso programado consulta /api/admin/qa/casos; el reporte debe mostrar
--   la misma ruta que de verdad se prueba.
--
-- Que NO hace:
--   No crea ni borra filas ni tablas. Solo cambia un texto de un caso.
--
-- Cuando aplicarla:
--   Despues de V12. Las bases nuevas ya traen el texto correcto desde
--   initdb/04_qa.sql. Es idempotente: el WHERE solo actualiza si el texto
--   todavia es el viejo.
-- =====================================================================

UPDATE qa_casos
SET datos_prueba = 'GET /api/empresas, GET /api/admin/panel y GET /api/admin/qa/casos sin el encabezado Authorization.'
WHERE codigo = 'TC-REG-008'
  AND datos_prueba LIKE '%/api/admin/qa/catalogo%';

SELECT codigo, datos_prueba FROM qa_casos WHERE codigo = 'TC-REG-008';
