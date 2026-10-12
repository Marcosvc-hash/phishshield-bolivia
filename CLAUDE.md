# PhishShield Bolivia — notas para Claude

Proyecto universitario de detección de phishing. El usuario es estudiante, viene de C# básico
y no tiene experiencia previa en Java: respóndele en español sencillo.

## Reglas de trabajo (permanentes)

1. **Antes de modificar código, explica qué vas a hacer y por qué, y espera la aprobación.**
   No hagas cambios fuera de lo acordado. Si encuentras un problema, repórtalo y propón la
   solución, pero no la apliques sin preguntar.
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

## Estructura

- `backend/`: Spring Boot 4.1 + Java 25, Maven Wrapper (`mvnw.cmd`).
- `frontend/`: Angular 22 + TypeScript 6, pruebas con Vitest.
- `database/`: PostgreSQL 18 en Docker (contenedor `phishshield-db`).
  - `initdb/01_schema.sql` (15 tablas) y `initdb/02_seed.sql`: solo corren con el volumen vacío.
  - `migraciones/V<n>__*.sql`: cambios para bases que ya existen.

## Arquitectura hexagonal del backend

Paquete `bo.phishshield.backend`, organizado por verticales: `analisis`, `empresa`, `usuario`, `shared`.
Cada vertical se divide así:

| Capa | Qué va ahí | Reglas |
|---|---|---|
| `domain/model` | Entidades, records, enums y excepciones de negocio | Sin Spring ni JPA (regla 4) |
| `domain/service` | Lógica pura, como las reglas `Regla*` del motor | Sin Spring ni JPA (regla 4) |
| `domain/port/out` | Interfaces de salida (`*RepositoryPort`) | Sin Spring ni JPA (regla 4) |
| `application` | Casos de uso (`*UseCase`) y comandos (`Comando*`) | **Sin anotaciones de Spring** |
| `infrastructure/config` | Clases `@Configuration` que crean los casos de uso con `@Bean` | |
| `infrastructure/persistence` | `*Entity` (sin Lombok en `analisis`), `*JpaRepository`, `*PersistenceAdapter` | |
| `infrastructure/web` | Controladores, DTOs y `*ExceptionHandler` | Los handlers llevan `@Order(HIGHEST_PRECEDENCE)` |
| `infrastructure/ia` | Cliente HTTP (RestClient) hacia el servicio de IA | Si falla: `log.warn` y `Optional.empty()` |

Otros puntos del diseño:
- La API expone siempre `uuid_publico` como `id`, nunca el BIGINT interno.
- Motor de URL:
  - Los pesos se leen de la tabla `reglas_deteccion`.
  - Una regla en Java sin fila en la base aporta 0 puntos. `VerificacionReglasAlArrancar` lo avisa con un `WARN` al arrancar.
  - Las reglas sin estado van en `AnalisisConfig.reglasUrlBase()`. Typosquatting y lista negra se arman en cada ejecución dentro de `AnalizarUrlUseCase`.
  - Umbrales: sospechoso desde 40, phishing desde 70.
  - La caché de análisis dura 24 h.
- Modelo de IA (`ia/`, ver `ia/FICHA_MODELO.md`):
  - Solo mira el host: PhiUSIIL tiene sesgos de ruta y https (las legítimas no tienen ruta) y de IP (todas phishing).
  - Indicador `IA_MODELO_URL`: aporte = probabilidad × peso (40), solo si supera el umbral del modelo.
  - No se consulta para IP literales ni para dominios de la lista blanca (comparación exacta por dominio registrable).
  - Si el servicio no responde en 2 s, el análisis sigue solo con las reglas.
  - El dataset y `modelo.joblib` no van a git; se regeneran con `descargar_datos.py` y `entrenar.py`.
- Las pruebas de dominio y de casos de uso usan dobles en memoria, sin Spring.

## Ejecutar

- **Backend:** `.\mvnw.cmd test` dentro de `backend/`. La prueba `contextLoads` necesita la base levantada y la variable `DB_PASSWORD`.
- **Frontend:** dentro de `frontend/`, `npm test -- --watch=false` y `npm run build`.
- **`DB_PASSWORD`:** es el valor de `POSTGRES_PASSWORD` en `database/.env`.
  - Si ese archivo no existe (pasó en esta máquina), tómala del contenedor sin imprimirla:
    `$env:DB_PASSWORD = (docker exec phishshield-db printenv POSTGRES_PASSWORD)`
  - **Nunca la muestres, nunca la escribas en un archivo y nunca la commitees.**

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
- **Contraseña pública:** la de la base es el valor por defecto de `docker-compose.yml` y ya está en el historial público. Está pendiente rotarla.

## Convenciones

- Commits en español y en imperativo, por ejemplo `Agregar validación de URL en el adaptador REST`.
- Una rama `feature/nombre-corto` por tarea; antes de empezar, revisar `git status` (si hay cambios sin commitear, preguntar).
- Antes de proponer un merge a `main`: `mvnw test`, `npm test` y `pytest` (en `ia/`) deben pasar.
- **Nunca hacer push ni merge a `main` sin preguntar.**
- **Nunca ejecutar `docker compose down -v`** ni nada que borre datos de la base.
- No afirmar que algo funciona sin haberlo ejecutado.
- Para calcular puntajes, usar el motor real con una prueba temporal que no se commitea, nunca cálculos a mano.
