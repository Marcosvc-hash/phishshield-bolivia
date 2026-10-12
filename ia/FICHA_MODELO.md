# Ficha del modelo — phishshield-url-ia 1.0.0

Todas las cifras de esta ficha salen de `entrenar.py` y están completas en
[reporte_metricas.md](reporte_metricas.md) y en `modelos/1.0.0/metadata.json`.

## Para qué sirve

Estima la probabilidad de que una URL sea phishing **mirando solo su dominio (host)**
y explica la estimación con 3 a 5 señales en texto claro. En PhishShield es **una
señal más** dentro del motor de reglas del backend (indicador `IA_MODELO_URL`), no un
veredicto por sí sola.

No sirve para: juzgar el contenido de la página, la ruta, los parámetros ni el
protocolo de la URL; ni reemplazar la lista negra o la detección de typosquatting.

## Datos

- **PhiUSIIL Phishing URL Dataset** (Prasad y Chandra, 2024), repositorio UCI,
  licencia CC BY 4.0. https://archive.ics.uci.edu/dataset/967/phiusiil+phishing+url+dataset
- SHA-256 del CSV: `a236549cd369cd80bd478ff8e1779cbf44c58d5c3f79f7a51a1adbed7d06d1c6`.
- Etiqueta: según la documentación oficial, *"Label 1 corresponds to a legitimate URL,
  label 0 to a phishing URL"*. Se comprobó con ejemplos (p. ej. `metamask-usa-walet.godaddysites.com`
  tiene label 0) y se invirtió para que **1 = phishing**.
- Solo se usa la columna `URL`. Las otras 54 columnas no se usan porque varias salen de
  descargar la página, y en producción solo tenemos la URL.

### Limpieza

| Paso | Filas |
|---|---|
| Original | 235 795 (134 850 legítimas, 100 945 phishing) |
| Sin URL duplicadas | 235 370 |
| Sin hosts con etiqueta contradictoria (58 hosts) | 235 211 |
| Un registro por host | **219 036 (134 792 legítimas, 84 244 phishing; 38,5 % phishing)** |

### Sesgo del dataset y decisión de usar solo el host

En PhiUSIIL **todas** las URL legítimas tienen la forma `https://www.dominio.tld`, sin
ruta (medido: 100 % con https, 100 % con `www.`, 0 % con ruta). En cambio, el 60 % de las
de phishing tiene ruta y el 51 % no usa https. Un modelo con la URL completa aprende
"tiene ruta → phishing", no a reconocer phishing.

Por eso el modelo recibe **solo el host normalizado** (minúsculas, sin esquema, sin `www.`,
sin puerto). La ruta y el https quedan para las reglas del backend (`URL_RUTA_SENSIBLE`,
`URL_SIN_HTTPS`). No se decidió a ojo: hay un **experimento de control** que entrena la
misma Regresión Logística con la URL completa:

| | F1 validación | PR-AUC validación | Legítimas bolivianas con ruta marcadas como phishing |
|---|---|---|---|
| Control (URL completa) | 0.9822 | 0.9953 | **10 de 10** |
| Modelo elegido (solo host) | 0.7342 | 0.8426 | 2 de 10 (ambas de bg.com.bo, ver limitaciones) |

Las métricas casi perfectas del control vienen del sesgo, no de que el modelo funcione.

**Por qué se eligió el modelo solo con el dominio, aunque su F1 en validación sea menor:**

1. **El F1 de 0,982 del control no mide lo que creemos.** En validación, las legítimas
   tampoco tienen ruta, así que el control acierta usando la regla "con ruta → phishing",
   que solo vale dentro de este dataset. Cualquier métrica sobre datos con el mismo sesgo
   va a premiar ese atajo.
2. **Fuera del dataset, el atajo falla siempre.** Las 10 URL bolivianas legítimas con ruta
   real (por ejemplo `https://www.bancounion.com.bo/BancaDigital/UninetPlus`) son 10
   falsos positivos para el control. Con el modelo elegido son 2, y ambos vienen de
   `bg.com.bo`, que falla igual con o sin ruta. En PhishShield casi todas las URL que llegan
   (de correos, SMS o la extensión) tienen ruta, así que el control marcaría como phishing
   casi cualquier enlace legítimo.
3. **El conjunto boliviano completo lo confirma:** el control tiene precisión 0,615 y
   F1 0,727; el modelo solo con dominio, precisión 0,824 y F1 0,800.
4. **No se pierde la información de la ruta:** las reglas `URL_RUTA_SENSIBLE` y
   `URL_SIN_HTTPS` del backend ya la evalúan, con pesos que se pueden auditar.

Conclusión: entre una cifra alta que no se sostiene fuera del dataset y una más baja que
sí se sostiene, se eligió la segunda.

## División sin fuga de datos

- Se agrupa por **dominio registrable** (`tldextract`, lista de sufijos públicos incluida,
  sin red): `a.web.app` y `b.web.app` caen siempre en la misma partición. Así se mide la
  capacidad de reconocer **dominios nunca vistos**.
- 70 / 15 / 15 en filas: entrenamiento 153 283, validación 32 897, prueba 32 856.
  Se verifica con `assert` que ningún dominio está en dos particiones.
- Validación se divide a su vez en dos mitades, también por dominio: una para calibrar
  y otra para elegir el umbral.

## Método

**Características** (`caracteristicas.py`, sin red):

- Léxicas del host: largo, puntos, guiones, dígitos, caracteres especiales, subdominios,
  si es una IP, si el TLD es de los abusados (mismos que `ReglaTldSospechoso`), entropía
  de Shannon y palabras sensibles (login, verificar, cuenta, secure...).
- N-gramas de caracteres del host: TF-IDF `char_wb` de 3 a 5, máximo 20 000 términos
  (modelo de 0,3 MB).
- Calculadas también, pero fuera del modelo por el sesgo: largo de la URL y de la ruta,
  https y palabras sensibles en la ruta.

**Comparación en validación** (umbral 0,5, sin calibrar):

| Modelo | F1 | PR-AUC |
|---|---|---|
| Regresión Logística, léxicas + n-gramas, C = 0,3 | 0.7130 | 0.8375 |
| Regresión Logística, léxicas + n-gramas, C = 1 | 0.7315 | 0.8429 |
| **Regresión Logística, léxicas + n-gramas, C = 3** | **0.7342** | 0.8426 |
| HistGradientBoosting, léxicas | 0.6452 | 0.7500 |

Se eligió la Regresión Logística con C = 3: tiene el mejor F1. Su PR-AUC queda a 0,0003
de la mejor, y supera a HistGradientBoosting por 8,9 puntos de F1. Además, cada predicción
se puede explicar de forma exacta.

**Calibración:** Platt (sigmoide sobre el logit) en la mitad de calibración. El modelo ya
estaba bien calibrado: ECE 0,0168 → 0,0150; Brier 0,1074 → 0,1074.

**Umbral: 0,5527.** Criterio: el mayor recall con precisión ≥ 0,90 en la mitad de
validación reservada para el umbral. Ahí da precisión 0,900 y recall 0,601.
Interpretación: se acepta como mucho 1 falsa alarma de cada 10 avisos y, con esa
condición, se atrapa todo el phishing posible. Una precisión más baja haría que la IA
moleste en sitios legítimos; una más alta dejaría pasar más phishing.

**Explicabilidad:** en la Regresión Logística, el logit es la suma de coeficiente ×
valor de cada característica, así que el aporte de cada una es exacto. El detector
devuelve las de mayor aporte en la dirección del veredicto, como frases
("tiene 4 subdominios (bnb.com.bo.verificar.cuenta)", "la extensión .xyz se usa mucho en
phishing", "el dominio contiene 'login'"). Los n-gramas que se solapan cuentan como uno.

## Métricas en prueba (evaluación única)

| Exactitud | Precisión | Recall | F1 | ROC-AUC | PR-AUC |
|---|---|---|---|---|---|
| 0.8955 | 0.9610 | 0.8057 | 0.8766 | 0.9413 | 0.9512 |

Matriz de confusión (32 856 hosts): VN 17 239, FP 494, FN 2 938, VP 12 185.

**Cuidado al leer estas cifras.** Son mejores que las de validación porque `web.app`, con
5 527 hosts de phishing (37 % del phishing de prueba), cayó entero en prueba y se reconoce
casi siempre. Estos desgloses usan **las mismas predicciones**, sin cambiar el modelo ni el
umbral, y muestran el rendimiento sin ese efecto:

| Desglose de prueba | Exactitud | Precisión | Recall | F1 | ROC-AUC | PR-AUC |
|---|---|---|---|---|---|---|
| Cada dominio registrable pesa lo mismo | 0.8539 | 0.8570 | 0.4839 | 0.6186 | 0.8418 | 0.7424 |
| Sin web.app, workers.dev ni glitch.me (25 544 hosts) | 0.8657 | 0.9080 | 0.6240 | 0.7397 | 0.8866 | 0.8484 |

**Lectura honesta:** ante un dominio de phishing realmente nuevo, el modelo acierta cerca de
la mitad o algo más (recall de 0,48 a 0,62) y se equivoca poco cuando avisa (precisión de
0,86 a 0,91). Es un apoyo a las reglas, no un reemplazo.

### Exactitud frente a la meta del documento ("no inferior al 85 %")

Se recalculó el 2026-10-12 cargando `modelos/1.0.0/modelo.joblib`, **sin reentrenar**, sobre
el mismo conjunto de prueba (misma limpieza, semilla 42 y división por dominio) y con el umbral
0,5527. Coincide hasta el cuarto decimal con `metadata.json`.

| Exactitud en prueba | Modelo | Línea base "todo es legítimo" | ¿Cumple ≥ 85 %? |
|---|---|---|---|
| Por fila (32 856 hosts) | **89,55 %** | 53,97 % | Sí |
| Por dominio (22 708 dominios, cada uno pesa lo mismo) | **85,39 %** | 75,51 % | Sí, con 0,39 puntos de margen |

**Cómo leerla:**
- La meta se cumple en las dos mediciones, pero **por dominio queda justo en el límite**.
- La exactitud depende mucho de cuántas legítimas hay. Por dominio, el 75,5 % de los dominios
  de prueba son legítimos, así que un "modelo" que dijera siempre "legítimo" ya tendría 75,5 %
  sin detectar nada. Por eso la exactitud sola no alcanza: el modelo aporta 10 puntos sobre
  esa línea base, y su recall por dominio es 0,48.
- En el conjunto boliviano hecho a mano (44 URL, abajo), la exactitud del modelo solo es
  **84,09 %, por debajo del 85 %**. Es un conjunto pequeño (un acierto más o menos mueve 2,3
  puntos) y no es una estimación estadística, pero hay que decirlo.
- Para el documento se recomienda informar la exactitud **junto con** precisión, recall y la
  línea base, y aclarar que el 85 % se refiere al modelo de IA solo, sobre dominios no vistos.

## Evaluación boliviana (aparte)

`datos_evaluacion/bolivia.csv`: 44 URL etiquetadas a mano. Son 26 legítimas (los 9 dominios
oficiales de `entidades_suplantadas`, con y sin `www` y con rutas reales tomadas de sus
portadas) y 18 maliciosas (lista negra, variantes de marca, typosquatting y subdominios
engañosos). **Es un conjunto pequeño y construido a mano**: sirve como prueba de humo del
contexto local, no como estimación estadística.

| Exactitud | Precisión | Recall | F1 |
|---|---|---|---|
| 0.8409 | 0.8235 | 0.7778 | 0.8000 |

- Detecta las 3 URL de lista negra, las 6 variantes de marca y los 4 subdominios engañosos.
- **No detecta typosquatting** (1 de 5: solo `bnbb.com.bo`). Es esperable: `bancounlon.com.bo`
  tiene la misma forma que un dominio legítimo. Eso lo cubre `ReglaTyposquatting`.
- Falso positivo en `bg.com.bo` (probabilidad 0,63), en sus 3 variantes.

## Limitaciones

1. **Casi no conoce dominios bolivianos:** en los 219 036 hosts solo hay 24 `.bo`. Además, el
   n-grama `.com.` (típico de `paypal.com.seguro.xyz`) castiga a los `.com.bo` legítimos:
   `bnb.com.bo` da 0,53, cerca del umbral, y `bg.com.bo` lo supera.
2. **No ve la ruta, los parámetros ni el https** (decisión por el sesgo del dataset).
   **Tampoco sirve para IP literales:** las 608 URL con IP del dataset son todas phishing, así
   que el modelo da ~1,0 a cualquier IP, incluso `192.168.1.1`. El backend no consulta la IA
   para IP y deja ese caso a `URL_IP_LITERAL`. Tampoco la consulta para dominios de la lista
   blanca.
3. **No detecta typosquatting** de una o dos letras.
4. Las legítimas del dataset son portadas de sitios populares. Un sitio legítimo pequeño
   con un dominio largo o con guiones puede parecer sospechoso.
5. La probabilidad está calibrada para la proporción del dataset (~38 % de phishing). En
   el tráfico real el phishing es mucho más raro, así que el valor absoluto sobrestima el
   riesgo. Por eso en el backend es solo un indicador, con peso 40 sobre 100.
6. Los datos tienen fecha: las campañas cambian y el modelo debe reentrenarse
   (ver `docs/plan-ia-v2.md`).

## Reproducir

Ver [README.md](README.md): `descargar_datos.py` y luego `entrenar.py`, con semilla 42 y
versiones fijas en `requirements.txt`.

**Qué se reproduce y qué no.** Al volver a ejecutar `entrenar.py` salen idénticos el modelo
(mismo SHA-256 de `modelo.joblib`), todas las métricas, el umbral y la calibración. Esto se
verificó el 2026-10-12 comparando `metadata.json` valor por valor. Lo que **no** se repite son
los tiempos: los segundos de entrenamiento, `segundos_totales` y la latencia por URL dependen
de la carga de la máquina. La ejecución que dejó los valores actuales de
`reporte_metricas.md` y `metadata.json` (625 s en total, 23,20 ms por URL) corrió con Maven en
paralelo. Una ejecución anterior, sin esa carga, tardó 444 s y midió 16,11 ms por URL. Los
tiempos sirven como orden de magnitud, no como medición.
