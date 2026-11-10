# Preguntas probables en la defensa — módulo de IA

Cada respuesta indica dónde está la prueba en el repositorio. Las rutas del backend empiezan en
`backend/src/main/java/bo/phishshield/backend/analisis/` (abreviado como `analisis/`) y las de
pruebas en `backend/src/test/java/bo/phishshield/backend/analisis/`.

---

### 1. ¿Por qué Regresión Logística y no Random Forest?

Random Forest no lo probé. Probé HistGradientBoosting, que también es un modelo de árboles, y
quedó 0,0890 de F1 por debajo de la mejor Regresión Logística en validación (0,6452 frente a
0,7342). Dejé una regla escrita en el código: se prefiere la Regresión Logística salvo que el
otro modelo gane por 0,02 de F1 o más, y si eso pasa el script se detiene. La razón es que con
la Regresión Logística puedo explicar cada predicción de forma exacta, y eso es lo que muestra la
pantalla al usuario.

- `ia/entrenar.py`, líneas 46–47 (`PRECISION_MINIMA`, `MARGEN_F1_PREFERIR_LR`) y 258–277
  (entrena HistGradientBoosting y elige).
- `ia/reporte_metricas.md`, líneas 37–46 (tabla de comparación).

### 2. Si el modelo con la URL completa tenía F1 de 0,98, ¿por qué usaste uno de 0,73?

Porque el 0,98 viene de un sesgo del dataset: todas las URL legítimas de PhiUSIIL son portadas
sin ruta, así que el modelo aprende "con ruta = phishing". Lo medí con un experimento de control:
ese modelo marca como phishing las 10 páginas bolivianas legítimas con ruta; el mío, solo 2
(las dos de `bg.com.bo`). En el conjunto boliviano completo el control tiene precisión 0,6154 y
el mío 0,8235.

- `ia/entrenar.py`, líneas 199–212 (`control_url_completa`) y 333–347 (cómo se ejecuta).
- `ia/reporte_metricas.md`, líneas 152–162 (resultados del control).
- `ia/FICHA_MODELO.md`, líneas 36–73 (explicación completa).

### 3. ¿Dónde se llama al modelo desde el backend?

En el caso de uso `AnalizarUrlUseCase`, después de aplicar las reglas. Llama al puerto
`ModeloIaPort`, que es una interfaz del dominio; la implementación real es
`ModeloIaRestAdapter`, que hace `POST /predecir` al servicio de Python. La conexión se arma en
`AnalisisConfig` con `@Bean` (≈ `builder.Services.AddSingleton(...)` en .NET).

- `analisis/application/AnalizarUrlUseCase.java`, líneas 108–112 y 158–163 (`consultarIa`).
- `analisis/domain/port/out/ModeloIaPort.java`, línea 13.
- `analisis/infrastructure/ia/ModeloIaRestAdapter.java`, líneas 47–58.
- `analisis/infrastructure/config/AnalisisConfig.java`, líneas 46–67.
- `ia/servicio.py`, líneas 103–124 (el endpoint en Python).

### 4. ¿Qué pasa si el servicio de IA está caído?

El análisis sigue solo con las reglas. El adaptador tiene un timeout de 2 segundos; cualquier
error (servicio apagado, colgado o respuesta incompleta) se registra con `log.warn` y devuelve
`Optional.empty()` (≈ un `T?` nulo que obliga a revisar si viene vacío). El caso de uso no suma
el indicador y guarda el análisis sin modelo.

- `analisis/infrastructure/ia/ModeloIaRestAdapter.java`, líneas 59–62 y 69–72.
- `backend/src/main/resources/application.yaml`, línea 25 (`timeout: 2s`).
- Pruebas: `infrastructure/ia/ModeloIaRestAdapterTest.java`, líneas 20 (servicio apagado) y 33
  (servicio colgado), y `application/AnalizarUrlUseCaseTest.java`, línea 584
  (`con_ia_caida_el_analisis_sigue_solo_con_las_reglas`: queda en 63, sospechoso).

### 5. Muéstrame en el código de dónde sale la señal "tiene 5 subdominios".

1. Se cuenta en `ia/caracteristicas.py`, línea 133 (`subdominios = ...`).
2. Su aporte es coeficiente × valor: `ia/modelo.py`, línea 79 (`aportes = coef[indices] * valores`).
3. Se ordena por aporte en la dirección del veredicto: `ia/modelo.py`, líneas 93–95.
4. Se convierte en frase con `describir()`: `ia/caracteristicas.py`, líneas 181–182.
5. El backend se queda con las que van hacia phishing: `ModeloIaRestAdapter.java`, líneas 63–66.
6. `IndicadorIa.evidencia()` arma el texto con hasta 3 señales y lo recorta a 255 caracteres:
   `analisis/domain/service/IndicadorIa.java`, líneas 43–55.
7. Angular lo muestra: `frontend/src/app/analizador/analizador.html`, líneas 49–53.

Para `http://bnb.com.bo.verificar.cuenta.segura-login.xyz/` esa señal aporta 16,538 en logit
(`ia/README.md`, línea 82).

### 6. ¿Cómo se combina la IA con las reglas? ¿La IA puede bajar el puntaje?

No puede bajarlo. `IndicadorIa` solo crea el indicador si la probabilidad alcanza el umbral del
modelo (0,5527), y su aporte es probabilidad × peso, con el peso 40 leído de
`reglas_deteccion`. Por debajo del umbral no suma ni resta. Después `Analisis.deUrl()` suma todos
los aportes y los limita entre 0 y 100, y `Resultado` decide: sospechoso desde 40, phishing
desde 70.

- `analisis/domain/service/IndicadorIa.java`, líneas 29–37.
- `analisis/domain/model/PrediccionIa.java`, líneas 28–30 (`superaUmbral`).
- `analisis/domain/model/Analisis.java`, líneas 61–66.
- `analisis/domain/model/Resultado.java`, líneas 11–12.
- `database/initdb/02_seed.sql`, línea 60 (peso 40).
- Pruebas: `AnalizarUrlUseCaseTest.java`, líneas 545 (0,90 × 40 = 36), 559 (typosquatting 45 +
  IA 32 = 77) y 572 (bajo el umbral no suma).

### 7. ¿De dónde sale el umbral 0,5527 y por qué el peso 40?

El umbral lo calcula el script: es el que da el mayor recall manteniendo una precisión de al
menos 0,90, en una mitad de la validación que no se usó para calibrar. Ahí dio precisión 0,9001
y recall 0,6007. El peso 40 lo comparé con 30 y con 0 usando el motor real: de las 18 URL de
phishing bolivianas, salen como phishing 4 sin IA, 13 con peso 30 y 14 con peso 40, y los 36
enlaces legítimos fuera de la lista blanca siguen seguros con cualquier peso.

- `ia/entrenar.py`, líneas 183–194 (`elegir_umbral`) y 294.
- `ia/reporte_metricas.md`, líneas 54–58.
- `ia/datos_evaluacion/puntajes_peso_ia.md`, sección "Conclusiones".

### 8. ¿Cómo evitaste que el modelo hiciera trampa con dominios repetidos?

Dividí los datos por dominio registrable, no por fila: todos los hosts de `web.app` caen en la
misma partición. Así la prueba mide dominios que el modelo nunca vio. El script lo verifica con
`assert`. Además dejé un solo registro por host y quité 58 hosts con etiquetas contradictorias.

- `ia/entrenar.py`, líneas 82–91 (limpieza), 99–111 (`dividir_por_grupo`) y 235–236 (`assert`).
- `ia/reporte_metricas.md`, líneas 25–35.

### 9. ¿Qué tan bueno es el modelo, honestamente?

En prueba: exactitud 0,8955, precisión 0,9610, recall 0,8057 y F1 0,8766. Pero esas cifras están
infladas porque `web.app` (5 527 hosts) cayó entero en prueba. Si cada dominio pesa lo mismo, el
recall baja a 0,4839 y el F1 a 0,6186. Es decir: atrapa más o menos la mitad del phishing nuevo,
pero cuando avisa casi siempre tiene razón. Por eso es un indicador más y no decide solo. La meta
de exactitud del 85 % se cumple por fila (89,55 %) y por dominio (85,39 %), esta última justo en
el límite.

- `ia/reporte_metricas.md`, líneas 60–78.
- `ia/FICHA_MODELO.md`, líneas 147–168.

### 10. ¿Por qué no se consulta la IA para IP ni para dominios de la lista blanca?

Para IP: en el dataset las 608 URL con IP son todas phishing, así que el modelo le daría casi 1,0
hasta a `192.168.1.1`; ese caso lo maneja la regla `URL_IP_LITERAL`. Para la lista blanca: el
modelo conoce muy pocos dominios `.bo` (24 en todo el dataset) y castiga los `.com.bo`
legítimos (`bnb.com.bo` da 0,5318). La comparación es exacta por dominio registrable, así que
`bnb.com.bo.verificar.cuenta.segura-login.xyz` sí se consulta.

- `analisis/application/AnalizarUrlUseCase.java`, líneas 147–163 (comentario y `consultarIa`).
- `ia/FICHA_MODELO.md`, líneas 187–196.
- Pruebas: `AnalizarUrlUseCaseTest.java`, líneas 623 (IP), 635 (lista blanca) y 651 (oficial
  usado como subdominio).

---

**Nota sobre las líneas de `AnalizarUrlUseCaseTest.java`:** son las de la copia de trabajo
actual, que tiene un cambio sin commitear (se borró una línea en blanco cerca de la línea 259).
Si ese cambio se descarta, las pruebas citadas bajan una línea (545 → 546, etc.).
