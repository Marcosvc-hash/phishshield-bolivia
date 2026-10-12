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
- **Lista blanca:** revisar `ende.bo`, que typosquatting confunde con `entel.bo`.
