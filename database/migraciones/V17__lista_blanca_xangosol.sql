-- =====================================================================
-- PhishShield Bolivia - Migracion V17: xangosol.com en la lista blanca (DEF-004)
-- =====================================================================
-- Que hace:
--   1. Agrega xangosol.com a la lista blanca (sin entidad: no es una
--      entidad boliviana suplantada).
--   2. Si el paso 1 inserto la fila, registra en auditoria un evento
--      DOMINIO_AGREGADO (sin usuario: lo hizo la migracion), igual que el
--      panel, para que la cache de analisis de 24 h no siga devolviendo
--      resultados calculados antes de este cambio.
--
-- Por que:
--   Defecto DEF-004. Desde V16 (BancoSol), URL_TYPOSQUATTING marcaba
--   www.xangosol.com con 45 puntos ("sospechoso"): "xangosol" esta a
--   distancia 2 de la marca "bancosol". Es un sitio legitimo segun
--   PhiUSIIL (label 1). Se detecto el 2026-11-24 al medir la regresion de
--   V16 con el motor real sobre 235 448 URL; fue el unico efecto colateral.
--   Se corrige como visa.com: la lista blanca arregla un falso positivo
--   puntual sin aflojar la tolerancia de la regla para todos.
--
-- Que NO hace:
--   No borra filas ni cambia el esquema.
--
-- Cuando aplicarla:
--   Despues de V16. Las bases nuevas ya traen la fila desde
--   initdb/02_seed.sql. Es idempotente: ON CONFLICT DO NOTHING, y la fila
--   de auditoria solo se escribe si se inserto el dominio.
--
-- Como aplicarla (desde la raiz del repositorio):
--   PowerShell:
--     Get-Content database\migraciones\V17__lista_blanca_xangosol.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction
--   Git Bash / Linux / macOS:
--     docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction < database/migraciones/V17__lista_blanca_xangosol.sql
--
-- Nota: este archivo se mantiene en ASCII a proposito (ver V2).
-- =====================================================================

WITH nuevo AS (
    INSERT INTO dominios_lista (dominio, lista, id_entidad, fuente, motivo)
    VALUES ('xangosol.com', 'blanca', NULL, 'manual',
            'Sitio legitimo (PhiUSIIL); se parece a bancosol.com.bo (BancoSol). DEF-004')
    ON CONFLICT (dominio, lista) DO NOTHING
    RETURNING id_dominio, dominio, lista, motivo
)
INSERT INTO auditoria (id_usuario, accion, entidad, id_entidad_afectada, detalle)
SELECT NULL, 'DOMINIO_AGREGADO', 'dominios_lista', id_dominio,
       jsonb_build_object('dominio', dominio, 'lista', lista, 'motivo', motivo,
                          'entidad', NULL, 'origen', 'migracion V17 (DEF-004)')
FROM nuevo;

-- Comprobacion: xangosol.com activo en la lista blanca
SELECT dominio, lista, activo, fuente
FROM dominios_lista
WHERE dominio = 'xangosol.com';
