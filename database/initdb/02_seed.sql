-- =====================================================================
-- PhishShield Bolivia — Datos de ejemplo (desarrollo y demo)
-- No usar en producción: las contraseñas son de prueba.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Entidades bolivianas más suplantadas
-- ---------------------------------------------------------------------
INSERT INTO entidades_suplantadas (nombre, tipo, dominio_oficial) VALUES
    ('Banco Nacional de Bolivia',      'banco',     'bnb.com.bo'),
    ('Banco Unión',                    'banco',     'bancounion.com.bo'),
    ('Banco Mercantil Santa Cruz',     'banco',     'bmsc.com.bo'),
    ('Banco BISA',                     'banco',     'bisa.com'),
    ('Banco Ganadero',                 'banco',     'bg.com.bo'),
    ('Tigo Money',                     'billetera', 'tigo.com.bo'),
    ('Impuestos Nacionales',           'gobierno',  'impuestos.gob.bo'),
    ('Entel',                          'telco',     'entel.bo'),
    ('Segip',                          'gobierno',  'segip.gob.bo');

-- ---------------------------------------------------------------------
-- Listas de dominios
-- ---------------------------------------------------------------------
INSERT INTO dominios_lista (dominio, lista, id_entidad, fuente, motivo)
SELECT e.dominio_oficial, 'blanca', e.id_entidad, 'semilla', 'Dominio oficial verificado'
FROM entidades_suplantadas e
WHERE e.dominio_oficial IS NOT NULL;

INSERT INTO dominios_lista (dominio, lista, id_entidad, fuente, motivo) VALUES
    ('bnb-bo-seguro.com',        'negra',
        (SELECT id_entidad FROM entidades_suplantadas WHERE nombre = 'Banco Nacional de Bolivia'),
        'reporte_usuario', 'Clon de la banca en línea del BNB'),
    ('bancounion-verificacion.net', 'negra',
        (SELECT id_entidad FROM entidades_suplantadas WHERE nombre = 'Banco Unión'),
        'reporte_usuario', 'Formulario falso de verificación de cuenta'),
    ('impuestos-gob-bo.info',    'negra',
        (SELECT id_entidad FROM entidades_suplantadas WHERE nombre = 'Impuestos Nacionales'),
        'analista', 'Suplanta al SIN pidiendo datos de NIT'),
    ('premios-tigo-bo.xyz',      'gris', NULL, 'automatico', 'Dominio recién registrado, sin reputación');

-- ---------------------------------------------------------------------
-- Reglas heurísticas
-- ---------------------------------------------------------------------
INSERT INTO reglas_deteccion (codigo, descripcion, categoria, peso) VALUES
    ('URL_IP_LITERAL',      'La URL usa una dirección IP en lugar de un dominio',            'url',        18.00),
    ('URL_SIN_HTTPS',       'El sitio no utiliza HTTPS',                                     'url',         8.00),
    ('URL_TYPOSQUATTING',   'El dominio se parece a una entidad conocida (distancia ≤ 2)',   'url',        25.00),
    ('URL_TLD_SOSPECHOSO',  'Extensión de dominio asociada a abuso (.xyz, .top, .info)',     'url',        12.00),
    ('URL_DOMINIO_NUEVO',   'Dominio registrado hace menos de 30 días',                      'url',        15.00),
    ('URL_SUBDOMINIOS',     'Cantidad anómala de subdominios',                               'url',        10.00),
    ('TXT_URGENCIA',        'Lenguaje de urgencia o amenaza de bloqueo de cuenta',           'contenido',  14.00),
    ('TXT_PIDE_CREDENCIAL', 'Solicita contraseña, PIN o código de verificación',             'contenido',  30.00),
    ('TXT_PREMIO',          'Promete premios, bonos o sorteos',                              'contenido',  12.00),
    ('MAIL_SPF_FALLA',      'El remitente no pasa la validación SPF/DKIM',                   'remitente',  22.00),
    ('MAIL_DOMINIO_DISTINTO','El dominio del remitente no coincide con la marca declarada',  'remitente',  20.00),
    ('IMG_LOGO_ALTERADO',   'Logotipo institucional con diferencias respecto al oficial',    'visual',     16.00);

-- ---------------------------------------------------------------------
-- Modelos de IA registrados
-- ---------------------------------------------------------------------
INSERT INTO modelos_ia (nombre, version, tipo, algoritmo, exactitud, precision_val, recall_val, f1_val, umbral_phishing, activo, entrenado_en) VALUES
    ('phishshield-url',   '0.1.0', 'url',    'Random Forest',      88.40, 86.10, 84.70, 85.39, 70.00, FALSE, DATE '2026-10-05'),
    ('phishshield-url',   '0.2.0', 'url',    'XGBoost + features', 93.20, 92.00, 90.80, 91.40, 70.00, TRUE,  DATE '2026-10-20'),
    ('phishshield-texto', '0.1.0', 'texto',  'BERT multilingüe',   91.70, 90.30, 89.10, 89.70, 70.00, TRUE,  DATE '2026-10-22');

-- ---------------------------------------------------------------------
-- Empresa y usuarios de prueba
-- ---------------------------------------------------------------------
INSERT INTO empresas (nombre, nit, sector, ciudad, plan) VALUES
    ('Importadora Andina S.R.L.', '1023456789', 'Comercio',  'La Paz',      'profesional'),
    ('Clínica San Rafael',        '2098765432', 'Salud',     'Cochabamba',  'basico');

-- NOTA: password_hash de ejemplo (BCrypt de la palabra "password").
-- Regeneralo desde el backend antes de cualquier demo real.
INSERT INTO usuarios (id_empresa, nombres, apellidos, email, password_hash, rol, estado, email_verificado) VALUES
    (NULL,
     'Admin', 'PhishShield', 'admin@phishshield.bo',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin', 'activo', TRUE),
    ((SELECT id_empresa FROM empresas WHERE nit = '1023456789'),
     'María', 'Quispe', 'maria.quispe@andina.bo',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'empresa', 'activo', TRUE),
    ((SELECT id_empresa FROM empresas WHERE nit = '1023456789'),
     'Carlos', 'Mamani', 'carlos.mamani@andina.bo',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'usuario', 'activo', TRUE),
    ((SELECT id_empresa FROM empresas WHERE nit = '2098765432'),
     'Lucía', 'Ferrufino', 'lucia.ferrufino@sanrafael.bo',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'usuario', 'activo', FALSE);

-- ---------------------------------------------------------------------
-- Análisis de ejemplo (el hash SHA-256 se calcula solo)
-- ---------------------------------------------------------------------
INSERT INTO analisis (id_usuario, id_empresa, id_modelo, tipo, origen, contenido, contenido_hash, dominio, resultado, nivel_riesgo, tiempo_ms)
VALUES
    ((SELECT id_usuario FROM usuarios WHERE email = 'carlos.mamani@andina.bo'),
     (SELECT id_empresa FROM empresas WHERE nit = '1023456789'),
     (SELECT id_modelo  FROM modelos_ia WHERE nombre = 'phishshield-url' AND version = '0.2.0'),
     'url', 'extension',
     'http://bnb-bo-seguro.com/login/verificar-cuenta',
     encode(sha256('http://bnb-bo-seguro.com/login/verificar-cuenta'::bytea), 'hex'),
     'bnb-bo-seguro.com', 'phishing', 94.50, 180),

    ((SELECT id_usuario FROM usuarios WHERE email = 'maria.quispe@andina.bo'),
     (SELECT id_empresa FROM empresas WHERE nit = '1023456789'),
     (SELECT id_modelo  FROM modelos_ia WHERE nombre = 'phishshield-texto' AND version = '0.1.0'),
     'correo', 'plugin_correo',
     'Estimado cliente: su cuenta será bloqueada en 24 horas. Confirme su contraseña aquí.',
     encode(sha256('Estimado cliente: su cuenta será bloqueada en 24 horas. Confirme su contraseña aquí.'::bytea), 'hex'),
     'bancounion-verificacion.net', 'phishing', 88.00, 260),

    ((SELECT id_usuario FROM usuarios WHERE email = 'lucia.ferrufino@sanrafael.bo'),
     (SELECT id_empresa FROM empresas WHERE nit = '2098765432'),
     (SELECT id_modelo  FROM modelos_ia WHERE nombre = 'phishshield-url' AND version = '0.2.0'),
     'url', 'extension',
     'https://premios-tigo-bo.xyz/sorteo',
     encode(sha256('https://premios-tigo-bo.xyz/sorteo'::bytea), 'hex'),
     'premios-tigo-bo.xyz', 'sospechoso', 61.25, 140),

    ((SELECT id_usuario FROM usuarios WHERE email = 'carlos.mamani@andina.bo'),
     (SELECT id_empresa FROM empresas WHERE nit = '1023456789'),
     (SELECT id_modelo  FROM modelos_ia WHERE nombre = 'phishshield-url' AND version = '0.2.0'),
     'url', 'web',
     'https://www.bnb.com.bo/',
     encode(sha256('https://www.bnb.com.bo/'::bytea), 'hex'),
     'bnb.com.bo', 'seguro', 4.00, 95);

-- Indicadores que dispararon cada análisis
INSERT INTO analisis_indicadores (id_analisis, id_regla, valor, aporte)
SELECT a.id_analisis, r.id_regla, v.valor, v.aporte
FROM (VALUES
        ('bnb-bo-seguro.com',          'URL_TYPOSQUATTING',   'bnb-bo-seguro.com ≈ bnb.com.bo', 25.00),
        ('bnb-bo-seguro.com',          'URL_SIN_HTTPS',       'esquema http',                    8.00),
        ('bnb-bo-seguro.com',          'URL_DOMINIO_NUEVO',   'registrado hace 6 días',         15.00),
        ('bancounion-verificacion.net','TXT_URGENCIA',        '24 horas / bloqueada',           14.00),
        ('bancounion-verificacion.net','TXT_PIDE_CREDENCIAL', 'solicita contraseña',            30.00),
        ('premios-tigo-bo.xyz',        'URL_TLD_SOSPECHOSO',  '.xyz',                           12.00),
        ('premios-tigo-bo.xyz',        'TXT_PREMIO',          'sorteo',                         12.00)
     ) AS v(dominio, codigo, valor, aporte)
JOIN analisis          a ON a.dominio = v.dominio
JOIN reglas_deteccion  r ON r.codigo  = v.codigo;

-- Alertas generadas por los análisis de riesgo alto
INSERT INTO alertas (id_analisis, severidad, estado, canal)
SELECT id_analisis,
       CASE WHEN nivel_riesgo >= 90 THEN 'critica'
            WHEN nivel_riesgo >= 70 THEN 'alta'
            ELSE 'media' END,
       'pendiente',
       CASE WHEN origen = 'plugin_correo' THEN 'email' ELSE 'extension' END
FROM analisis
WHERE resultado IN ('phishing', 'sospechoso');

-- Feedback de un usuario que cree que hubo un falso positivo
INSERT INTO feedback_analisis (id_analisis, id_usuario, veredicto, comentario)
SELECT a.id_analisis,
       (SELECT id_usuario FROM usuarios WHERE email = 'lucia.ferrufino@sanrafael.bo'),
       'falso_positivo',
       'Es una promoción real que me llegó por SMS de la empresa.'
FROM analisis a
WHERE a.dominio = 'premios-tigo-bo.xyz';

-- ---------------------------------------------------------------------
-- Módulo educativo
-- ---------------------------------------------------------------------
INSERT INTO lecciones (titulo, descripcion, nivel, duracion_min, orden) VALUES
    ('¿Qué es el phishing?',                    'Conceptos básicos y ejemplos del contexto boliviano.', 'basico',      10, 1),
    ('Cómo revisar una URL antes de hacer clic','Anatomía de una URL y señales de typosquatting.',      'basico',      12, 2),
    ('Correos que suplantan a tu banco',        'Casos reales de suplantación de bancos bolivianos.',   'intermedio',  15, 3),
    ('Robo de cuentas de WhatsApp',             'Ingeniería social y verificación en dos pasos.',       'intermedio',  12, 4),
    ('Respuesta ante un incidente',             'Qué hacer en las primeras horas tras caer en phishing.','avanzado',   20, 5);

INSERT INTO progreso_lecciones (id_usuario, id_leccion, estado, puntaje, completado_en)
SELECT u.id_usuario, l.id_leccion, 'completada', 90, NOW() - INTERVAL '3 days'
FROM usuarios u, lecciones l
WHERE u.email = 'carlos.mamani@andina.bo' AND l.orden IN (1, 2);

INSERT INTO progreso_lecciones (id_usuario, id_leccion, estado)
SELECT u.id_usuario, l.id_leccion, 'en_curso'
FROM usuarios u, lecciones l
WHERE u.email = 'carlos.mamani@andina.bo' AND l.orden = 3;

-- ---------------------------------------------------------------------
-- Reporte agregado y auditoría
-- ---------------------------------------------------------------------
INSERT INTO reportes (id_empresa, periodo_inicio, periodo_fin, total_analisis, total_phishing, total_alertas, generado_por)
SELECT e.id_empresa,
       DATE '2026-10-01', DATE '2026-10-31',
       COUNT(a.id_analisis),
       COUNT(*) FILTER (WHERE a.resultado = 'phishing'),
       COUNT(al.id_alerta),
       (SELECT id_usuario FROM usuarios WHERE email = 'maria.quispe@andina.bo')
FROM empresas e
LEFT JOIN analisis a ON a.id_empresa = e.id_empresa
LEFT JOIN alertas  al ON al.id_analisis = a.id_analisis
WHERE e.nit = '1023456789'
GROUP BY e.id_empresa;

INSERT INTO auditoria (id_usuario, accion, entidad, id_entidad_afectada, detalle, ip_origen)
SELECT id_usuario, 'LOGIN_EXITOSO', 'usuarios', id_usuario,
       jsonb_build_object('navegador', 'Chrome 141', 'origen', 'extension'),
       '190.129.10.25'::inet
FROM usuarios WHERE email = 'admin@phishshield.bo';
