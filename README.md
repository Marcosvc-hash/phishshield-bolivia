# PhishShield Bolivia

Plataforma de detección y prevención contra el phishing y el fraude
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
├── .github/workflows/ci.yml   Integración continua: pruebas del backend, frontend e IA en cada push
├── database/        PostgreSQL 18 en Docker
│   ├── initdb/        Esquema, datos de ejemplo, vistas del panel y catálogo QA (01 a 04)
│   └── migraciones/   Cambios para bases ya creadas (V2__... a V15__...)
├── backend/         API REST Spring Boot, arquitectura hexagonal por verticales
│   ├── src/main/java/bo/phishshield/backend/
│   │   ├── analisis/    Motor de reglas para URL e integración con el modelo de IA
│   │   ├── usuario/     Registro, login con JWT y gestión de usuarios
│   │   ├── empresa/     Empresas cliente
│   │   ├── panel/       Datos del panel de administración
│   │   ├── historial/   Historial de análisis con filtros
│   │   ├── alerta/      Bandeja de alertas
│   │   ├── listas/      Listas blanca, negra y gris de dominios
│   │   ├── auditoria/   Registro de accesos y acciones administrativas
│   │   ├── reporte/     Reportes por empresa y período (HU-17), con descarga en PDF
│   │   ├── qa/          Centro de Pruebas QA: ejecutor, casos, reportes, correo y asistente de IA
│   │   └── shared/      Seguridad, paginación y manejo global de errores
│   ├── herramientas/  Scripts: levantar el backend contra la base de pruebas, crear el usuario QA, hash BCrypt
│   └── *.http         Peticiones de ejemplo (analisis, auth, empresas, reportes, qa)
├── frontend/        Aplicación Angular: analizador, panel de administración y Centro de Pruebas
├── ia/              Modelo de detección de URL en Python (scikit-learn) y servicio de predicción (FastAPI)
└── docs/            Guías, informe del modelo de IA y documentación de pruebas (docs/qa/)
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
| Pruebas automatizadas | JUnit 6 (Platform Launcher), Rest Assured, Playwright (Chromium), generador de carga propio, Vitest, pytest |
| Reportes y calidad | OpenPDF (reportes PDF), JaCoCo (cobertura), GitHub Actions (CI) |
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

Se aplican en orden y son idempotentes: ejecutarlas dos veces no hace daño. La
lista completa (hasta V15) y el comando de cada una están en
[`database/README.md`](database/README.md).

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

Hay peticiones de ejemplo en `backend/analisis.http`, `backend/auth.http`,
`backend/empresas.http`, `backend/reportes.http` y `backend/qa.http` (se ejecutan desde IntelliJ).

### 5. Frontend (puerto 4200)

```powershell
cd frontend
npm install        # la primera vez
npm start          # http://localhost:4200
npm test           # pruebas con Vitest
npm run build      # compilación de producción
```

---

## Centro de Pruebas QA

Pantalla `/admin/qa` (solo ADMIN) que ejecuta **pruebas reales** contra el propio
PhishShield y guarda cada ejecución con sus pasos, evidencias, capturas y métricas.
28 casos clasificados por tipo (funcional / no funcional) y suite:

| Suite | Casos | Herramienta |
|---|---|---|
| Smoke, Sanity, Regression, Integration, E2E | 22 | Rest Assured (API) y Playwright (interfaz) |
| Performance, Estrés, Soak | 4 | Generador de carga propio (HttpClient + hilos virtuales) |
| Robustez IA | 2 | Lote boliviano etiquetado: precisión, recall, F1 y matriz de confusión |

Al terminar cada ejecución se generan un **reporte PDF detallado** y un **resumen HTML**,
que se descargan desde la pantalla y, si hay SMTP configurado, se envían por correo.

**Cómo usarlo** (todo contra la base de pruebas `phishshield_qa`, nunca la real):

1. Levantar la base, el servicio de IA y el frontend (pasos 1, 3 y 5; el frontend con
   `$env:PORT = "4200"` antes de `npm start`).
2. La primera vez: `backend\herramientas\crear-usuario-qa.ps1` (crea el administrador de
   prueba en `phishshield_qa` y guarda sus datos en `backend\.env`, ignorado por Git) y
   descargar Chromium para Playwright (ver `docs/qa/PROGRESO_QA.md`).
3. Levantar el backend contra la base de pruebas: `backend\herramientas\levantar-backend-qa.ps1`.
4. Entrar a `http://localhost:4200` con un administrador → **Centro de Pruebas**, elegir una
   suite o marcar casos y pulsar **Ejecutar**. El avance se ve en vivo; al final se
   descargan los reportes.

Variables (ver [`backend/.env.example`](backend/.env.example)): `QA_BASE_URL`,
`QA_FRONTEND_URL`, `QA_TEST_USER`, `QA_TEST_PASSWORD` (obligatorias para los casos);
`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` (correo, opcional) y
`ANTHROPIC_API_KEY` (asistente de IA, opcional; nunca decide PASS ni FAIL).

Documentación de pruebas en [`docs/qa/`](docs/qa/): plan de pruebas, matriz de
trazabilidad, registro de defectos, informe de ejecución y bitácora del módulo.

## Calidad

- **Integración continua** (`.github/workflows/ci.yml`): en cada push y pull request corren
  las pruebas del backend (sin `contextLoads`, que necesita PostgreSQL), las del frontend y
  su build, y las del modelo de IA.
- **Cobertura** con JaCoCo: `.\mvnw.cmd test` deja el reporte en
  `backend/target/site/jacoco/index.html` (63,9 % de líneas y 60,6 % de ramas al 24/11/2026;
  90,5 % en el dominio y 88,5 % en los casos de uso).

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
| Marcos Javier Vargas Callizaya | Desarrollo completo (backend, frontend, base de datos e IA) |

Docente: Ing. Gabriel Alejandro Choque Callizaya
