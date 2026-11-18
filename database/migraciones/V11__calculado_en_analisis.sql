-- =====================================================================
-- PhishShield Bolivia - Migracion V11: columna analisis.calculado_en
-- =====================================================================
-- Que hace:
--   Agrega a analisis la columna calculado_en (TIMESTAMPTZ, admite NULL) y
--   la restriccion chk_analisis_calculado_en (calculado_en <= fecha_analisis).
--
-- Por que (hallazgo 12 de la auditoria del 16/11/2026):
--   Cuando otro usuario u otra empresa analiza una URL que ya esta en la
--   cache (menos de 24 h), el backend ya no le devuelve la fila de quien la
--   analizo primero: guarda una fila nueva a su nombre, con los mismos
--   indicadores y sin recalcular. Esa fila nueva lleva la fecha de hoy en
--   fecha_analisis (el historial y el panel filtran por esa fecha), y en
--   calculado_en la fecha del calculo que copio. La vigencia de 24 h se mide
--   desde calculado_en: si se midiera desde fecha_analisis, cada copia la
--   renovaria y una URL consultada a diario no se recalcularia nunca.
--
--   NULL significa "este analisis calculo sus propios indicadores", que es
--   el caso de todas las filas existentes.
--
-- Que NO hace:
--   No borra ni modifica filas: todas las existentes quedan con NULL.
--   No toca otras tablas ni las vistas del panel.
--
-- Cuando aplicarla:
--   En bases creadas antes de este cambio, despues de V2 a V10. Las bases
--   nuevas ya traen la columna desde initdb/01_schema.sql.
--   El backend la necesita: con ddl-auto=validate no arranca sin ella.
--   Es idempotente: ejecutarla de nuevo no cambia nada.
--
-- Para deshacerla (solo si se vuelve a la version anterior del backend):
--   ALTER TABLE analisis DROP CONSTRAINT IF EXISTS chk_analisis_calculado_en;
--   ALTER TABLE analisis DROP COLUMN IF EXISTS calculado_en;
--
-- Como aplicarla (desde la raiz del repositorio):
--   PowerShell:
--     Get-Content database\migraciones\V11__calculado_en_analisis.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction
--   Git Bash / Linux / macOS:
--     docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction < database/migraciones/V11__calculado_en_analisis.sql
--
-- Nota: este archivo se mantiene en ASCII a proposito (ver V2).
-- =====================================================================

ALTER TABLE analisis ADD COLUMN IF NOT EXISTS calculado_en TIMESTAMPTZ;

COMMENT ON COLUMN analisis.calculado_en IS
    'Solo en copias de la cache: cuando se calcularon los indicadores copiados. NULL si este analisis los calculo.';

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_analisis_calculado_en') THEN
        ALTER TABLE analisis ADD CONSTRAINT chk_analisis_calculado_en
            CHECK (calculado_en IS NULL OR calculado_en <= fecha_analisis);
    END IF;
END $$;

-- Comprobacion: la columna existe y ninguna fila previa quedo con valor
SELECT COUNT(*) AS filas, COUNT(calculado_en) AS copias_de_cache
FROM analisis;
