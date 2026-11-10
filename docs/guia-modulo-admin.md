# Guía del módulo de administración

Esta guía explica, módulo por módulo, qué se construyó en la rama `feature/admin-completo`,
qué archivos se tocaron, cómo viaja una petición desde el botón en Angular hasta la tabla en
PostgreSQL y qué equivale a cada pieza en C#/.NET. Sirve para defender el código.

Rutas abreviadas en las tablas:
`B/` = `backend/src/main/java/bo/phishshield/backend/`,
`T/` = `backend/src/test/java/bo/phishshield/backend/`,
`F/` = `frontend/src/app/`.

---

## 0. Antes de empezar

### Commits de la rama (uno por módulo)

| Commit | Módulo |
|---|---|
| `b790466` | 1. Historial de análisis |
| `b359ae9` | 4. Registro de accesos y auditoría (va segundo: los demás escriben en `auditoria`) |
| `0a95dc0` | 2. Alertas, más la migración V9 |
| `b2d1e6e` | 3. Listas de dominios e invalidación de la caché |
| `a541cdb` | 5. Empresas |
| `e8a77e2` | 6. Usuarios |

### La migración V9

`database/migraciones/V9__uuid_alertas_y_dominios.sql` agrega `uuid_publico` a `alertas` y a
`dominios_lista`. Sin ella, las páginas de **Alertas** y **Listas de dominios** responden error
500, porque sus consultas usan esa columna. Las demás páginas funcionan igual.

**Estado:** se aplicó en la base real el 2026-11-10. Antes se tomó un respaldo,
`respaldo_antes_v9.dump`, que está en la raíz del repo y no se sube a git. Se comprobó que ese
respaldo se restaura con los mismos conteos. Después de aplicarla, Alertas y Listas respondieron
200 en vivo, sin errores 500.

En otra máquina, se aplica así (desde la raíz del repo, en PowerShell):

```powershell
Get-Content database\migraciones\V9__uuid_alertas_y_dominios.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield -v ON_ERROR_STOP=1 --single-transaction
```

Se probó en una base temporal:

- Se ejecutó dos veces seguidas (es idempotente).
- Se mantuvieron las mismas filas: 5 alertas y 15 dominios.
- Cada fila recibió un uuid distinto.
- Una base creada desde cero con el `01_schema.sql` nuevo quedó con sus 15 tablas.

### Arquitectura hexagonal en una imagen

```
 Angular (botón)                         ┌──────────── backend ────────────┐
   │  servicio.ts  ── HTTP + JWT ──►  infrastructure/web   (Controller, DTO)
   │                                       │  traduce JSON ⇄ comandos
   │                                  application          (*UseCase)
   │                                       │  coordina: permisos, orden, auditoría
   │                                  domain               (modelo, reglas, puertos)
   │                                       ▲  interfaces *Port: el dominio no sabe de SQL
   │                                  infrastructure/persistence (*JdbcAdapter)
   │                                       │  SQL con JdbcClient
   └──────────────────────────────────  PostgreSQL (tablas)
```

- **Dominio limpio (regla 4).** Nada dentro de `domain/` importa Spring ni JPA. Se comprueba con:

  ```
  git grep -nE "import (org\.springframework|jakarta\.persistence)" -- "backend/src/main/java/**/domain/**"
  ```

  Ese comando no devuelve nada.
- **Capa `application` sin anotaciones.** Los casos de uso se crean con `@Bean` en las clases
  `*Config`.
- **Seguridad en dos barreras.** Primero, `SeguridadConfig` filtra por ruta. Después, cada caso de
  uso vuelve a revisar el rol con `AlcanceDatos`. Si alguien olvida una regla de ruta, el dominio
  igual responde 403.

### Piezas compartidas (commit 1 y 2)

| Pieza | Para qué | Equivalente en C# |
|---|---|---|
| `B/shared/domain/model/SolicitudPagina` | Página pedida y tamaño. Corrige los valores fuera de rango (máximo 100). | Un `record PageRequest(int Page, int Size)` |
| `B/shared/domain/model/Pagina<T>` | Una página de resultados y el total de filas. | `PagedResult<T>` |
| `B/shared/domain/model/RangoFechas` | Filtro "desde/hasta" en hora de Bolivia, con el día "hasta" incluido completo. | Un `record` con `DateOnly` y `TimeZoneInfo` |
| `AlcanceDatos.incluye(idEmpresa)` / `soloPlataforma(quien)` | ¿Este dato está en mi alcance? ¿Soy ADMIN? | Un *policy* de autorización propio |
| `B/shared/domain/port/out/AuditoriaPort` + `EventoAuditoria` + `AccionAuditoria` | Escribir una acción administrativa en la tabla `auditoria`. | Una interfaz `IAuditLog` |
| `B/shared/infrastructure/persistence/AuditoriaJdbcAdapter` | El `INSERT INTO auditoria` (JSONB e INET con `CAST`). | Una implementación con Dapper |
| `B/shared/infrastructure/persistence/FiltrosSql` | Filtro por empresa reutilizable, y `ILIKE` con los comodines escapados. | Un *helper* de SQL |
| `B/shared/infrastructure/web/dto/PaginaResponse`, `FechasLocales`, `CambiarEstadoRequest` | Formato JSON común de las páginas, fechas en `-04:00` y el cuerpo `{"activo": …}`. | DTOs compartidos |
| `F/core/pagina.ts` | Tipo `Pagina<T>` y `parametros()`, que quita los filtros vacíos. | — |
| `F/core/ui/paginador.ts` | Botones Anterior/Siguiente; recibe datos por `input()` y avisa por `output()`. | Un componente Blazor con `[Parameter]` y `EventCallback` |
| `frontend/src/styles.css` | Estilos comunes de administración: `.filtros`, `.formulario`, `.btn`, `.chip`. | Un CSS global |

### Equivalencias de Java/Spring que aparecen en todos los módulos

| Java / Spring / Angular | C# / .NET |
|---|---|
| `@RestController` + `@RequestMapping("/api/admin/…")` | `[ApiController]` + `[Route("api/admin/…")]` |
| `@GetMapping`, `@PostMapping`, `@PatchMapping`, `@PutMapping` | `[HttpGet]`, `[HttpPost]`, `[HttpPatch]`, `[HttpPut]` |
| `@RequestParam(required = false) String q` | `[FromQuery] string? q` |
| `@PathVariable UUID id` | `Guid id` tomado de la ruta |
| `@Valid @RequestBody XRequest` | `[FromBody] XRequest` + DataAnnotations (validación automática del modelo) |
| `@NotBlank`, `@Size`, `@Pattern` | `[Required]`, `[StringLength]`, `[RegularExpression]` |
| `@AuthenticationPrincipal UsuarioActual quien` | Leer `HttpContext.User` (los *claims*) |
| `@Configuration` + `@Bean` | `builder.Services.AddSingleton(...)` en `Program.cs` |
| `@Component` en un adaptador | `services.AddScoped<IPort, Adapter>()` |
| `@RestControllerAdvice` + `@ExceptionHandler` | Un *middleware* de excepciones o un `IExceptionFilter` |
| `@Order(Ordered.HIGHEST_PRECEDENCE)` | Registrar ese filtro antes que el genérico |
| `JdbcClient.sql(...).param(...).query(...)` | Dapper: `conn.Query<T>(sql, new { ... })` |
| `JpaRepository` (Spring Data) | EF Core `DbSet<T>` + repositorio |
| `record` | `record` (igual que en C# 9+) |
| `Optional<T>` | Un `T?` que obliga a revisar si viene vacío |
| `@Transactional` | `using var tx = conn.BeginTransaction()` / `TransactionScope` |
| `Clock` inyectado | `TimeProvider` (.NET 8) |
| `signal()` / `computed()` (Angular) | Una propiedad observable / `INotifyPropertyChanged` |
| `HttpClient` + `Observable` (Angular) | `HttpClient` + `Task` |
| `HttpTestingController` (pruebas Angular) | `MockHttpMessageHandler` |
| JUnit `@Test`, `@ParameterizedTest` + `@CsvSource` | xUnit `[Fact]`, `[Theory]` + `[InlineData]` |

---

## 1. Historial de análisis

**Qué hace.** `GET /api/admin/analisis` devuelve los análisis paginados en el servidor, con
filtros por resultado, rango de fechas y texto (dentro del dominio o de la URL completa). Al hacer
clic en una fila, `GET /api/admin/analisis/{id}` trae el detalle: cada indicador con su
descripción, la evidencia y el aporte (incluido `IA_MODELO_URL`), además del modelo de IA usado y
el estado de la alerta.

Quién ve qué:

- El **ADMIN** ve todo, incluidos los análisis anónimos.
- El **ADMIN_EMPRESA** ve solo los de su empresa.
- Si pide el detalle de un análisis de otra empresa, recibe **404**, igual que si no existiera.
  Así no puede averiguar qué ids existen.

### Archivos

| Capa | Archivo |
|---|---|
| Dominio | `B/historial/domain/model/FiltroHistorial`, `AnalisisFila`, `DetalleAnalisis`, `AnalisisNoEncontradoException` |
| Puerto | `B/historial/domain/port/out/HistorialQueryPort` |
| Casos de uso | `B/historial/application/ListarHistorialUseCase`, `ObtenerDetalleAnalisisUseCase` |
| Config | `B/historial/infrastructure/config/HistorialConfig` |
| Persistencia | `B/historial/infrastructure/persistence/HistorialJdbcAdapter` |
| Web | `B/historial/infrastructure/web/AdminAnalisisController`, `HistorialExceptionHandler`, `dto/AnalisisFilaResponse`, `dto/DetalleAnalisisResponse` |
| Angular | `F/paginas/admin/historial/historial.ts/.html/.css/.service.ts/.spec.ts`, `F/app.routes.ts`, `F/layout/admin-layout/admin-layout.ts` |
| Pruebas | `T/historial/application/HistorialUseCaseTest`, `T/shared/domain/model/PaginacionYFechasTest`, `F/core/ui/paginador.spec.ts` |

### Recorrido: "Buscar" con resultado = phishing y texto "bnb"

1. **Angular.** El botón "Buscar" de `historial.html` es el `(ngSubmit)` del formulario y llama a
   `Historial.buscar()`. Ese método llama a `cargar(0)`.
2. `HistorialService.listar(filtro, 0)` arma
   `GET /api/admin/analisis?resultado=phishing&q=bnb&pagina=0&tamano=20`. La función
   `parametros()` quita los filtros vacíos.
3. `authInterceptor` agrega el encabezado `Authorization: Bearer <jwt>`.
4. **Spring Security.** Se valida la firma del JWT. Después, `JwtUsuarioActualConverter` relee al
   usuario de la tabla `usuarios` y arma el `UsuarioActual`. `SeguridadConfig` deja pasar a ADMIN y
   ADMIN_EMPRESA en `/api/admin/**`.
5. **Controlador.** `AdminAnalisisController.listar` convierte los textos de la petición:
   - `resultado` pasa al enum `Resultado` con `FiltroHistorial.resultadoDesdeTexto`; un valor
     inválido responde 400;
   - las fechas pasan a `RangoFechas`; si "desde" es posterior a "hasta", responde 400;
   - la paginación pasa a `SolicitudPagina`.
6. **Caso de uso.** `ListarHistorialUseCase.ejecutar` calcula
   `AlcanceDatos.administrativo(quien)`. El ADMIN recibe `idEmpresa = null`; el ADMIN_EMPRESA, el
   id de su empresa; el USUARIO, 403.
7. **Adaptador.** `HistorialJdbcAdapter.buscar` ejecuta dos SQL con el mismo `WHERE`: uno con
   `COUNT(*)` y otro con `ORDER BY fecha_analisis DESC LIMIT :limite OFFSET :salto`.
8. **PostgreSQL** consulta las tablas `analisis`, `usuarios`, `empresas` y `alertas` (`LEFT JOIN`).
   Cada filtro tiene la forma `(CAST(:p AS tipo) IS NULL OR condición)`: si el parámetro llega
   vacío, deja pasar todo. Los valores van siempre como parámetros, nunca pegados al SQL, y eso
   evita la inyección SQL.
9. **Respuesta.** Sale como `PaginaResponse<AnalisisFilaResponse>`, con las fechas en hora de
   Bolivia. Angular la guarda en el signal `pagina` y la tabla se redibuja.

Para el detalle, `abrir(fila)` llama a `GET /api/admin/analisis/{id}`. El caso de uso pide el
detalle **sin filtro por empresa** y revisa `alcance.incluye(d.idEmpresa())`. Se hizo así para
poder probar esa regla con un doble en memoria. Los indicadores salen de
`analisis_indicadores JOIN reglas_deteccion`.

### Decisiones

- **El filtro por empresa del listado va en el SQL.** Si se filtrara en Java después de paginar,
  una página podría salir con 3 filas en vez de 20 y el total no cuadraría.
- **Texto con comodines.** `FiltrosSql.contiene` escapa `%` y `_`. Sin eso, buscar `mi_sitio`
  encontraría también `miXsitio`.
- **Suma de aportes.** El detalle muestra la suma, y puede pasar de 100: el puntaje final se
  recorta a 100 en el dominio (`Analisis`).

---

## 2. Alertas

**Qué hace.** Es una bandeja con filtros por estado y severidad que muestra primero las alertas
abiertas. Tiene tres endpoints:

- `GET /api/admin/alertas`: la bandeja;
- `GET /api/admin/alertas/pendientes`: devuelve `{"cantidad": n}` para el número del menú
  lateral;
- `PATCH /api/admin/alertas/{id}` con `{estado?, notas?}`.

Ciclo de vida:

```
pendiente ──► en_revision ──► revisada
                         └──► descartada      (revisada y descartada son finales)
```

### Archivos

| Capa | Archivo |
|---|---|
| Migración | `database/migraciones/V9__uuid_alertas_y_dominios.sql`, `database/initdb/01_schema.sql`, `database/README.md` |
| Dominio | `B/alerta/domain/model/EstadoAlerta` (**transiciones**), `AlertaGestionada` (**reglas**), `SeveridadAlerta`, `FiltroAlertas`, `AlertaFila`, `TransicionAlertaInvalidaException`, `AlertaNoEncontradaException`, `AlertaModificadaException` |
| Puerto | `B/alerta/domain/port/out/AlertaRepositoryPort` |
| Casos de uso | `B/alerta/application/ListarAlertasUseCase`, `ActualizarAlertaUseCase`, `ComandoActualizarAlerta` |
| Config | `B/alerta/infrastructure/config/AlertaConfig` |
| Persistencia | `B/alerta/infrastructure/persistence/AlertaJdbcAdapter` (y un comentario en `B/analisis/infrastructure/persistence/AlertaEntity`) |
| Web | `B/alerta/infrastructure/web/AdminAlertaController`, `AlertaExceptionHandler`, `dto/AlertaResponse`, `dto/ActualizarAlertaRequest` |
| Angular | `F/paginas/admin/alertas/*`; número en el menú: `F/layout/admin-layout/admin-layout.ts/.html/.css/.spec.ts` |
| Pruebas | `T/alerta/domain/model/AlertaGestionadaTest` (las 16 combinaciones de estados), `T/alerta/application/ActualizarAlertaUseCaseTest` |

### Recorrido: "Descartar (falso positivo)" con una nota

1. **Angular.** En `alertas.html`, el botón llama a `aplicar(a, 'descartada')`. El método manda
   también las notas escritas: `AlertasService.actualizar(id, {estado: 'descartada', notas})`
   arma `PATCH /api/admin/alertas/{id}`.
2. **Seguridad.** Es igual que en el módulo 1: ADMIN y ADMIN_EMPRESA pasan.
3. **Controlador.** `AdminAlertaController.actualizar` hace dos cosas:
   - convierte el texto `"descartada"` en el enum `EstadoAlerta`; un texto desconocido responde
     400;
   - arma `ComandoActualizarAlerta(id, estado, notas, ip)`; si no viene ni estado ni notas,
     responde 400.
   El controlador **no** revisa las transiciones.
4. **Caso de uso.** `ActualizarAlertaUseCase.ejecutar`:
   1. Calcula el alcance. Busca la alerta con `buscarPorUuid` y comprueba
      `alcance.incluye(idEmpresa)`; si es de otra empresa, responde 404.
   2. Guarda el estado leído (`en_revision`).
   3. **Primero las notas**: `alerta.anotar(...)`. Después de cerrarla ya no se podría anotar.
   4. `alerta.cambiarEstado(DESCARTADA, quien, ahora)`. Aquí el **dominio** decide:
      - si `EstadoAlerta.puedePasarA` es falso, lanza `TransicionAlertaInvalidaException` (409);
      - si el destino es final, guarda `fechaCierre` (nunca anterior a `fecha_notificacion`, por
        el CHECK `chk_alertas_cierre`).
   5. `alertas.actualizar(alerta, estadoLeido)`.
   6. Escribe en `auditoria` la acción `ALERTA_ACTUALIZADA`, con el estado anterior y el nuevo.
5. **Adaptador.** `AlertaJdbcAdapter.actualizar` ejecuta:

   ```sql
   UPDATE alertas SET estado=…, id_asignado_a=…, notas=…, fecha_cierre=…
   WHERE id_alerta = :id AND estado = :leido
   ```

   Si otro administrador la cambió un segundo antes, no actualiza ninguna fila. Entonces el caso
   de uso lanza `AlertaModificadaException` (409) y el primero no pierde su cambio. Esto es
   *control de concurrencia optimista*; en EF Core sería un `[ConcurrencyCheck]`.
6. **PostgreSQL.** Se escriben las tablas `alertas` y `auditoria`.
7. **Angular.** Reemplaza la fila y llama a `refrescarPendientes()`, que actualiza el signal
   `pendientes`. Ese signal es el que muestra el menú lateral junto a "Alertas".

### Decisiones

- **El asignado** es quien toma la alerta en revisión. No hay un selector de otras personas.
- **Pedir el mismo estado** que ya tiene la alerta no es un error: no cambia nada.
- **El id de la alerta** es `alertas.uuid_publico`, que agrega la V9 (decisión **1B**).
- **Se usa SQL directo y no `AlertaEntity`.** Hibernate tiene `ddl-auto: validate`: si
  `AlertaEntity` mapeara `uuid_publico`, el backend no arrancaría en una base sin la V9.

---

## 3. Listas de dominios (solo ADMIN)

**Qué hace.** Es un CRUD sobre `dominios_lista`:

- `GET /api/admin/dominios`: lista, con filtros por lista, estado y texto;
- `POST /api/admin/dominios`: agregar a la lista blanca o a la negra, con motivo y entidad
  suplantada opcional;
- `PUT /api/admin/dominios/{id}`: corregir el motivo y la entidad;
- `PATCH /api/admin/dominios/{id}/estado`: "eliminar" (`activo = false`) o reactivar;
- `GET /api/admin/entidades`: alimenta el selector de entidad.

**No hay `DELETE`**: nunca se borran filas.

### Archivos

| Capa | Archivo |
|---|---|
| Dominio | `B/listas/domain/model/NombreDominio` (**normalización y formato**), `DominioEnLista`, `TipoLista`, `DominioFila`, `EntidadSuplantada`, `FiltroDominios`, `ConflictoListaException`, `DominioNoEncontradoException` |
| Puertos | `B/listas/domain/port/out/DominioListaRepositoryPort`, `EntidadSuplantadaPort` |
| Casos de uso | `B/listas/application/AgregarDominioUseCase`, `EditarDominioUseCase`, `CambiarEstadoDominioUseCase`, `ListarDominiosUseCase`, `ComandoDominio`, `CambiosDeListas` (pasos comunes) |
| Config | `B/listas/infrastructure/config/ListasConfig` |
| Persistencia | `B/listas/infrastructure/persistence/DominioListaJdbcAdapter`, `EntidadSuplantadaJdbcAdapter` |
| Web | `B/listas/infrastructure/web/AdminDominioController`, `ListasExceptionHandler`, `dto/DominioRequest`, `dto/DominioResponse` |
| Caché del motor | `B/analisis/domain/port/out/CambiosListasPort`, `B/analisis/infrastructure/persistence/CambiosListasJdbcAdapter`, `B/analisis/application/AnalizarUrlUseCase`, `B/analisis/infrastructure/config/AnalisisConfig` |
| Seguridad | `B/shared/infrastructure/security/SeguridadConfig` (`/api/admin/dominios/**` y `/api/admin/entidades/**` solo ADMIN) |
| Angular | `F/paginas/admin/dominios/*` |
| Pruebas | `T/listas/domain/model/NombreDominioTest`, `T/listas/application/ListasUseCaseTest`, `T/analisis/application/AnalizarUrlUseCaseTest` (3 pruebas nuevas de caché) |

### Recorrido: agregar " BNB-Pagos.XYZ " a la lista negra

1. **Angular.** El botón "Agregar" (`ngSubmit`) llama a `Dominios.guardar()`, y
   `DominiosService.agregar(...)` arma `POST /api/admin/dominios` con
   `{dominio, lista: "negra", motivo, entidad}`.
2. **Seguridad.** `SeguridadConfig` exige `hasRole("ADMIN")` en `/api/admin/dominios/**`: un
   ADMIN_EMPRESA recibe 403 aquí.
3. **Controlador.** `AdminDominioController.agregar` valida lo básico con `@Valid` (motivo no
   vacío, largos) y arma `ComandoDominio`.
4. **Caso de uso.** `AgregarDominioUseCase.ejecutar`:
   1. Segunda barrera: `AlcanceDatos.soloPlataforma(quien)`.
   2. `NombreDominio.de(" BNB-Pagos.XYZ ")` lo convierte en `bnb-pagos.xyz`. El **dominio**
      normaliza y valida, y rechaza:
      - un esquema, una ruta o un puerto;
      - etiquetas inválidas;
      - un TLD con dígitos, como una IP;
      - un sufijo público entero, como `com.bo`;
      - caracteres que no son ASCII.
   3. `cambios.idEntidad("Banco Nacional de Bolivia")` busca el id interno por nombre, que es
      UNIQUE. Así la API no expone el BIGINT.
   4. `cambios.validarSinChoque(...)`: si el dominio está activo en la lista **blanca**, responde
      409.
   5. Si ya existe en la negra: activo → 409; desactivado → se **reactiva la misma fila**, porque
      hay `UNIQUE(dominio, lista)`.
   6. Si es nuevo: `DominioEnLista.nuevo(...)`. En la lista blanca exige el dominio registrable,
      porque el motor la compara por dominio registrable exacto.
   7. `dominios.guardar(...)` y luego `auditoria`: `DOMINIO_AGREGADO`.
5. **Adaptador.** `DominioListaJdbcAdapter.guardar` ejecuta
   `INSERT INTO dominios_lista … RETURNING id_dominio, uuid_publico, …`.
6. **PostgreSQL.** Se escriben `dominios_lista` y `auditoria`. El CHECK `chk_dominios_minus`
   vuelve a garantizar las minúsculas.
7. **Angular.** Muestra el aviso y recarga la página actual de la tabla.

### Cómo afecta la caché de 24 h a las URL ya analizadas

**Antes.** `AnalizarUrlUseCase` buscaba un análisis previo por el SHA-256 de la URL. Si tenía
menos de 24 h, lo devolvía tal cual, sin volver a aplicar las reglas. Por eso un cambio en las
listas tardaba hasta 24 h en notarse para quien ya hubiera analizado esa URL:

| Caso | Qué pasaba |
|---|---|
| La URL se analizó esta mañana y salió "sospechoso"; después se agregó a la **lista negra**. | Seguía saliendo "sospechoso" hasta 24 h. Una página de phishing confirmada no se marcaba como tal. |
| Se **quitó** un dominio de la lista negra porque era un falso positivo. | Seguía saliendo "phishing" hasta 24 h. |
| Se agregó un dominio a la **lista blanca**. | Seguía con la marca de typosquatting que tenía. |

**Ahora (decisión 2 aprobada).** Un análisis guardado deja de servir si es **anterior al último
cambio de listas**. La fecha de ese cambio sale de `auditoria`: cada alta, edición, baja o
reactivación deja una fila `DOMINIO_*`. `CambiosListasJdbcAdapter` ejecuta:

```sql
SELECT MAX(creado_en) FROM auditoria WHERE accion IN ('DOMINIO_AGREGADO', …)
```

Y `AnalizarUrlUseCase.sigueVigente` exige las dos condiciones:

```java
previo.getFechaAnalisis().isAfter(hace24h)  &&  previo.getFechaAnalisis().isAfter(ultimoCambio)
```

- **Comprobado con una prueba.** `el_analisis_guardado_se_invalida_al_cambiar_la_lista_negra`, en
  `AnalizarUrlUseCaseTest`, primero analiza la URL (sale sospechosa), luego la agrega a la lista
  negra y la vuelve a analizar: se recalcula, sale phishing y queda una segunda fila.
- **Comprobado en vivo**, en la base temporal: `http://pagos-tigo-bo.xyz/recarga` pasó de
  sospechoso (65) a phishing (100) al agregarla, y volvió a sospechoso al desactivarla.
- **Costo.** Después de un cambio de listas, cada URL se recalcula una vez. Si no es segura, se
  crea un análisis nuevo con su alerta, porque la alerta va unida a cada análisis.
- **Límite conocido.** Si la escritura en `auditoria` fallara (solo deja un WARN), ese cambio no
  invalidaría la caché, y regiría el plazo normal de 24 h.
- **Por qué global y no por dominio.** Es simple de explicar y de probar. Invalidar solo las URL
  de ese dominio exigiría guardar más datos por análisis.

---

## 4. Registro de accesos (solo ADMIN) y auditoría

**Qué hace.** `GET /api/admin/auditoria` es un listado paginado, con filtros por acción, usuario
(parte del email) y fechas, que muestra la IP y el detalle. `GET /api/admin/auditoria/acciones`
llena el selector de acciones. Además, todos los módulos nuevos **escriben** en `auditoria`:

| Acción | La escribe |
|---|---|
| `USUARIO_ACTIVADO` / `USUARIO_DESACTIVADO` | `CambiarEstadoUsuarioUseCase` |
| `USUARIO_CREADO` / `USUARIO_ROL_CAMBIADO` | `CrearUsuarioAdminUseCase` / `CambiarRolUsuarioUseCase` |
| `ALERTA_ACTUALIZADA` | `ActualizarAlertaUseCase` |
| `DOMINIO_AGREGADO` / `EDITADO` / `DESACTIVADO` / `REACTIVADO` | casos de uso de `listas/` |
| `EMPRESA_CREADA` / `EDITADA` / `ACTIVADA` / `DESACTIVADA` | casos de uso de `empresa/` |
| `LOGIN_EXITOSO` / `LOGIN_FALLIDO` / `LOGOUT` | ya existían (`RegistroAccesoJdbcAdapter`) |

### Archivos

| Capa | Archivo |
|---|---|
| Escritura (compartida) | `B/shared/domain/model/AccionAuditoria`, `EventoAuditoria`, `B/shared/domain/port/out/AuditoriaPort`, `B/shared/infrastructure/persistence/AuditoriaJdbcAdapter` |
| Lectura: dominio | `B/auditoria/domain/model/FiltroAuditoria`, `RegistroAuditoria` |
| Lectura: puerto | `B/auditoria/domain/port/out/AuditoriaQueryPort` |
| Caso de uso | `B/auditoria/application/ListarAuditoriaUseCase` |
| Config | `B/auditoria/infrastructure/config/AuditoriaConfig` |
| Persistencia | `B/auditoria/infrastructure/persistence/AuditoriaJdbcQueryAdapter` |
| Web | `B/auditoria/infrastructure/web/AuditoriaController`, `dto/RegistroAuditoriaResponse` |
| Seguridad | `B/shared/infrastructure/security/SeguridadConfig` |
| Usuario (estado) | `B/usuario/application/CambiarEstadoUsuarioUseCase`, `B/usuario/infrastructure/config/UsuarioConfig`, `B/usuario/infrastructure/web/AdminUsuarioController` |
| Angular | `F/paginas/admin/auditoria/*` |
| Pruebas | `T/auditoria/application/ListarAuditoriaUseCaseTest`, `T/shared/dobles/AuditoriaEnMemoria` (doble compartido), `T/shared/infrastructure/security/SeguridadRutasTest`, `T/usuario/application/CambiarEstadoUsuarioUseCaseTest` |

### Recorrido A: desactivar a un usuario, hasta la fila en `auditoria`

1. **Angular.** `Usuarios.alternarEstado(u)` llama a
   `PATCH /api/admin/usuarios/{id}/estado` con `{"activo": false}`.
2. **Controlador.** `AdminUsuarioController.cambiarEstado` pasa `http.getRemoteAddr()` como IP.
3. **Caso de uso.** `CambiarEstadoUsuarioUseCase`:
   - `PoliticaGestionUsuarios` valida los permisos;
   - `usuario.desactivar()`;
   - `usuarioRepository.guardar(...)`;
   - arma `EventoAuditoria(USUARIO_DESACTIVADO, quien, "usuarios", id, {email, estado_anterior, estado_nuevo}, ip)`;
   - lo entrega a `auditoria.registrar(...)`.
4. **Adaptador.** `AuditoriaJdbcAdapter` ejecuta
   `INSERT INTO auditoria (…, detalle, ip_origen) VALUES (…, CAST(:detalle AS JSONB), CAST(:ip AS INET))`.
5. **PostgreSQL.** Se escribe la tabla `auditoria`, con su índice GIN sobre `detalle`.

### Recorrido B: ver el registro filtrado

`Auditoria.buscar()` llama a `GET /api/admin/auditoria?accion=LOGIN_FALLIDO&usuario=carlos`:

1. `SeguridadConfig` solo deja pasar al ADMIN.
2. `ListarAuditoriaUseCase` vuelve a exigirlo con `soloPlataforma`.
3. `AuditoriaJdbcQueryAdapter` busca el email en la cuenta (`usuarios.email`) **o** en el detalle
   (`detalle ->> 'email'`). Así aparecen también los intentos con emails que no existen, porque
   esas filas no tienen `id_usuario`.
4. La IP sale con `host(ip_origen)` y el detalle llega a Angular como objeto JSON.

### Decisión

**Auditoría "best effort".** Igual que en los logins, si falla el `INSERT` en `auditoria` se deja
un `WARN` y la acción ya hecha no se deshace. Para que la acción y su auditoría fueran una sola
transacción, la capa `application` tendría que usar transacciones de Spring, y la regla del
proyecto dice que esa capa no lleva Spring.

---

## 5. Empresas

**Qué hace.** Antes solo se podía listar y crear. Ahora también:

- `PUT /api/empresas/{id}`: editar;
- `PATCH /api/empresas/{id}/estado`: activar o desactivar.

Todo `/api/empresas/**` es solo para el ADMIN.

### Archivos

| Capa | Archivo |
|---|---|
| Dominio | `B/empresa/domain/model/Empresa` (`actualizarDatos`, `activar`, `desactivar`; valida el NIT; los textos vacíos quedan como null), `EmpresaNoEncontradaException` |
| Puerto | `B/empresa/domain/port/out/EmpresaRepositoryPort` (+ `existeNitEnOtra`) |
| Casos de uso | `B/empresa/application/EditarEmpresaUseCase`, `CambiarEstadoEmpresaUseCase`, `CrearEmpresaUseCase` (ahora con permisos y auditoría), `AuditoriaEmpresa` |
| Config | `B/empresa/infrastructure/config/EmpresaConfig` |
| Persistencia | `B/empresa/infrastructure/persistence/EmpresaPersistenceAdapter` (**corrección**), `EmpresaJpaRepository` (+ `existsByNitAndIdEmpresaNot`) |
| Web | `B/empresa/infrastructure/web/EmpresaController`, `EmpresaExceptionHandler`, `dto/EmpresaRequest` (antes `CrearEmpresaRequest`), `dto/EmpresaResponse` |
| Angular | `F/empresa.service.ts`, `F/empresa.model.ts`, `F/paginas/admin/empresas/*` (incluida la prueba nueva `empresas.spec.ts`) |
| Pruebas | `T/empresa/application/CrearEmpresaUseCaseTest`, `EditarEmpresaUseCaseTest` |

### Recorrido: "Editar" → "Guardar cambios"

1. **Angular.** `Empresas.empezarEdicion(e)` llena el formulario. Al guardar, `guardar()` llama a
   `EmpresaService.editar(id, datos)`, que arma `PUT /api/empresas/{id}`.
2. **Seguridad y controlador.** `/api/empresas/**` es solo ADMIN. `EmpresaController.editar`
   valida el DTO con `@Valid`: el NIT puede venir vacío o con 7 a 15 dígitos.
3. **Caso de uso.** `EditarEmpresaUseCase`:
   - `soloPlataforma`;
   - `buscarPorUuid`;
   - `empresa.actualizarDatos(...)`, donde el **dominio** valida el nombre, el NIT y el plan;
   - `existeNitEnOtra(nit, id)`: el NIT de *otra* empresa responde 409; el suyo propio no;
   - `guardar`;
   - auditoría `EMPRESA_EDITADA`.
4. **Adaptador.** `EmpresaPersistenceAdapter.guardar`, si la empresa ya tiene id, primero hace
   `findById` y luego actualiza los campos y `save`. JPA emite un `UPDATE empresas …`, y el
   trigger `trg_empresas_actualizado` pone `actualizado_en`.

**Error corregido.** Antes, `guardar()` armaba siempre una `EmpresaEntity` nueva sin id. Si se
hubiera usado para editar, JPA habría hecho un `INSERT`: una empresa duplicada con otro uuid. La
prueba `edita_la_misma_fila_sin_crear_otra` lo cubre, y en vivo se comprobó que el número de filas
no cambió al editar.

### Decisión: qué significa desactivar una empresa (aprobada, opción "recomendada")

- La empresa **queda marcada como inactiva** y conserva todo su historial: análisis, alertas y
  usuarios.
- **No se le pueden crear usuarios nuevos.** Lo aplica `PoliticaGestionUsuarios.validarEmpresaActiva`
  en el módulo 6; además, el formulario de usuarios solo ofrece empresas activas.
- **Sus usuarios actuales siguen entrando.** Bloquearlos habría exigido tocar
  `JwtUsuarioActualConverter`, es decir, la seguridad de todas las peticiones. Si hace falta
  cortar el acceso de alguien, se desactiva su cuenta desde "Usuarios": rige en su siguiente
  petición, porque el backend relee el estado del usuario cada vez.
- **Por qué así.** Desactivar una empresa suele ser una decisión comercial (dejó de pagar,
  pausa), no un incidente de seguridad. Cortar de golpe el acceso de todos sus usuarios sería un
  efecto grande y difícil de deshacer para un solo clic. Si en el futuro se quiere el bloqueo
  total, se agrega una revisión de `empresa.activa` en `JwtUsuarioActualConverter`.

---

## 6. Usuarios: alta desde el panel y cambio de rol

**Qué hace.** Agrega dos endpoints:

- `POST /api/admin/usuarios`: nombres, apellidos, email, contraseña inicial, rol y empresa
  opcional;
- `PATCH /api/admin/usuarios/{id}/rol`.

Reglas, todas en `PoliticaGestionUsuarios` (dominio):

| Regla | Resultado |
|---|---|
| Un ADMIN_EMPRESA crea un usuario sin indicar empresa | Va a **su** empresa |
| Un ADMIN_EMPRESA indica otra empresa | 403 |
| Un ADMIN_EMPRESA intenta crear un ADMIN o ascender a alguien a ADMIN | 403 |
| Un ADMIN_EMPRESA toca a un ADMIN o a alguien de otra empresa | 403 |
| Cualquiera cambia su propio rol | 403 |
| Rol ADMIN_EMPRESA sin empresa / rol ADMIN con empresa | 400 |
| La empresa está desactivada | 400 |
| Email ya registrado | 409 (se revisa **después** de los permisos) |

### Archivos

| Capa | Archivo |
|---|---|
| Dominio | `B/usuario/domain/model/Usuario` (`creadoPorAdmin`, `cambiarRol`; `rol` deja de ser `final`), `B/usuario/domain/service/PoliticaGestionUsuarios` (`empresaParaAlta`, `validarEmpresaActiva`, `validarCambioDeRol`), `B/shared/domain/model/Rol` (`desdeNombre`) |
| Casos de uso | `B/usuario/application/CrearUsuarioAdminUseCase`, `ComandoCrearUsuario`, `CambiarRolUsuarioUseCase` |
| Config | `B/usuario/infrastructure/config/UsuarioConfig` |
| Persistencia | `B/usuario/infrastructure/persistence/UsuarioPersistenceAdapter` (ahora guarda también el rol) |
| Web | `B/usuario/infrastructure/web/AdminUsuarioController`, `dto/CrearUsuarioRequest`, `dto/CambiarRolRequest` |
| Angular | `F/paginas/admin/usuarios/*` |
| Pruebas | `T/usuario/application/GestionUsuariosAdminTest` (16 pruebas) |

### Recorrido: un ADMIN_EMPRESA crea un usuario

1. **Angular.** Al pulsar "Nuevo usuario", `abrirFormulario()` muestra el formulario. El
   ADMIN_EMPRESA no ve el selector de empresa ni la opción ADMIN (`rolesAsignables`). Al enviar,
   `crear()` llama a `UsuariosService.crear(...)`, que arma `POST /api/admin/usuarios`.
2. **Controlador.** `AdminUsuarioController.crear` valida el DTO con `@Valid` y convierte el rol
   con `Rol.desdeNombre("USUARIO")`.
3. **Caso de uso.** `CrearUsuarioAdminUseCase.ejecutar`:
   1. `AlcanceDatos.administrativo`: un USUARIO recibe 403 antes de que se consulte nada.
   2. Si vino una empresa, la busca por uuid y obtiene su id interno.
   3. `PoliticaGestionUsuarios.empresaParaAlta(quien, rol, idPedida)` decide la empresa y los
      permisos.
   4. Si la empresa está desactivada, responde 400.
   5. `PoliticaContrasena.validar`, y después revisa el email repetido (409).
   6. `Usuario.creadoPorAdmin(...)`, con la contraseña hasheada por `PasswordHasherPort` (BCrypt).
   7. `usuarioRepository.guardar` y auditoría `USUARIO_CREADO`.
4. **Adaptador.** `UsuarioPersistenceAdapter.guardar` crea una `UsuarioEntity` nueva y JPA hace el
   `INSERT INTO usuarios`. Los CHECK de la base vuelven a exigir el email en minúsculas y un rol
   válido.

**Cambio de rol.** El selector de la fila llama a `cambiarRol(u, rol)`, que arma
`PATCH /api/admin/usuarios/{id}/rol`. Después, `CambiarRolUsuarioUseCase` llama a
`validarCambioDeRol`, luego a `usuario.cambiarRol(...)`, guarda (`UPDATE usuarios SET rol = …`) y
escribe la auditoría `USUARIO_ROL_CAMBIADO`.

El cambio rige **en la siguiente petición** del usuario: `JwtUsuarioActualConverter` relee el rol
de la base en cada petición. Se comprobó en vivo: con el mismo token, `/api/admin/panel` respondió
403, se ascendió al usuario, y la siguiente vez respondió 200.

---

## 7. Cómo verificar todo

```powershell
# Backend (dentro de backend/). contextLoads necesita la base levantada.
$env:DB_PASSWORD = (docker exec phishshield-db printenv POSTGRES_PASSWORD)
.\mvnw.cmd test                      # 221 pruebas

# Frontend (dentro de frontend/)
npm test -- --watch=false            # 46 pruebas
npm run build

# Regla 4 (desde la raíz): no debe imprimir nada
git grep -nE "import (org\.springframework|jakarta\.persistence)" -- "backend/src/main/java/**/domain/**"
```

## 8. Preguntas probables y dónde está la respuesta

- **"¿Dónde se valida que una alerta no pase de pendiente a revisada?"** En
  `EstadoAlerta.puedePasarA` y `AlertaGestionada.cambiarEstado` (dominio). El controlador no lo
  sabe. Prueba: `AlertaGestionadaTest`.
- **"¿Cómo evitan que un admin de empresa vea datos de otra?"** Con `AlcanceDatos`. En los
  listados, el filtro va en el SQL (`FiltrosSql.DE_LA_EMPRESA`); en el detalle o al modificar, el
  caso de uso usa `alcance.incluye(...)` y responde 404.
- **"¿Por qué uuid y no el id numérico?"** Un id numérico deja adivinar cuántas filas hay y probar
  ids vecinos. Por eso la V9 agregó `uuid_publico` a `alertas` y `dominios_lista`, y las entidades
  suplantadas se identifican por su nombre, que es único.
- **"¿Qué pasa si dos administradores cambian la misma alerta a la vez?"** El `UPDATE … WHERE
  estado = :leido` hace que el segundo reciba 409 (`AlertaModificadaException`).
- **"¿Eliminar un dominio lo borra?"** No: pone `activo = false`. No existe un endpoint `DELETE`.
- **"¿Por qué SQL directo (JdbcClient) y no JPA en los módulos nuevos?"** Son consultas de lectura
  con filtros opcionales, `JOIN`, columnas JSONB e INET, y paginación. Con SQL directo se ve
  exactamente qué se ejecuta. Además, Hibernate (`ddl-auto: validate`) no depende de la columna de
  la V9.
