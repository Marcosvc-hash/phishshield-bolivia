# Plan del módulo de IA — versión 2

Parte de lo que se midió en la v1 (`ia/FICHA_MODELO.md`, `ia/reporte_metricas.md`). Está
ordenado por impacto esperado.

## 1. Datos con contexto boliviano y legítimas con ruta

**Problema medido:**
- PhiUSIIL tiene 24 dominios `.bo` en 219 036 hosts.
- Las legítimas nunca tienen ruta, así que el modelo no puede usar la ruta.
- Las 608 IP son todas phishing.

**Mejora:**
- Sumar URL legítimas reales con ruta: enlaces internos de sitios populares y de los listados
  de Tranco, recorridos con permiso.
- Sumar dominios `.bo` legítimos (universidades, gobierno, medios) y phishing local de
  PhishTank y OpenPhish filtrado por marcas bolivianas.
- Con eso se puede volver a incluir la ruta en el modelo y repetir el experimento de control
  para comprobar que el sesgo desapareció.

**Además:** tratar el sufijo público como un dato aparte del resto del host. Así `.com.bo`
deja de parecerse a `paypal.com.seguro.xyz`, que es la causa del falso positivo de
`bg.com.bo`.

## 2. Contenido de la página

**Por qué:** el mayor límite de la v1 es que solo ve el texto de la URL. Un dominio de phishing
nuevo y con un nombre neutro (`pagos-cliente.com`) no tiene señales léxicas. El recall por
dominio nuevo es de 0,48 a 0,62.

**Mejora:** un servicio aparte, aislado y con timeout, que descargue la página una sola vez.
De ahí se sacan características como formularios que piden contraseña, el destino de esos
formularios, el título frente al dominio, logos de marcas bolivianas y la cantidad de enlaces
externos. PhiUSIIL ya trae varias de estas columnas, así que sirve para empezar.

**Cuidado:** descargar una página maliciosa es un riesgo. Hay que hacerlo en un entorno aislado
y sin ejecutar JavaScript.

## 3. Antigüedad del dominio

**Por qué:** la mayoría de los dominios de phishing tiene días de vida. La regla
`URL_DOMINIO_NUEVO` ya existe en la base, pero no está implementada.

**Mejora:** consultar RDAP (el reemplazo de WHOIS) con caché, implementar la regla y usar la
antigüedad como característica del modelo. Para el `.bo` (NIC Bolivia) hay que comprobar
primero si ofrece RDAP.

## 4. Reentrenamiento con `feedback_analisis`

**Por qué:** las campañas cambian y el modelo envejece. Los usuarios ya marcan falsos
positivos y negativos en `feedback_analisis`.

**Mejora:**
- Un script mensual exporta el feedback revisado por un analista. Nunca debe entrar sin
  revisión, para que nadie pueda envenenar el modelo a propósito.
- Ese feedback se suma al entrenamiento y se reentrena con la misma división por dominio.
- La nueva versión se publica solo si mejora en prueba **y** en el conjunto boliviano.
- Se registra como una fila nueva de `modelos_ia` con una migración, igual que V5.

`analisis.id_modelo` permite comparar versiones con datos reales.

## 5. Docker

**Por qué:** hoy la demo necesita cuatro ventanas y un entorno de Python armado a mano.

**Mejora:**
- Un `Dockerfile` para `ia/`: imagen de Python fija, `requirements.txt` y el modelo copiado,
  o descargado de un almacenamiento de artefactos con su hash.
- Sumarlo al `docker-compose.yml` con un healthcheck en `/salud`.
- El backend lo encuentra con `IA_URL=http://ia:8000`.
- Aprovechar para rotar la contraseña de la base, que hoy es pública.

## 6. Mejoras menores

- **Más modelos:** probar modelos de caracteres más ricos (por ejemplo, un modelo lineal con más
  n-gramas o un CNN pequeño). Se mantendría la regla de preferir el modelo explicable salvo que
  el otro gane por 2 puntos de F1.
- **Más variación en las métricas:** validación cruzada agrupada por dominio en entrenamiento.
- **Caché:** que no se cachee un análisis hecho con la IA caída, o que dure menos.
- **Lista blanca ampliada (pospuesta):** cargar en la lista blanca sitios bolivianos legítimos
  ya verificados (universidades, ministerios, medios, empresas estatales), para que
  typosquatting no repita el caso de `ende.bo` (resuelto en V6).
  **Advertencia:** los 18 sitios de `ia/datos_evaluacion/bolivia_legitimos_fuera_lista_blanca.csv`
  son el conjunto que mide los falsos positivos. Si entran a la lista blanca, no pasan por las
  reglas ni por la IA y la medición deja de servir. Si se hace, hay que usar sitios distintos o
  armar antes otro conjunto de medición que quede fuera de la lista blanca.

## 7. Pendientes de las reglas (fuera del modelo)

- **Entidades de servicios básicos en `entidades_suplantadas`.** ENDE va en
  `feature/entidad-ende` (V7). Quedan pendientes, con los sitios verificados el 2026-10-12. La
  naturaleza de cada una hay que confirmarla antes de cargarla:

  | Servicio | Entidad | Dominio | Naturaleza (a confirmar) |
  |---|---|---|---|
  | Luz | DELAPAZ | `delapaz.bo` | Distribuidora del grupo ENDE |
  | Luz | ELFEC | `elfec.bo` | Distribuidora del grupo ENDE |
  | Luz | CRE | `cre.com.bo` | Cooperativa |
  | Agua | EPSAS | `epsas.com.bo` | Estatal |
  | Agua | SEMAPA | `semapa.gob.bo` | Municipal |
  | Agua | ELAPAS | `elapas.com.bo` | Municipal |
  | Agua | SAGUAPAC | `saguapac.com.bo` | Cooperativa |
  | Gas | YPFB | `ypfb.gob.bo` | Estatal |

  Advertencias:
  - **Medir cada una antes de cargarla**, con la misma prueba temporal que se usó para ENDE:
    los 18 sitios, `bolivia.csv` y los 219 036 hosts de PhiUSIIL. Al agregar una entidad,
    typosquatting empieza a comparar todos los dominios contra su marca.
  - **CRE:** la marca `cre` tiene 3 letras. Cualquier dominio con la palabra suelta `cre`
    (`cre-...`, `...-cre.com`) se marcaría, y los dominios `.com.bo` cortos quedan a
    distancia 2 o menos de `cre.com.bo` con facilidad. Riesgo alto de falsos positivos.
  - **Conjunto de medición:** cada `dominio_oficial` entra solo a la lista blanca (así lo hace
    `02_seed.sql`). `ypfb.gob.bo` está en el conjunto de medición de falsos positivos; si YPFB
    entra como entidad, hay que reemplazarlo en ese conjunto igual que se hizo con `ende.bo`.
  - **Tipo:** el `CHECK` de `entidades_suplantadas.tipo` no tiene un tipo para servicios
    básicos. Para las públicas sirve `gobierno` (como ENDE); para las cooperativas habría que
    usar `otro` o migrar el esquema.
- **Revisar los 49 falsos positivos de typosquatting en PhiUSIIL.** Con los dominios oficiales
  actuales, `URL_TYPOSQUATTING` marca 49 hosts **legítimos** de PhiUSIIL (y 26 de phishing).
  Medido el 2026-10-12 con una prueba temporal sobre los 219 036 hosts. Hay que listarlos,
  ver qué caso de la regla los dispara (distancia del dominio completo, marca exacta o marca
  mal escrita) y decidir entre ajustar ese caso o aceptarlos. Cuidado: bajar la tolerancia
  sin más deja pasar ataques reales como `bmcs.com.bo` (ver la prueba
  `bmcs_sigue_siendo_sospechoso_por_typosquatting`).
