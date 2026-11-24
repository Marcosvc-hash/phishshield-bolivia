# PhishShield Bolivia — Base de datos local

PostgreSQL 18 + pgAdmin levantados con Docker Compose. El esquema y los datos de
ejemplo se cargan solos la primera vez que arranca el contenedor.

---

## 1. Requisito único

**Docker Desktop** instalado (Windows, macOS o Linux). Nada más: no hace falta
instalar PostgreSQL en la máquina.

Verificá que funcione:

```bash
docker --version
docker compose version
```

---

## 2. Levantar la base de datos

```bash
cd database
cp .env.example .env      # en Windows PowerShell: copy .env.example .env
# editá .env y poné tus propias contraseñas
docker compose up -d
```

Eso es todo. En la primera ejecución Docker descarga la imagen, crea la base
`phishshield` y ejecuta en orden `initdb/01_schema.sql`, `02_seed.sql`,
`03_vistas_panel_admin.sql` y `04_qa.sql` (Centro de Pruebas QA).

Comprobá que arrancó bien:

```bash
docker compose ps
docker compose logs -f db     # Ctrl+C para salir
```

Buscá la línea `database system is ready to accept connections`.

---

## 3. Datos de conexión

| Parámetro | Valor |
|---|---|
| Host | `localhost` |
| Puerto | `5432` |
| Base | `phishshield` |
| Usuario | `phishshield` |
| Contraseña | la que pusiste en `.env` |

Cadena JDBC: `jdbc:postgresql://localhost:5432/phishshield`

Desde **otro contenedor** (por ejemplo el backend dockerizado) el host no es
`localhost` sino el nombre del servicio: `db`.

---

## 4. Conectarse

### Opción A — psql dentro del contenedor (rápido, sin instalar nada)

```bash
docker exec -it phishshield-db psql -U phishshield -d phishshield
```

Comandos útiles dentro de psql:

```
\dt              -- listar tablas
\d analisis      -- ver estructura de una tabla
\dv              -- listar vistas
\di              -- listar índices
\q               -- salir
```

### Opción B — pgAdmin en el navegador

1. Abrí <http://localhost:5050>
2. Entrá con el email y contraseña de `.env`
3. *Add New Server* → pestaña **General**: nombre `PhishShield`
4. Pestaña **Connection**: Host `db` (no `localhost`, porque pgAdmin corre dentro de Docker),
   puerto `5432`, base `phishshield`, usuario y contraseña de `.env`

### Opción C — DBeaver o DataGrip

Nueva conexión PostgreSQL con los datos de la tabla de arriba. DBeaver es
gratuito; DataGrip es gratis con la licencia de estudiante de JetBrains.

---

## 5. Consultas de prueba

```sql
-- Vistas del dashboard
SELECT * FROM v_resumen_empresa;
SELECT * FROM v_alertas_abiertas;
SELECT * FROM v_top_dominios_maliciosos;

-- Por qué un análisis fue marcado como phishing
SELECT a.contenido, r.codigo, i.valor, i.aporte
FROM analisis a
JOIN analisis_indicadores i ON i.id_analisis = a.id_analisis
JOIN reglas_deteccion     r ON r.id_regla    = i.id_regla
WHERE a.dominio = 'bnb-bo-seguro.com';

-- Detecciones por día
SELECT DATE(fecha_analisis) AS dia, resultado, COUNT(*)
FROM analisis
GROUP BY 1, 2
ORDER BY 1 DESC;
```

Probá también que las restricciones funcionan (esto **debe** fallar):

```sql
INSERT INTO analisis (id_usuario, tipo, contenido, contenido_hash, resultado, nivel_riesgo)
VALUES (1, 'url', 'http://test.com', repeat('a', 64), 'seguro', 95);
-- ERROR: viola la restricción chk_analisis_coherencia
```

---

## 6. Operaciones frecuentes

```bash
docker compose stop            # apagar sin borrar nada
docker compose start           # volver a encender
docker compose down            # borrar contenedores (los datos sobreviven)
docker compose down -v         # BORRAR TAMBIÉN LOS DATOS y volver a cero
```

**Importante:** los scripts de `initdb/` se ejecutan **solo cuando el volumen
está vacío**. Si modificás el esquema y querés recargarlo desde cero:

```bash
docker compose down -v && docker compose up -d
```

### Migraciones

Los scripts de `initdb/` solo corren con el volumen vacío, así que un cambio en
`02_seed.sql` **no** llega a una base que ya existe. Para eso está la carpeta
`migraciones/`: cada archivo `V<n>__descripcion.sql` aplica un cambio puntual
sobre una base existente, **sin borrar datos**, y es idempotente (se puede
ejecutar más de una vez). Todo cambio se escribe en los dos lugares: en
`initdb/` para las bases nuevas y en `migraciones/` para las existentes.

| Migración | Qué hace |
|---|---|
| `V2__regla_lista_negra.sql` | Registra la regla `URL_LISTA_NEGRA` (peso 70) |
| `V3__ajustar_peso_typosquatting.sql` | Sube el peso de `URL_TYPOSQUATTING` de 25 a 45 |
| `V4__ajuste_reglas_y_lista_blanca.sql` | `URL_IP_LITERAL` a 30, registra `URL_RUTA_SENSIBLE` (peso 10) y agrega `visa.com` a la lista blanca |
| `V5__modelo_ia_url.sql` | Registra la regla `IA_MODELO_URL` (peso 40) y el modelo real `phishshield-url-ia` 1.0.0 con sus métricas de prueba, como único modelo de URL activo |
| `V6__lista_blanca_ende.sql` | Agrega `ende.bo` a la lista blanca (typosquatting lo confundía con `entel.bo`) |
| `V7__entidad_ende.sql` | Agrega Ende a `entidades_suplantadas` (dominio oficial `ende.bo`) y vincula su fila de la lista blanca |
| `V8__vistas_panel_admin.sql` | Crea las vistas `v_analisis_por_dia` y `v_top_entidades_suplantadas` y el índice `idx_analisis_fecha`, para el panel de administración. No toca tablas ni datos. En bases nuevas lo hace `initdb/03_vistas_panel_admin.sql` |
| `V9__uuid_alertas_y_dominios.sql` | Agrega `uuid_publico` (con índice único) a `alertas` y `dominios_lista`, para que la gestión de alertas y de listas no exponga el BIGINT interno. Cada fila existente recibe un uuid distinto; no borra datos. **El módulo de administración la necesita**: sin ella, las páginas de alertas y de listas de dominios responden error |
| `V10__desactivar_modelos_de_ejemplo.sql` | Pone `activo = FALSE` en las filas de ejemplo de `modelos_ia` (`phishshield-url` y `phishshield-texto`), que no tienen un modelo real detrás. El único modelo activo queda `phishshield-url-ia` 1.0.0. No borra filas ni cambia métricas |
| `V11__calculado_en_analisis.sql` | Agrega a `analisis` la columna `calculado_en` (admite NULL) y la restricción `chk_analisis_calculado_en`. Cuando otro usuario u otra empresa analiza una URL que está en la caché, se guarda una fila a su nombre con los indicadores copiados; `calculado_en` guarda cuándo se calcularon, para que la copia no renueve la vigencia de 24 h. Las filas existentes quedan en NULL; no borra datos. **El backend la necesita**: sin ella no arranca |
| `V12__modulo_qa.sql` | Crea las 6 tablas del Centro de Pruebas QA (`qa_casos`, `qa_ejecuciones`, `qa_resultados`, `qa_pasos`, `qa_metricas`, `qa_lote_robustez`) y carga el catálogo de 27 casos y el lote de 80 URL etiquetadas. No toca tablas ni filas existentes. Mismo contenido que `initdb/04_qa.sql`; los textos con tildes van como `U&'...'` para que el archivo quede en ASCII. **El backend con el módulo QA la necesita** |
| `V13__ruta_catalogo_qa.sql` | Corrige los datos de prueba de `TC-REG-008` en `qa_casos`: la ruta del catálogo es `GET /api/admin/qa/casos`. Solo cambia ese texto; no crea ni borra filas. En bases nuevas ya viene corregido en `initdb/04_qa.sql` |
| `V14__uuid_reportes.sql` | Agrega `uuid_publico` (con índice único) a `reportes`, para que la HU-17 (reportes por empresa) no exponga el BIGINT interno. Cada fila existente recibe un uuid distinto; no borra datos. **El módulo de reportes la necesita**. En bases nuevas ya viene en `initdb/01_schema.sql` |
| `V15__caso_qa_reportes.sql` | Agrega al catálogo QA el caso `TC-REG-009` (reporte de una empresa por API) y actualiza el objetivo de `TC-E2E-001`. No borra filas. En bases nuevas ya viene en `initdb/04_qa.sql` |
| `V16__entidad_bancosol.sql` | Corrige **DEF-001**: agrega BancoSol a `entidades_suplantadas` (dominio oficial `bancosol.com.bo`, tipo `banco`) y su dominio a la lista blanca, y registra un `DOMINIO_AGREGADO` en `auditoria` para que la caché de análisis no devuelva resultados anteriores. Así `URL_TYPOSQUATTING` detecta `bancosoI.com.bo`. No borra filas. En bases nuevas ya viene en `initdb/02_seed.sql` |

Aplicarlas en orden, desde la raíz del repositorio:

```powershell
# PowerShell (no admite "<")
Get-Content database\migraciones\V2__regla_lista_negra.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
Get-Content database\migraciones\V3__ajustar_peso_typosquatting.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
Get-Content database\migraciones\V4__ajuste_reglas_y_lista_blanca.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
Get-Content database\migraciones\V5__modelo_ia_url.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
Get-Content database\migraciones\V6__lista_blanca_ende.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
Get-Content database\migraciones\V7__entidad_ende.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
Get-Content database\migraciones\V8__vistas_panel_admin.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction
Get-Content database\migraciones\V9__uuid_alertas_y_dominios.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction
Get-Content database\migraciones\V10__desactivar_modelos_de_ejemplo.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction
Get-Content database\migraciones\V11__calculado_en_analisis.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction
Get-Content database\migraciones\V12__modulo_qa.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction
Get-Content database\migraciones\V13__ruta_catalogo_qa.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction
Get-Content database\migraciones\V14__uuid_reportes.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction
Get-Content database\migraciones\V15__caso_qa_reportes.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction
Get-Content database\migraciones\V16__entidad_bancosol.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction
```

```bash
# Git Bash / Linux / macOS
docker exec -i phishshield-db psql -U phishshield -d phishshield < database/migraciones/V2__regla_lista_negra.sql
docker exec -i phishshield-db psql -U phishshield -d phishshield < database/migraciones/V3__ajustar_peso_typosquatting.sql
docker exec -i phishshield-db psql -U phishshield -d phishshield < database/migraciones/V4__ajuste_reglas_y_lista_blanca.sql
docker exec -i phishshield-db psql -U phishshield -d phishshield < database/migraciones/V5__modelo_ia_url.sql
docker exec -i phishshield-db psql -U phishshield -d phishshield < database/migraciones/V6__lista_blanca_ende.sql
docker exec -i phishshield-db psql -U phishshield -d phishshield < database/migraciones/V7__entidad_ende.sql
docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction < database/migraciones/V8__vistas_panel_admin.sql
docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction < database/migraciones/V9__uuid_alertas_y_dominios.sql
docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction < database/migraciones/V10__desactivar_modelos_de_ejemplo.sql
docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction < database/migraciones/V11__calculado_en_analisis.sql
docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction < database/migraciones/V12__modulo_qa.sql
docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction < database/migraciones/V13__ruta_catalogo_qa.sql
docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction < database/migraciones/V14__uuid_reportes.sql
docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction < database/migraciones/V15__caso_qa_reportes.sql
docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction < database/migraciones/V16__entidad_bancosol.sql
```

### Base de pruebas `phishshield_qa`

El Centro de Pruebas QA ejecuta pruebas reales contra PhishShield: cada ejecución
crea análisis, alertas, empresas y filas de auditoría, y las pruebas de carga pueden
crear miles. Para no ensuciar la base real, la demo usa otra base, `phishshield_qa`,
en el mismo contenedor. Tiene la misma configuración de detección que la real
(reglas, listas, entidades y modelos), y el admin entra con la misma contraseña que
en la base real.

Para levantar el backend contra ella: `backend/herramientas/levantar-backend-qa.ps1`.

Para recrearla desde cero (Git Bash, desde la raíz del repositorio; **solo afecta a
`phishshield_qa`**):

```bash
export MSYS_NO_PATHCONV=1
docker exec phishshield-db dropdb -U phishshield --if-exists phishshield_qa
docker exec phishshield-db createdb -U phishshield phishshield_qa
for f in database/initdb/0*.sql $(ls database/migraciones/V*.sql | sort -V); do
  docker exec -i phishshield-db psql -U phishshield -d phishshield_qa -q -v ON_ERROR_STOP=1 --single-transaction < "$f" || break
done
# Copia la contraseña del admin de la base real (sin mostrar el hash)
docker exec phishshield-db psql -U phishshield -d phishshield -Atc "SELECT format('UPDATE usuarios SET password_hash = %L WHERE email = %L;', password_hash, email) FROM usuarios WHERE email = 'admin@phishshield.bo'" | docker exec -i phishshield-db psql -U phishshield -d phishshield_qa -q
```

Los casos del Centro de Pruebas inician sesión con un administrador propio,
`qa.admin@phishshield.bo`, que existe **solo en `phishshield_qa`**. Para crearlo (o
cambiarle la contraseña) y dejar sus datos en `backend/.env`:
`backend/herramientas/crear-usuario-qa.ps1`. La contraseña es aleatoria y no se muestra.

Si una migración falta, el backend lo avisa al arrancar con un `WARN` del tipo
"La regla URL_... existe en Java pero no en reglas_deteccion".

Los archivos de migración se escriben solo en ASCII (sin tildes ni eñes): Windows
PowerShell 5.1 cambia esos caracteres por `?` al pasarlos por la tubería.

### Sobre los datos de ejemplo

`02_seed.sql` tiene datos **ilustrativos** para desarrollo y demo:

- Las filas `phishshield-url` y `phishshield-texto` de `modelos_ia` tienen métricas y
  fechas inventadas, y quedan inactivas (V10). La única fila real, y la única activa,
  es `phishshield-url-ia` 1.0.0 (ver `ia/`).
- Los análisis de ejemplo (`analisis` y `analisis_indicadores`) tienen puntajes
  escritos a mano, no calculados por el motor de reglas. El backend solo
  reutiliza un análisis durante 24 horas, así que estas filas no se devuelven
  como resultado de un análisis nuevo.

### Respaldo y restauración

```bash
docker exec phishshield-db pg_dump -U phishshield phishshield > backup.sql
cat backup.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
```

---

## 7. Estructura del esquema (15 tablas)

| Bloque | Tablas |
|---|---|
| Organización y acceso | `empresas`, `usuarios`, `sesiones` |
| Inteligencia de amenazas | `entidades_suplantadas`, `dominios_lista` |
| Motor de detección | `modelos_ia`, `reglas_deteccion` |
| Operación | `analisis`, `analisis_indicadores`, `alertas`, `feedback_analisis` |
| Educación | `lecciones`, `progreso_lecciones` |
| Dashboard y trazabilidad | `reportes`, `auditoria` |
| Centro de Pruebas QA (`04_qa.sql` / V12) | `qa_casos`, `qa_ejecuciones`, `qa_resultados`, `qa_pasos`, `qa_metricas`, `qa_lote_robustez` |

Relaciones principales:

```
empresas ──< usuarios ──< sesiones
    │           │
    │           └──< analisis >── modelos_ia
    │                  │  │
    │                  │  └──< analisis_indicadores >── reglas_deteccion
    │                  ├──── alertas (1:1)
    │                  └──< feedback_analisis
    └──< reportes

entidades_suplantadas ──< dominios_lista
usuarios ──< progreso_lecciones >── lecciones
usuarios ──< auditoria
```

Elementos que van más allá de un CREATE TABLE simple y conviene mencionar en la
defensa del proyecto:

- Claves subrogadas `BIGINT IDENTITY` + `uuid_publico` para exponer en la API.
- `TIMESTAMPTZ` en vez de `TIMESTAMP` (zona horaria explícita, `America/La_Paz`).
- Restricciones `CHECK` que codifican reglas de negocio, incluida la coherencia
  entre `resultado` y `nivel_riesgo`.
- Índices parciales (`WHERE activo`, `WHERE estado IN (...)`) y un índice GIN
  sobre `JSONB` en `auditoria`.
- Índice único parcial que garantiza un solo modelo de IA activo por tipo.
- Trigger `fn_set_actualizado_en()` para auditoría de cambios.
- Tres vistas que alimentan el dashboard sin duplicar lógica en el backend.

---

## 8. Conectar el backend

### Spring Boot — `application.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/phishshield
    username: phishshield
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate   # el esquema lo manda el SQL, no Hibernate
    properties:
      hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect
```

Dependencias: `spring-boot-starter-data-jpa`, `postgresql`, y para versionar
cambios de esquema, `flyway-core` + `flyway-database-postgresql`.

### Django — `settings.py`

```python
DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.postgresql",
        "NAME": "phishshield",
        "USER": "phishshield",
        "PASSWORD": os.environ["DB_PASSWORD"],
        "HOST": "localhost",
        "PORT": "5432",
    }
}
```

Para generar los modelos a partir de este esquema:
`python manage.py inspectdb > models.py`

---

## 9. Problemas comunes

| Síntoma | Causa y solución |
|---|---|
| `port 5432 already in use` | Tenés PostgreSQL instalado nativo. Apagá ese servicio o cambiá el puerto a `"5433:5432"` en `docker-compose.yml`. |
| Cambié el `.sql` y no se aplica | El volumen ya tiene datos. `docker compose down -v && docker compose up -d`. |
| pgAdmin no conecta a `localhost` | Desde pgAdmin el host es `db`, el nombre del servicio en la red de Docker. |
| `password authentication failed` | El volumen conserva la contraseña de la primera creación. Recreá con `down -v`. |
| Acentos raros en la consola de Windows | Ejecutá `chcp 65001` antes de entrar a psql. |
