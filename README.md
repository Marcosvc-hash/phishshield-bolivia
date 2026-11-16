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
| Hito 3 | Primera presentación funcional (backend + frontend + IA base) | En curso: análisis de URL por reglas y modelo de IA, autenticación con JWT y panel de administración funcionando |
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
├── ia/              Modelo de detección de URL en Python (scikit-learn) y servicio de predicción (FastAPI)
└── docs/            Guía de la demo, guía del módulo de administración e informe del modelo de IA
```

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Java 25, Spring Boot 4.1 (Web MVC, Data JPA, Validation), Maven Wrapper |
| Base de datos | PostgreSQL 18 (Docker), pgAdmin 4 |
| Frontend | Angular 22, TypeScript 6, pruebas con Vitest |
| Seguridad | Spring Security + JWT (HS256), contraseñas con BCrypt |
| IA | Python 3.11+ con scikit-learn; servicio de predicción con FastAPI |
| Infraestructura | Docker y Docker Compose |

---

## Cómo levantar el entorno

Requisitos: Docker Desktop en ejecución, JDK 25, Node.js con npm y Python 3.11 o
superior (para el servicio de IA).

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
Get-Content database\migraciones\V3__ajustar_peso_typosquatting.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
Get-Content database\migraciones\V4__ajuste_reglas_y_lista_blanca.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
```

Se aplican en orden y son idempotentes: ejecutarlas dos veces no hace daño.

### 3. Servicio de IA (puerto 8000)

El backend consulta a este servicio en cada análisis de URL. Si está apagado, el
análisis sigue funcionando solo con las reglas y el backend deja un `WARN` en su
consola.

El modelo entrenado (`ia/modelos/1.0.0/modelo.joblib`) y el dataset no están en el
repositorio: se generan la primera vez con `descargar_datos.py` y `entrenar.py`.

```powershell
cd ia
python -m venv .venv                                            # la primera vez
.\.venv\Scripts\python.exe -m pip install -r requirements.txt   # la primera vez
.\.venv\Scripts\python.exe descargar_datos.py                   # la primera vez: descarga el dataset
.\.venv\Scripts\python.exe entrenar.py                          # la primera vez: genera el modelo (varios minutos)
.\.venv\Scripts\python.exe -m uvicorn servicio:app --port 8000
```

Para comprobar que responde: `Invoke-RestMethod http://localhost:8000/salud` debe
devolver `estado = ok`. El detalle está en [`ia/README.md`](ia/README.md) y las
métricas del modelo en [`ia/FICHA_MODELO.md`](ia/FICHA_MODELO.md).

### 4. Backend (puerto 8080)

El backend necesita dos variables de entorno y no arranca si falta alguna:

- `DB_PASSWORD`: la contraseña de la base (el valor de `POSTGRES_PASSWORD` en
  `database/.env`). Nunca la escriba en `application.yaml`.
- `JWT_SECRET`: la clave con la que se firman los tokens de sesión (JWT). Debe
  tener al menos 32 caracteres y tampoco se escribe en ningún archivo del
  repositorio.

**PowerShell:**

```powershell
cd backend
$env:DB_PASSWORD = "su-contraseña"
# Clave aleatoria para firmar los tokens
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); $env:JWT_SECRET = [Convert]::ToBase64String($b)
.\mvnw.cmd spring-boot:run
```

Las variables valen solo para esa ventana de PowerShell. Como la clave se genera
de nuevo en cada ventana, las sesiones abiertas antes dejan de valer y hay que
volver a iniciar sesión. Las pruebas se ejecutan con `.\mvnw.cmd test` (la prueba
`contextLoads` también necesita la base y `DB_PASSWORD`; para `JWT_SECRET` usan
una clave falsa de prueba).

**IntelliJ IDEA:** *Run → Edit Configurations… → BackendApplication →
Environment variables* → `DB_PASSWORD=su-contraseña;JWT_SECRET=una-clave-de-al-menos-32-caracteres`.
Haga lo mismo con `DB_PASSWORD` en la plantilla de JUnit si va a ejecutar las
pruebas desde el IDE.

Hay peticiones de ejemplo en `backend/analisis.http` y `backend/empresas.http`
(se ejecutan desde IntelliJ).

### 5. Frontend (puerto 4200)

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
