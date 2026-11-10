# Guion del video (3 minutos) — Mi aporte en la IA de PhishShield Bolivia

Ritmo: unas 140 palabras por minuto. La columna "Qué digo" es el texto literal para leer.
Las líneas de código citadas corresponden al estado actual del repositorio.

| Tiempo | Qué digo | Qué muestro en pantalla |
|---|---|---|
| 0:00–0:20 | Hola, soy Marcos Vargas y este es PhishShield Bolivia, un sistema que analiza enlaces para detectar phishing contra bancos y entidades bolivianas. En este video muestro mi aporte en la inteligencia artificial: el modelo, cómo lo entrené y cómo lo conecté con el backend. | Navegador en el analizador de Angular (`http://localhost:4200`), vacío. Al final, cambiar al IDE con la carpeta `ia/` abierta en el explorador. |
| 0:20–0:45 | El sistema ya tenía un motor de reglas: revisa si la URL usa una IP, si no tiene https, si la extensión es sospechosa o si se parece a un banco. Las reglas son claras, pero solo atrapan lo que alguien pensó antes. El modelo aprende patrones de miles de dominios de phishing y suma una señal más. | `backend/src/main/java/bo/phishshield/backend/analisis/infrastructure/config/AnalisisConfig.java`, líneas 35–44 (`reglasUrlBase()` con la lista de reglas). |
| 0:45–1:20 | Entrené con PhiUSIIL, un dataset de la UCI con unas 235 mil URL. Primero invertí la etiqueta, porque ahí el cero es phishing, y dejé un registro por dominio. Revisándolo encontré un sesgo: todas las legítimas son portadas sin ruta. Lo comprobé con un experimento de control. Con la URL completa, el modelo saca un F1 de 0,98, pero marca como phishing las 10 páginas bolivianas legítimas con ruta. Por eso mi modelo mira solo el dominio: baja a 0,73, pero falla solo en 2 de esas 10. | 1) `ia/entrenar.py`, línea 68 (`df["y"] = 1 - df["label"]`). 2) `ia/FICHA_MODELO.md`, líneas 36–51: párrafo del sesgo y **tabla del experimento de control**. Resaltar con el cursor "10 de 10" y "2 de 10". |
| 1:20–2:00 | En caracteristicas.py calculo diez rasgos del dominio, como el largo, los guiones, los subdominios o si la extensión es de las abusadas, y además n-gramas de letras con TF-IDF. En entrenar.py comparo tres regresiones logísticas y un HistGradientBoosting; ganó la regresión logística. Luego la calibro con Platt y elijo el umbral 0,5527, que exige un 90 % de precisión. En modelo.py, como el logit es una suma de coeficiente por valor, sé exactamente qué rasgo empujó la predicción: esas son las señales. | 1) `ia/caracteristicas.py`, líneas 118–121 (`CARACTERISTICAS_HOST`). 2) `ia/entrenar.py`, líneas 249–256 (bucle de C = 0,3, 1 y 3) y 183–194 (`elegir_umbral`). 3) `ia/modelo.py`, líneas 75–79 (`explicar`, `aportes = coef[indices] * valores`). |
| 2:00–2:40 | El servicio en Python expone POST predecir. En el backend, ModeloIaRestAdapter lo llama con un límite de dos segundos y, si falla, el análisis sigue solo con las reglas. IndicadorIa suma probabilidad por cuarenta solo si pasa el umbral; nunca resta. Ahora en vivo: esta URL usa bnb.com.bo como subdominio de un dominio punto xyz. Sale phishing, cien sobre cien, y la IA aporta cuarenta con sus señales. Y el sitio real del BNB sale seguro: está en la lista blanca y ni siquiera consulta a la IA. | 1) `backend/.../analisis/infrastructure/ia/ModeloIaRestAdapter.java`, líneas 69–72 (`catch` con `log.warn`). 2) `backend/.../analisis/domain/service/IndicadorIa.java`, líneas 29–37. 3) Navegador: pegar `http://bnb.com.bo.verificar.cuenta.segura-login.xyz/` → **Analizar**. Señalar "Phishing detectado", "100 / 100" y la línea "Modelo de IA 1.0.0: 100% de probabilidad de phishing. Senales: tiene 5 subdominios…" **+40**. 4) **Limpiar**, pegar `https://www.bnb.com.bo/` → **Analizar**. Señalar "Sin senales de riesgo". |
| 2:40–3:00 | Sus limitaciones: el dataset casi no tiene dominios bolivianos, el modelo no ve la ruta y no detecta typosquatting de una letra; eso lo cubren las reglas. Como mejora, quiero reentrenarlo con datos bolivianos y con páginas legítimas que sí tengan ruta. Gracias por su atención. | `ia/FICHA_MODELO.md`, sección "Limitaciones" (líneas 187–204). Terminar en el navegador con el resultado de phishing visible. |

**Palabras narradas:** 406 en total (0:00–0:20: 44 · 0:20–0:45: 58 · 0:45–1:20: 88 ·
1:20–2:00: 82 · 2:00–2:40: 88 · 2:40–3:00: 46). El bloque de 0:45 es el más denso: léelo sin pausas largas.

### Notas sobre la demo en vivo

- **Usé otra URL falsa que la propuesta.** La URL `http://bnb.com.bo.verificar.cuenta.segura-login.xyz/`
  es la que está verificada de punta a punta en `docs/guia-demo.md` (fila 6: phishing, 100
  puntos, IA +40). La variante `http://bnb.com.bo.verificar-cuenta.xyz/` también da
  probabilidad 1,0 en el modelo (lo comprobé cargando `modelo.joblib`), pero su puntaje
  completo en el backend no lo calculé. Si prefieres esa, pruébala antes de grabar.
- **La probabilidad y las señales no salen en campos aparte.** En el frontend aparecen dentro del
  texto del indicador de la IA, en la lista "Por que se marco asi" (`analizador.html`, líneas
  46–55), con el aporte a la derecha.
- **Para `bnb.com.bo` no aparece nada de la IA,** porque está en la lista blanca y el backend no
  la consulta (`AnalizarUrlUseCase.java`, líneas 158–163). Eso es lo que se narra. El frontend
  exige `http://` o `https://`, así que hay que escribir `https://www.bnb.com.bo/`.
- **Si la base se creó hace menos de 24 horas,** `https://www.bnb.com.bo/` sale "seguro" con 4
  puntos en vez de 0, por un análisis de ejemplo del seed (ver `docs/guia-demo.md`, sección 4).
  El veredicto no cambia.

---

## Antes de grabar: levantar todo (PowerShell, una ventana por servicio)

Tomado de `docs/guia-demo.md`, que se verificó con los servicios reales.

**Una sola vez, si `ia\modelos\1.0.0\modelo.joblib` no existe o la base es vieja:**

```powershell
cd C:\proyectos\phishshield-bolivia\ia
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe descargar_datos.py
.\.venv\Scripts\python.exe entrenar.py
cd ..
Get-Content database\migraciones\V5__modelo_ia_url.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
Get-Content database\migraciones\V6__lista_blanca_ende.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
Get-Content database\migraciones\V7__entidad_ende.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
```

**Ventana 1 — base de datos (PostgreSQL en Docker):**

```powershell
docker start phishshield-db
docker ps          # debe decir "phishshield-db ... (healthy)"
```

**Ventana 2 — servicio de IA (puerto 8000):**

```powershell
cd C:\proyectos\phishshield-bolivia\ia
.\.venv\Scripts\python.exe -m uvicorn servicio:app --port 8000
```

Esperar `Modelo phishshield-url-ia 1.0.0 cargado ... (umbral 0.5527)`.

**Ventana 3 — backend (puerto 8080):**

```powershell
cd C:\proyectos\phishshield-bolivia\backend
$env:DB_PASSWORD = (docker exec phishshield-db printenv POSTGRES_PASSWORD)
.\mvnw.cmd spring-boot:run
```

Esperar `Modelo de IA en http://localhost:8000 (timeout 2000 ms, habilitado)` y
`Started BackendApplication`. No grabes esta ventana mientras escribes la línea de
`DB_PASSWORD` (el comando no la imprime, pero mejor no mostrarlo).

**Ventana 4 — frontend (puerto 4200):**

```powershell
cd C:\proyectos\phishshield-bolivia\frontend
npx ng serve --port 4200
```

**Verificación (otra ventana). Las dos deben responder sin error antes de abrir el navegador:**

```powershell
Invoke-RestMethod http://localhost:8000/salud
Invoke-RestMethod -Method Post http://localhost:8000/predecir -ContentType "application/json" -Body '{"url": "https://pagos-ende.xyz/"}' | Select-Object probabilidad_phishing, es_phishing
```

La segunda debe dar `probabilidad_phishing 0.9433` y `es_phishing True`.

## Lista de comprobación para grabar

- [ ] Las 4 ventanas levantadas y la verificación anterior en "ok". **No analices ninguna URL
      con la IA apagada:** el backend guarda cada resultado 24 horas y lo devolvería sin la IA.
- [ ] Ensayo previo: si ya analizaste las URL del video y el resultado no coincide con el guion,
      agrega `?demo=1` (o 2, 3…) al final; es otra URL para el backend y no cambia el puntaje.
- [ ] Cada análisis queda guardado en la base real y el de phishing crea una alerta. Si quieres
      poder encontrarlos después, usa una marca propia, por ejemplo `?m=video-20261022`.
- [ ] Pestañas del IDE abiertas en este orden: `AnalisisConfig.java`, `entrenar.py`,
      `FICHA_MODELO.md`, `caracteristicas.py`, `modelo.py`, `ModeloIaRestAdapter.java`,
      `IndicadorIa.java`.
- [ ] Zoom del IDE al 150 % o más (texto legible en 1080p), tema claro u oscuro con buen
      contraste, y la barra lateral cerrada cuando muestres código.
- [ ] `FICHA_MODELO.md` abierto en vista previa de Markdown, para que la tabla del control se lea bien.
- [ ] Navegador en `http://localhost:4200`, zoom al 125 %, sin otras pestañas ni marcadores
      personales visibles.
- [ ] Las dos URL copiadas en un bloc de notas fuera de la grabación:
      `http://bnb.com.bo.verificar.cuenta.segura-login.xyz/` y `https://www.bnb.com.bo/`.
- [ ] Notificaciones de Windows apagadas (modo "No molestar").
- [ ] Cronometrar un ensayo leyendo el guion completo: debe quedar entre 2:50 y 3:00.
