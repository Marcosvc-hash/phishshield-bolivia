# PhishShield Bolivia — notas para Claude

Proyecto universitario de detección de phishing. El usuario es estudiante, viene de C# básico
y no tiene experiencia previa en Java: respóndele en español sencillo.

## Reglas de trabajo (permanentes)

1. **Pedir aprobación solo antes de:** borrar datos, cambiar el esquema de la base, agregar o
   actualizar dependencias, o cambiar algo fuera de lo acordado. Lo demás se hace de corrido,
   con un resumen al final de cada commit. Si aparece un problema fuera de lo acordado, se
   reporta y se propone la solución.
2. **Equivalencias con C#:** cuando aparezca un concepto nuevo de Java o Spring, da su
   equivalente en C#/.NET en una línea (por ejemplo: `@Bean` ≈ registrar un servicio con
   `builder.Services.AddSingleton(...)`; `Optional<T>` ≈ un `T?` que obliga a revisar si viene vacío).
3. **Ramas y commits:** trabajar en ramas `feature/nombre-corto`; `main` siempre debe levantar.
   Commits en español y en imperativo.
4. **Dominio limpio:** ningún archivo dentro de un paquete `domain/` puede importar
   `org.springframework` ni `jakarta.persistence` (el docente lo verifica). Comprobarlo con
   `git grep -nE "import (org\.springframework|jakarta\.persistence)" -- "backend/src/main/java/**/domain/**"`,
   que no debe devolver nada.
5. **Código para revisar:** mostrar el archivo o el método completo, nunca fragmentos sueltos.
6. **Secretos:** nunca subir contraseñas ni claves; van en `.env` (ignorado por git) o en
   variables de entorno.
7. **Pruebas en vivo sin ensuciar la base real:** las mediciones y pruebas en vivo se hacen en
   una base temporal (`CREATE DATABASE prueba_…` dentro del contenedor, con `01_schema.sql` +
   `02_seed.sql` y las migraciones; se borra al terminar). Si es inevitable usar la base real:
   - marcar todas las URL de la prueba con un parámetro propio y único, por ejemplo
     `?m=<prueba>-<fecha>`, para poder encontrarlas después;
   - al terminar, proponer un script de limpieza que filtre solo por esa marca, dentro de una
     transacción y mostrando los conteos antes del `COMMIT`. No ejecutarlo sin aprobación.
   Recordar que cada análisis no seguro crea una alerta (`alertas`), y que eso afecta a los
   reportes y al dashboard.

## Estructura

- `backend/`: Spring Boot 4.1 + Java 25, Maven Wrapper (`mvnw.cmd`).
- `frontend/`: Angular 22 + TypeScript 6, pruebas con Vitest.
- `database/`: PostgreSQL 18 en Docker (contenedor `phishshield-db`).
  - `initdb/01_schema.sql` (15 tablas), `initdb/02_seed.sql` y `initdb/03_vistas_panel_admin.sql`
    (vistas del panel, igual que V8): solo corren con el volumen vacío.
  - `migraciones/V<n>__*.sql`: cambios para bases que ya existen.

## Arquitectura hexagonal del backend

Paquete `bo.phishshield.backend`, organizado por verticales: `analisis`, `empresa`, `usuario`, `panel`,
`historial`, `alerta`, `listas`, `auditoria` y `shared` (ver `docs/guia-modulo-admin.md`).
Cada vertical se divide así:

| Capa | Qué va ahí | Reglas |
|---|---|---|
| `domain/model` | Entidades, records, enums y excepciones de negocio | Sin Spring ni JPA (regla 4) |
| `domain/service` | Lógica pura, como las reglas `Regla*` del motor | Sin Spring ni JPA (regla 4) |
| `domain/port/out` | Interfaces de salida (`*RepositoryPort`) | Sin Spring ni JPA (regla 4) |
| `application` | Casos de uso (`*UseCase`) y comandos (`Comando*`) | **Sin anotaciones de Spring** |
| `infrastructure/config` | Clases `@Configuration` que crean los casos de uso con `@Bean` | |
| `infrastructure/persistence` | `*Entity` (sin Lombok en `analisis`), `*JpaRepository`, `*PersistenceAdapter`; consultas con filtros en `*JdbcAdapter` (JdbcClient) | |
| `infrastructure/web` | Controladores, DTOs y `*ExceptionHandler` | Los handlers llevan `@Order(HIGHEST_PRECEDENCE)` |
| `infrastructure/ia` | Cliente HTTP (RestClient) hacia el servicio de IA | Si falla: `log.warn` y `Optional.empty()` |

Otros puntos del diseño:
- La API expone siempre `uuid_publico` como `id`, nunca el BIGINT interno.
- Motor de URL:
  - Los pesos se leen de la tabla `reglas_deteccion`.
  - Una regla en Java sin fila en la base aporta 0 puntos. `VerificacionReglasAlArrancar` lo avisa con un `WARN` al arrancar.
  - Las reglas sin estado van en `AnalisisConfig.reglasUrlBase()`. Typosquatting y lista negra se arman en cada ejecución dentro de `AnalizarUrlUseCase`.
  - Umbrales: sospechoso desde 40, phishing desde 70.
  - La caché de análisis dura 24 h, pero un análisis anterior al último cambio de listas
    (filas `DOMINIO_*` en `auditoria`, vía `CambiosListasPort`) ya no se reutiliza.
- Modelo de IA (`ia/`, ver `ia/FICHA_MODELO.md`):
  - Solo mira el host: PhiUSIIL tiene sesgos de ruta y https (las legítimas no tienen ruta) y de IP (todas phishing).
  - Indicador `IA_MODELO_URL`: aporte = probabilidad × peso (40), solo si supera el umbral del modelo.
  - No se consulta para IP literales ni para dominios de la lista blanca (comparación exacta por dominio registrable).
  - Si el servicio no responde en 2 s, el análisis sigue solo con las reglas.
  - El dataset y `modelo.joblib` no van a git; se regeneran con `descargar_datos.py` y `entrenar.py`.
- Las pruebas de dominio y de casos de uso usan dobles en memoria, sin Spring.
- Autenticación (JWT HS256 con Nimbus, BCrypt):
  - Roles en el dominio (`shared/domain/model/Rol`): ADMIN, ADMIN_EMPRESA y USUARIO. En la base
    se guardan como `admin`, `empresa` y `usuario`; el valor `analista` no se usa.
  - Reglas por ruta en `shared/infrastructure/security/SeguridadConfig`. Los casos de uso
    reciben `UsuarioActual` y filtran con `AlcanceDatos`: ADMIN ve todo, ADMIN_EMPRESA solo su
    empresa y USUARIO recibe 403.
  - En cada petición, `JwtUsuarioActualConverter` relee al usuario de la base, así que el rol y
    el estado del token no se usan para decidir permisos.
  - Tokens: el JWT dura 15 min. El de refresco es aleatorio, dura 7 días, se rota en cada uso y
    en `sesiones` solo se guarda su SHA-256.
  - Los accesos (`LOGIN_EXITOSO`, `LOGIN_FALLIDO`, `LOGOUT`) se escriben en `auditoria`.
  - Las acciones administrativas también (`shared/domain/port/out/AuditoriaPort`, acciones en
    `AccionAuditoria`). Si falla la escritura: `log.warn` y la acción no se deshace.
  - `/api/admin/auditoria/**`, `/api/admin/dominios/**` y `/api/admin/entidades/**` son solo
    ADMIN (y los casos de uso lo repiten con `AlcanceDatos.soloPlataforma`).
- Panel de administración:
  - Paginación: `shared/domain/model/Pagina` + `SolicitudPagina` (máximo 100), JSON `PaginaResponse`.
  - Un dato fuera del alcance de un ADMIN_EMPRESA responde 404, igual que uno inexistente.
  - Alertas: las transiciones válidas viven en `alerta/domain/model/EstadoAlerta`.
  - Listas: "eliminar" es `activo = false`; nunca se borran filas de `dominios_lista`.
  - Una empresa desactivada no recibe usuarios nuevos; sus usuarios actuales siguen entrando.
  - `POST /api/analisis` sigue siendo público. Con token, el análisis queda a nombre del
    usuario; los ids del cuerpo se ignoran.
- Hash de una contraseña para la base: `backend/herramientas/GenerarHashBcrypt.java` (ver su
  comentario). Lee la contraseña por teclado y no la muestra.

## Ejecutar

- **Backend:** `.\mvnw.cmd test` dentro de `backend/`. La prueba `contextLoads` necesita la base levantada y la variable `DB_PASSWORD`.
- **Frontend:** dentro de `frontend/`, `npm test -- --watch=false` y `npm run build`.
- **`DB_PASSWORD`:** es el valor de `POSTGRES_PASSWORD` en `database/.env`.
  - Si ese archivo no existe (pasó en esta máquina), tómala del contenedor sin imprimirla:
    `$env:DB_PASSWORD = (docker exec phishshield-db printenv POSTGRES_PASSWORD)`
  - **Nunca la muestres, nunca la escribas en un archivo y nunca la commitees.**
- **`JWT_SECRET`:** el backend no arranca sin ella (mínimo 32 caracteres). Las pruebas usan un
  secreto falso de `src/test/resources/config/application.yaml`. Para levantar el backend en
  una sesión, genérala al azar sin imprimirla:
  `$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); $env:JWT_SECRET = [Convert]::ToBase64String($b)`

## Migraciones

- Van numeradas (`V2`, `V3`, `V4`…), son idempotentes (`ON CONFLICT DO NOTHING`, `UPDATE … WHERE`) y nunca borran datos.
- Todo cambio de datos se escribe en dos lugares: en `02_seed.sql` y en una migración nueva. La tabla de migraciones de `database/README.md` también se actualiza.
- Los archivos de migración van **solo en ASCII**: PowerShell 5.1 convierte los acentos en `?` al pasarlos por la tubería.
- Cómo aplicarlas en PowerShell, que no admite `<` (desde la raíz del repo):
  `Get-Content database\migraciones\V4__ajuste_reglas_y_lista_blanca.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield`

## Problemas conocidos de este entorno

- **Commits desde PowerShell 5.1:** las comillas dobles dentro del mensaje rompen `git commit -m`, y el commit falla sin avisar. Haz los commits desde Bash con `git commit -F archivo`.
- **Mensajes rojos en PowerShell:** git escribe en stderr y PowerShell 5.1 lo muestra como `NativeCommandError` aunque el comando haya funcionado. Revisa el resultado real.
- **`docker compose ps` sale vacío en `database/`:** el contenedor se creó desde la carpeta vieja `C:\proyectos\phishshield-db`. Usa `docker ps`.
- **Puerto del frontend:** en las sesiones de Claude existe `PORT=0`, y por eso `ng serve` elige un puerto al azar en lugar del 4200.
  Ni siquiera `--port 4200` lo evita: hay que hacer antes `$env:PORT = "4200"`. El CORS del backend solo admite `localhost:4200`.
- **Tubería de PowerShell 5.1 hacia programas nativos:** antepone un BOM al texto (`"x" | java ...`
  recibe `﻿x`). `GenerarHashBcrypt` ya lo descarta; tenerlo en cuenta con otras herramientas.
- **`System.console()` en Java 22+:** ya no es `null` aunque la entrada venga redirigida; usar `isTerminal()`.
- **Contraseña pública:** la de la base es el valor por defecto de `docker-compose.yml` y ya está en el historial público. Está pendiente rotarla.
- **V9 aplicada en la base real (2026-11-10)**, con respaldo previo en `respaldo_antes_v9.dump`
  (raíz del repo, ignorado por git; restaurar con `pg_restore`).
- **La base real tiene 13 tablas**, no las 15 de `01_schema.sql` (faltan las de educación).
- **Python:** `python` del sistema abre la Microsoft Store. Las pruebas de la IA se corren con el
  entorno virtual: dentro de `ia/`, `.\.venv\Scripts\python.exe -m pytest`.
- **Git Bash convierte rutas** como `/tmp/x` en rutas de Windows al pasarlas a `docker exec`.
  Usar PowerShell o anteponer `MSYS_NO_PATHCONV=1`.

## Convenciones

- Commits en español y en imperativo, por ejemplo `Agregar validación de URL en el adaptador REST`.
- Una rama `feature/nombre-corto` por tarea; antes de empezar, revisar `git status` (si hay cambios sin commitear, preguntar).
- Antes de proponer un merge a `main`: `mvnw test`, `npm test` y `pytest` (en `ia/`) deben pasar.
- **Nunca hacer push ni merge a `main` sin preguntar.**
- **Nunca ejecutar `docker compose down -v`** ni nada que borre datos de la base.
- No afirmar que algo funciona sin haberlo ejecutado.
- Para calcular puntajes, usar el motor real con una prueba temporal que no se commitea, nunca cálculos a mano.
