# PhishShield Bolivia

Plataforma de detección, prevención y educación contra el phishing y el fraude
digital, basada en inteligencia artificial y adaptada al contexto boliviano.

Proyecto universitario — La Paz, Bolivia, 2026.

---

## Estado del proyecto

| Hito | Contenido | Estado |
|---|---|---|
| Hito 1 | Documento de avance: problema, objetivos, alcance | Completado |
| Hito 2 | Arquitectura definida + diseño de base de datos | Base de datos lista; arquitectura pendiente |
| Hito 3 | Primera presentación funcional (backend + frontend + IA base) | Pendiente |
| Hito 4 | Sistema integrado listo para demo | Pendiente |

---

## Estructura del repositorio

```
phishshield-bolivia/
├── database/        Esquema PostgreSQL y entorno Docker (listo)
├── backend/         API REST con arquitectura hexagonal (pendiente)
├── frontend/        Aplicación Angular (pendiente)
├── ia/              Modelos de detección en Python (pendiente)
└── docs/            Documento de avance, cronograma, diagramas
```

---

## Cómo levantar el entorno

Requisito: Docker Desktop instalado y en ejecución.

```bash
cd database
docker compose up -d
```

Esto levanta PostgreSQL 18 en el puerto 5432 y pgAdmin en el 5050, con el
esquema y los datos de ejemplo ya cargados. El detalle completo, incluidas las
credenciales y la resolución de problemas frecuentes, está en
[`database/README.md`](database/README.md).

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Spring Boot 3.x o Django 5.x, arquitectura hexagonal |
| Base de datos | PostgreSQL 18 |
| Seguridad | JWT |
| Frontend | Angular 17+ con TypeScript |
| IA | Python 3.11+, TensorFlow / PyTorch / scikit-learn |
| Infraestructura | Docker y Docker Compose |

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
