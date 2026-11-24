# Matriz de trazabilidad — PhishShield Bolivia

Relaciona cada caso del Centro de Pruebas (`TC-*`) con la historia de usuario (`HU-*`) y el
requerimiento funcional (`RF-*`) o no funcional (`RNF-*`) que verifica.

> **Sobre la numeración.** En el repositorio no hay un backlog con números de historia; el único
> número conocido es **HU-17 (Generar reportes por empresa)**. Las demás historias (HU-01 a HU-10)
> son una propuesta derivada del sistema tal como está construido: si el backlog oficial del curso
> usa otros números, basta con reemplazarlos en las tablas de abajo. Los RF/RNF se definieron a
> partir del código y del catálogo de casos.

## 1. Historias de usuario

| HU | Como... quiero... para... |
|---|---|
| HU-01 | Como usuario registrado, quiero iniciar sesión con mi correo y contraseña, para usar las funciones de mi rol |
| HU-02 | Como usuario, quiero pegar un enlace y saber si es seguro, sospechoso o phishing, para no caer en una estafa |
| HU-03 | Como administrador, quiero ver un panel con los análisis, el phishing detectado y las alertas abiertas, para conocer la situación de un vistazo |
| HU-04 | Como administrador de la plataforma, quiero crear, editar y desactivar empresas cliente, para gestionar a quién presto el servicio |
| HU-05 | Como administrador, quiero consultar el historial de análisis con filtros, para revisar qué se analizó y con qué resultado |
| HU-06 | Como administrador, quiero que cada análisis no seguro abra una alerta que pueda revisar, para reaccionar a tiempo |
| HU-07 | Como administrador, quiero que las rutas administrativas exijan sesión y rol, para que nadie vea datos ajenos |
| HU-08 | Como dueño del producto, quiero que el detector use un modelo de IA además de las reglas, para detectar imitaciones nuevas |
| HU-09 | Como responsable de la plataforma, quiero que el análisis responda rápido y estable aunque muchos usuarios lo usen a la vez, para dar un buen servicio |
| HU-10 | Como responsable de calidad, quiero medir qué tan bien detecta el sistema el phishing boliviano, para saber cuánto confiar en él |
| HU-17 | Como administrador, quiero generar reportes de una empresa para un período (análisis, phishing y alertas), para informar al cliente |

## 2. Requerimientos

| Código | Requerimiento |
|---|---|
| RF-01 | Autenticación con correo y contraseña; emisión de token de acceso y de refresco; el rol viaja en la sesión |
| RF-02 | Análisis de una URL con el motor de reglas: veredicto seguro / sospechoso / phishing según umbrales 40 y 70 |
| RF-03 | Integración con el servicio de IA: su probabilidad suma como indicador `IA_MODELO_URL` |
| RF-04 | Persistencia de cada análisis con sus indicadores y consulta del historial con filtros (resultado, texto, fechas) |
| RF-05 | Creación automática de una alerta pendiente por cada análisis no seguro |
| RF-06 | Gestión de empresas: crear, listar, editar y desactivar (el sistema no borra empresas) |
| RF-07 | Panel de indicadores (totales, phishing, alertas abiertas, usuarios activos) |
| RF-08 | Control de acceso: sin sesión, las rutas administrativas responden 401; sin el rol, 403 |
| RF-09 | Validación de entradas: URL vacía, sin esquema o malformada se rechaza con un mensaje claro |
| RF-10 | Reportes por empresa y período, con totales calculados desde la base, listado y descarga en PDF |
| RNF-01 | Rendimiento: con 20 usuarios concurrentes, p95 < 2000 ms y tasa de error < 1 % |
| RNF-02 | Capacidad: identificar el punto de degradación y recuperarse al bajar la carga |
| RNF-03 | Estabilidad: con carga constante, el p95 no crece más de 50 % y la tasa de error es < 1 % |
| RNF-04 | Calidad del detector: precisión y recall ≥ 0,80 sobre el lote boliviano etiquetado |
| RNF-05 | Usabilidad: los errores de validación y de credenciales se informan en pantalla sin perder el contexto |
| RNF-06 | Seguridad de la evidencia: contraseñas y tokens nunca se guardan en logs ni reportes |

## 3. Matriz caso ↔ historia ↔ requerimiento

| Caso | Tipo / suite | Nombre | HU | RF / RNF | Último resultado (2026-11-24, 11:50) |
|---|---|---|---|---|---|
| TC-SMK-001 | Funcional / Smoke | La API del backend responde | HU-07 | RF-08 | PASS |
| TC-SMK-002 | Funcional / Smoke | Login del administrador de prueba por API | HU-01 | RF-01, RNF-06 | PASS |
| TC-SMK-003 | Funcional / Smoke | El listado de empresas está disponible | HU-04 | RF-06 | PASS |
| TC-SMK-004 | Funcional / Smoke | Los datos del panel están disponibles | HU-03 | RF-07 | PASS |
| TC-SMK-005 | Funcional / Smoke | El login en la interfaz llega al panel | HU-01 | RF-01 | PASS |
| TC-SMK-006 | Funcional / Smoke | La pantalla de análisis carga | HU-02 | RF-02 | PASS |
| TC-SAN-001 | Funcional / Sanity | URL oficial de banco boliviano se califica como segura | HU-02 | RF-02 | PASS |
| TC-SAN-002 | Funcional / Sanity | Dominio que imita al BNB con extensión abusada se califica como phishing | HU-02 | RF-02 | PASS |
| TC-SAN-003 | Funcional / Sanity | Typosquatting de BancoSol con I mayúscula se califica como phishing | HU-02 | RF-02 | PASS (antes FAIL → DEF-001, corregido con V16) |
| TC-REG-001 | Funcional / Regression | URL vacía muestra mensaje de validación | HU-02 | RF-09, RNF-05 | PASS |
| TC-REG-002 | Funcional / Regression | URL sin http:// muestra mensaje de validación | HU-02 | RF-09, RNF-05 | PASS |
| TC-REG-003 | Funcional / Regression | La API rechaza una URL malformada | HU-02 | RF-09 | PASS |
| TC-REG-004 | Funcional / Regression | Credenciales inválidas muestran mensaje de error | HU-01 | RF-01, RNF-05 | PASS |
| TC-REG-005 | Funcional / Regression | Gestión de empresas: crear, consultar, editar y desactivar | HU-04 | RF-06 | PASS |
| TC-REG-006 | Funcional / Regression | Filtro del historial por resultado | HU-05 | RF-04 | PASS |
| TC-REG-007 | Funcional / Regression | Filtro del historial por texto en la interfaz | HU-05 | RF-04 | PASS |
| TC-REG-008 | Funcional / Regression | Las rutas administrativas sin token devuelven 401 | HU-07 | RF-08 | PASS |
| TC-REG-009 | Funcional / Regression | Generar el reporte de una empresa por API | HU-17 | RF-10 | PASS |
| TC-INT-001 | Funcional / Integration | Integración del backend con el servicio de IA | HU-08 | RF-03 | PASS |
| TC-INT-002 | Funcional / Integration | El análisis queda guardado en PostgreSQL | HU-05 | RF-04 | PASS |
| TC-INT-003 | Funcional / Integration | Un análisis no seguro genera su alerta | HU-06 | RF-05 | PASS |
| TC-E2E-001 | Funcional / E2E | Flujo completo: login, análisis, historial y alerta | HU-01, HU-02, HU-05, HU-06 | RF-01, RF-02, RF-04, RF-05 | PASS |
| TC-PERF-001 | No funcional / Performance | Carga con URL nuevas (motor completo) | HU-09 | RNF-01 | PASS |
| TC-PERF-002 | No funcional / Performance | Carga con URL repetidas (caché) | HU-09 | RNF-01 | PASS |
| TC-STR-001 | No funcional / Estrés | Estrés por escalones | HU-09 | RNF-02 | PASS |
| TC-SOAK-001 | No funcional / Soak | Resistencia con carga constante | HU-09 | RNF-03 | PASS |
| TC-ROB-001 | No funcional / Robustez IA | Precisión y recall del sistema completo sobre el lote boliviano | HU-10 | RNF-04 | PASS |
| TC-ROB-002 | No funcional / Robustez IA | Precisión y recall del modelo de IA por sí solo | HU-08, HU-10 | RNF-04 | **FAIL → DEF-002** |

## 4. Cobertura de requerimientos

| Requerimiento | Casos | ¿Cubierto? |
|---|---|---|
| RF-01 | TC-SMK-002, TC-SMK-005, TC-REG-004, TC-E2E-001 | Sí |
| RF-02 | TC-SMK-006, TC-SAN-001, TC-SAN-002, TC-SAN-003, TC-E2E-001 | Sí (DEF-001 corregido y verificado) |
| RF-03 | TC-INT-001 | Sí |
| RF-04 | TC-REG-006, TC-REG-007, TC-INT-002, TC-E2E-001 | Sí |
| RF-05 | TC-INT-003, TC-E2E-001 | Sí |
| RF-06 | TC-SMK-003, TC-REG-005 | Sí |
| RF-07 | TC-SMK-004 | Sí (solo disponibilidad) |
| RF-08 | TC-SMK-001, TC-REG-008 (y `SeguridadRutasTest` con 403 por rol) | Sí |
| RF-09 | TC-REG-001, TC-REG-002, TC-REG-003 | Sí |
| RF-10 | TC-REG-009 (y pruebas unitarias de la vertical `reporte`) | Sí |
| RNF-01 | TC-PERF-001, TC-PERF-002 | Sí |
| RNF-02 | TC-STR-001 | Sí |
| RNF-03 | TC-SOAK-001 | Sí |
| RNF-04 | TC-ROB-001, TC-ROB-002 | Sí (1 FAIL abierto del modelo solo: DEF-002) |
| RNF-05 | TC-REG-001, TC-REG-002, TC-REG-004 | Sí |
| RNF-06 | TC-SMK-002 y `JUnitEjecutorPruebasAdapterTest` (enmascarado) | Sí |

Historias sin caso en el catálogo: la gestión de usuarios, las listas de dominios y el registro
de accesos tienen pruebas unitarias de casos de uso (`GestionUsuariosAdminTest`, `ListasUseCaseTest`,
`ListarAuditoriaUseCaseTest`), pero ningún caso `TC-*`. Quedan como candidatos para ampliar el catálogo.
