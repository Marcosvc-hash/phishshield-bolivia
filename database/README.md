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
cd phishshield-db
cp .env.example .env      # en Windows PowerShell: copy .env.example .env
# editá .env y poné tus propias contraseñas
docker compose up -d
```

Eso es todo. En la primera ejecución Docker descarga la imagen, crea la base
`phishshield` y ejecuta en orden `initdb/01_schema.sql` y `initdb/02_seed.sql`.

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

### Respaldo y restauración

```bash
docker exec phishshield-db pg_dump -U phishshield phishshield > backup.sql
cat backup.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
```

---

## 7. Estructura del esquema (14 tablas)

| Bloque | Tablas |
|---|---|
| Organización y acceso | `empresas`, `usuarios`, `sesiones` |
| Inteligencia de amenazas | `entidades_suplantadas`, `dominios_lista` |
| Motor de detección | `modelos_ia`, `reglas_deteccion` |
| Operación | `analisis`, `analisis_indicadores`, `alertas`, `feedback_analisis` |
| Educación | `lecciones`, `progreso_lecciones` |
| Dashboard y trazabilidad | `reportes`, `auditoria` |

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
