-- =====================================================================
-- PhishShield Bolivia - Migracion V15: caso QA del modulo de reportes
-- =====================================================================
-- Que hace:
--   1. Agrega al catalogo del Centro de Pruebas el caso TC-REG-009
--      (generar el reporte de una empresa por API).
--   2. Actualiza el objetivo de TC-E2E-001, que decia que el sistema no
--      tenia modulo de reportes (ahora lo tiene: HU-17).
--
-- Que NO hace:
--   No borra filas ni toca ejecuciones ya guardadas.
--
-- Cuando aplicarla:
--   Despues de V14. Las bases nuevas ya traen lo mismo desde initdb/04_qa.sql.
--   Es idempotente: ON CONFLICT DO NOTHING y un UPDATE con WHERE.
--
-- Textos con tildes: van como U&'...' para que el archivo quede en ASCII.
-- =====================================================================

INSERT INTO qa_casos (codigo, tipo, suite, herramienta, orden, nombre, objetivo, precondiciones, datos_prueba, resultado_esperado) VALUES
('TC-REG-009', 'funcional', 'regression', 'api', 9,
 'Generar el reporte de una empresa por API',
 U&'Comprobar que el administrador genera el reporte de una empresa para un per\00EDodo, que los totales son coherentes y que el reporte se puede listar y descargar en PDF.',
 U&'Backend levantado en QA_BASE_URL. Sesi\00F3n iniciada con el administrador de prueba. Empresa de ejemplo Importadora Andina S.R.L. activa.',
 U&'POST /api/admin/reportes para Importadora Andina S.R.L. con el per\00EDodo de los \00FAltimos 365 d\00EDas; se genera dos veces el mismo per\00EDodo.',
 U&'HTTP 201 con los totales (an\00E1lisis, phishing y alertas) y phishing menor o igual a an\00E1lisis; la segunda vez devuelve el mismo id; el reporte aparece en GET /api/admin/reportes y GET /api/admin/reportes/{id}/pdf responde application/pdf.')
ON CONFLICT (codigo) DO NOTHING;

UPDATE qa_casos
SET objetivo = U&'Recorrer como un usuario real el camino completo de una detecci\00F3n: login, an\00E1lisis, historial y alerta.'
WHERE codigo = 'TC-E2E-001'
  AND objetivo LIKE '%no tiene m%dulo de reportes%';

SELECT codigo, nombre FROM qa_casos WHERE codigo IN ('TC-REG-009', 'TC-E2E-001') ORDER BY codigo;
