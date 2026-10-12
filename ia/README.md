# PhishShield Bolivia — módulo de IA (v1)

Modelo que estima la probabilidad de que una URL sea phishing a partir de su
**dominio (host)**, y explica cada predicción con 3 a 5 señales en texto claro.
Detalles del método, métricas y limitaciones en [FICHA_MODELO.md](FICHA_MODELO.md);
cifras completas en [reporte_metricas.md](reporte_metricas.md).

## Archivos

| Archivo | Qué hace | ¿En git? |
|---|---|---|
| `descargar_datos.py` | Baja PhiUSIIL de UCI a `datos/` y guarda su SHA-256 | sí |
| `caracteristicas.py` | Normaliza la URL y calcula las características (sin red) | sí |
| `modelo.py` | `Detector`: predicción calibrada + explicación | sí |
| `entrenar.py` | Limpia, divide, compara, calibra, evalúa y guarda | sí |
| `servicio.py` | API FastAPI (puerto 8000) | sí |
| `tests/` | Pruebas con pytest | sí |
| `datos_evaluacion/bolivia.csv` | 44 URL bolivianas etiquetadas a mano (1 = phishing) | sí |
| `datos_evaluacion/bolivia_resultados.csv` | Predicciones del modelo sobre ese conjunto | sí |
| `modelos/1.0.0/metadata.json` | Versión, hash del dataset, umbral y métricas | sí |
| `datos/` | Dataset descargado (~57 MB) | **no** (se regenera) |
| `modelos/1.0.0/modelo.joblib` | Modelo entrenado | **no** (se regenera) |

## Comandos (PowerShell, desde la raíz del repositorio)

Requisito: Python 3.11 o superior (`python --version`). Se probó con 3.14.3.

```powershell
cd ia

# 1. Entorno virtual y dependencias (una sola vez)
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt

# 2. Descargar el dataset (~15 MB comprimido)
.\.venv\Scripts\python.exe descargar_datos.py

# 3. Entrenar (varios minutos). Genera modelos\1.0.0\, reporte_metricas.md
#    y datos_evaluacion\bolivia_resultados.csv
.\.venv\Scripts\python.exe entrenar.py

# 4. Pruebas
.\.venv\Scripts\python.exe -m pytest -q

# 5. Levantar el servicio en el puerto 8000
.\.venv\Scripts\python.exe -m uvicorn servicio:app --port 8000
```

Probarlo desde otra ventana de PowerShell:

```powershell
Invoke-RestMethod http://localhost:8000/salud
Invoke-RestMethod -Method Post http://localhost:8000/predecir -ContentType "application/json" `
  -Body '{"url": "http://bnb.com.bo.verificar.cuenta.segura-login.xyz/"}' | ConvertTo-Json -Depth 4
Invoke-RestMethod http://localhost:8000/modelo | ConvertTo-Json -Depth 6
```

La documentación interactiva queda en http://localhost:8000/docs.

## Variables de entorno del servicio

| Variable | Por defecto | Para qué |
|---|---|---|
| `IA_MODELO_DIR` | `ia\modelos\1.0.0` | Carpeta con `modelo.joblib` y `metadata.json` |
| `IA_URL_MAX` | `2048` | Largo máximo de la URL (más largo → 422) |
| `IA_MAX_SENALES` | `5` | Señales por predicción (entre 3 y 5) |
| `IA_LOG_LEVEL` | `INFO` | Nivel de log |

En PowerShell: `$env:IA_URL_MAX = "1024"` antes de levantar el servicio.

## API

`POST /predecir` con `{"url": "http://bnb.com.bo.verificar.cuenta.segura-login.xyz/"}`
responde (respuesta real del modelo 1.0.0):

```json
{
  "probabilidad_phishing": 1.0,
  "es_phishing": true,
  "umbral": 0.5527,
  "senales": [
    {"texto": "tiene 5 subdominios (bnb.com.bo.verificar.cuenta)", "aporte": 16.538, "direccion": "phishing"},
    {"texto": "la extension .xyz se usa mucho en phishing", "aporte": 3.034, "direccion": "phishing"},
    {"texto": "el dominio contiene 'login', 'verificar', 'cuenta'", "aporte": 2.79, "direccion": "phishing"},
    {"texto": "el dominio es largo (44 caracteres)", "aporte": 1.805, "direccion": "phishing"},
    {"texto": "el dominio contiene '.bo', secuencia frecuente en phishing", "aporte": 0.303, "direccion": "phishing"}
  ],
  "modelo": "phishshield-url-ia",
  "version": "1.0.0"
}
```

- `aporte` está en escala logit: positivo empuja hacia phishing, negativo hacia legítimo.
- URL vacía, con espacios, sin dominio o de más de `IA_URL_MAX` caracteres → **422**.
- `GET /salud` → `{"estado": "ok", ...}`. `GET /modelo` → contenido de `metadata.json`.

## Reproducibilidad

- Semilla fija (`SEMILLA = 42` en `entrenar.py`) y versiones fijas en `requirements.txt`.
- `datos/dataset_info.json` guarda el SHA-256 del CSV; `metadata.json` lo repite.
  Si UCI publica otra versión del archivo, el hash cambiará y se notará.
- `tldextract` usa la lista de sufijos públicos que trae el paquete: no consulta internet.
