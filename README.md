# PhishShield Bolivia

Plataforma de detección, prevención y educación contra el phishing y el fraude
digital, basada en inteligencia artificial y adaptada al contexto boliviano.

Proyecto universitario — La Paz, Bolivia, 2026.

---

## Estado del proyecto

| Hito | Contenido | Estado |
|---|---|---|
| Hito 1 | Documento de avance: problema, objetivos, alcance | Completado |
| Hito 2 | Arquitectura definida + diseño de base de datos | Completado |
| Hito 3 | Primera presentación funcional (backend + frontend + IA base) | En curso: análisis de URL por reglas, empresas y usuarios funcionando; todavía sin IA ni autenticación |
| Hito 4 | Sistema integrado listo para demo | Pendiente |

---

## Estructura del repositorio

```
phishshield-bolivia/
├── database/        PostgreSQL 18 en Docker
│   ├── initdb/        Esquema (01_schema.sql) y datos de ejemplo (02_seed.sql)
│   └── migraciones/   Cambios para bases ya creadas (V2__..., V3__...)
├── backend/         API REST Spring Boot, arquitectura hexagonal por verticales
│   └── src/main/java/bo/phishshield/backend/
│       ├── analisis/    Motor de reglas para URL (lista negra, typosquatting, ...)
│       ├── empresa/     Empresas cliente
│       ├── usuario/     Consulta de usuarios
│       └── shared/      CORS y manejo global de errores
├── frontend/        Aplicación Angular (analizador de URL + listado de empresas)
├── ia/              Modelos de detección en Python (pendiente, aún no existe)
└── docs/            Documento de avance, cronograma, diagramas (pendiente, aún no existe)
```

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Java 25, Spring Boot 4.1 (Web MVC, Data JPA, Validation), Maven Wrapper |
| Base de datos | PostgreSQL 18 (Docker), pgAdmin 4 |
| Frontend | Angular 22, TypeScript 6, pruebas con Vitest |
| Seguridad | Pendiente (planificado: Spring Security + JWT) |
| IA | Pendiente (planificado: Python 3.11+) |
| Infraestructura | Docker y Docker Compose |

---

## Cómo levantar el entorno

Requisitos: Docker Desktop en ejecución, JDK 25 y Node.js con npm.

### 1. Base de datos

```powershell
cd database
copy .env.example .env      # la primera vez; edite .env con sus contraseñas
docker compose up -d
```

Levanta PostgreSQL 18 en el puerto 5432 y pgAdmin en el 5050, con el esquema y
los datos de ejemplo. El detalle está en [`database/README.md`](database/README.md).

### 2. Aplicar migraciones (solo si su base ya existía)

Las bases creadas antes de un cambio de esquema o de datos necesitan las
migraciones de `database/migraciones/`. Desde la raíz del repositorio, en
PowerShell (que no admite `<`):

```powershell
Get-Content database\migraciones\V2__regla_lista_negra.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
```

La migración es idempotente: ejecutarla dos veces no hace daño.

### 3. Backend (puerto 8080)

El backend lee la contraseña de la base desde la variable de entorno
`DB_PASSWORD` (el valor de `POSTGRES_PASSWORD` en `database/.env`). Nunca la
escriba en `application.yml`.

**PowerShell:**

```powershell
cd backend
$env:DB_PASSWORD = "su-contraseña"
.\mvnw.cmd spring-boot:run
```

La variable vale solo para esa ventana de PowerShell. Las pruebas se ejecutan
con `.\mvnw.cmd test` (la prueba `contextLoads` también necesita la base y
`DB_PASSWORD`).

**IntelliJ IDEA:** *Run → Edit Configurations… → BackendApplication →
Environment variables* → `DB_PASSWORD=su-contraseña`. Haga lo mismo en la
plantilla de JUnit si va a ejecutar las pruebas desde el IDE.

Hay peticiones de ejemplo en `backend/analisis.http` y `backend/empresas.http`
(se ejecutan desde IntelliJ).

### 4. Frontend (puerto 4200)

```powershell
cd frontend
npm install        # la primera vez
npm start          # http://localhost:4200
npm test           # pruebas con Vitest
npm run build      # compilación de producción
```

---

## Metodología

Scrum, cinco sprints entre el 1 de septiembre y el 23 de noviembre de 2026,
alineados con los cuatro hitos de evaluación del curso.

---

## Convenciones de trabajo

- La rama `main` siempre debe quedar en un estado que levante sin errores.
- El trabajo se hace en ramas por funcionalidad: `feature/nombre-corto`.
- Los mensajes de commit se escriben en español y en imperativo, describiendo
  qué cambia: `Agregar validación de URL en el adaptador REST`.
- Ningún archivo con contraseñas o claves se sube al repositorio. Las
  credenciales locales van en `.env`, que está ignorado por Git.

---

## Equipo

| Integrante | Rol |
|---|---|
| _(completar)_ | _(completar)_ |

Docente: _(completar)_
