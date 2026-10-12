# Guía de la demo — PhishShield Bolivia con el módulo de IA

Comandos para **Windows PowerShell**, en orden. Cada servicio corre en su propia ventana.
Los resultados esperados se verificaron en vivo el 2026-10-12, con los cuatro servicios encendidos.

## 0. Antes de empezar (una sola vez)

```powershell
cd C:\proyectos\phishshield-bolivia
git checkout modulo-ia

# Entorno de la IA y modelo (si ia\modelos\1.0.0\modelo.joblib no existe)
cd ia
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe descargar_datos.py
.\.venv\Scripts\python.exe entrenar.py          # unos 7 minutos
cd ..

# Migración V5 (si la base se creó antes de este cambio; se puede repetir sin problema)
Get-Content database\migraciones\V5__modelo_ia_url.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
```

## 1. Levantar los cuatro servicios

**Ventana 1: base de datos**
```powershell
docker start phishshield-db
docker ps          # debe decir "phishshield-db ... (healthy)"
```

**Ventana 2: servicio de IA (puerto 8000)**
```powershell
cd C:\proyectos\phishshield-bolivia\ia
.\.venv\Scripts\python.exe -m uvicorn servicio:app --port 8000
```
Esperar la línea `Modelo phishshield-url-ia 1.0.0 cargado ... (umbral 0.5527)`.

**Ventana 3: backend (puerto 8080)**
```powershell
cd C:\proyectos\phishshield-bolivia\backend
$env:DB_PASSWORD = (docker exec phishshield-db printenv POSTGRES_PASSWORD)
.\mvnw.cmd spring-boot:run
```
Esperar `Modelo de IA en http://localhost:8000 (timeout 2000 ms, habilitado)` y `Started BackendApplication`.

**Ventana 4: frontend (puerto 4200)**
```powershell
cd C:\proyectos\phishshield-bolivia\frontend
npx ng serve --port 4200
```
Abrir http://localhost:4200.

**Comprobación rápida** (en otra ventana):
```powershell
Invoke-RestMethod http://localhost:8000/salud
Invoke-RestMethod "http://localhost:8080/api/analisis?limite=1" | Out-Null; "backend ok"
```

## 2. URL de demostración y resultado esperado

Pegarlas en el analizador del frontend, en este orden. "IA" es el aporte del indicador
`IA_MODELO_URL` (probabilidad × 40, solo si supera el umbral 0,5527).

| # | URL | Resultado | Puntaje | IA | Qué muestra |
|---|---|---|---|---|---|
| 1 | `https://www.bnb.com.bo/` | seguro | 0 | no se consulta | Dominio oficial en la lista blanca |
| 2 | `https://www.bnb.com.bo/login` | seguro | 10 | no se consulta | La ruta sensible sola no alcanza |
| 3 | `https://www.umsa.bo/` | seguro | 0 | 0 (prob. 0,546 < umbral) | Sitio boliviano fuera de la lista blanca |
| 4 | `https://bancounion-bo-actualizacion.com/datos` | **phishing** | 75,61 | +30,61 | **El aporte de la IA:** sin ella es sospechoso (45) |
| 5 | `https://bmsc-verificar-cuenta.online/` | phishing | 96,98 | +39,98 | IA + typosquatting + TLD abusado |
| 6 | `http://bnb.com.bo.verificar.cuenta.segura-login.xyz/` | phishing | 100 | +40 | Oficial usado como subdominio: la IA sí se consulta |
| 7 | `https://bancounlon.com.bo/` | sospechoso | 45 | 0 (prob. 0,36) | La IA no ve el typosquatting; la regla sí |
| 8 | `https://impuestos-gob-bo.info/verificar-nit` | phishing | 100 | +34,93 | Lista negra + reglas + IA |
| 9 | `http://192.168.1.1/` | seguro | 38 | no se consulta | Las IP quedan solo a cargo de las reglas |

En la tarjeta del resultado, bajo "Por que se marco asi", el indicador de la IA muestra su
porcentaje y sus señales. Este es el texto real de la fila 6:
*"Modelo de IA 1.0.0: 100% de probabilidad de phishing. Senales: tiene 5 subdominios
(bnb.com.bo.verificar.cuenta); la extension .xyz se usa mucho en phishing; el dominio contiene
'login', 'verificar', 'cuenta'"* (+40).

**Caché de 24 h:** una URL ya analizada devuelve el mismo resultado durante 24 horas.
- Si una URL de la tabla da otra cosa, probablemente se analizó antes de activar la IA.
  Prueba la misma URL con `?demo=1` al final.
- Ojo con `http://192.168.1.1` **sin barra final**: hasta el 2026-10-13 a las 09:40 queda en
  caché un resultado previo a una corrección (phishing 78). Usa la versión con barra final
  (fila 9).

## 3. Demostrar que el análisis sigue sin la IA

1. En la ventana 2, apretar `Ctrl+C` para apagar el servicio de IA.
2. Analizar una URL nueva, por ejemplo `https://bancounion-bo-actualizacion.com/datos?demo=ia-apagada`.
3. Resultado esperado: **sospechoso, 45**, solo con `URL_TYPOSQUATTING`, en unos 50 ms. Es la
   misma URL de la fila 4: sin la IA baja de phishing a sospechoso.
4. En la ventana 3 aparece:
   `WARN ... ModeloIaRestAdapter : Servicio de IA no disponible (...); se sigue solo con las reglas`.
5. Volver a levantar la IA (paso 1, ventana 2).

## 4. Si algo falla

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| El backend no arranca: error de contraseña | Falta `DB_PASSWORD` en esa ventana | Repetir la línea `$env:DB_PASSWORD = ...` en la misma ventana |
| `WARN La regla IA_MODELO_URL existe en Java pero no en reglas_deteccion` | Falta la migración V5 | Aplicar V5 (paso 0) y reiniciar el backend |
| El servicio de IA dice `No existe ...modelo.joblib` | No se entrenó en esta máquina | `descargar_datos.py` y `entrenar.py` (paso 0) |
| El análisis funciona pero nunca aparece la IA | Servicio de IA apagado (hay `WARN` en el backend) | Levantar la ventana 2; comprobar `/salud` |
| Una URL da un resultado distinto al de la tabla | Viene de la caché de 24 h | Agregar `?demo=1` al final de la URL |
| El frontend no está en 4200 | El puerto está ocupado o existe la variable `PORT` | Usar la dirección que imprime `ng serve` |
| `docker compose ps` sale vacío | El contenedor se creó desde otra carpeta | Usar `docker ps` |
| Puerto 8000 u 8080 ocupado | Quedó un proceso de una ejecución anterior | `Get-NetTCPConnection -LocalPort 8000` y cerrar ese proceso |

Plan B si falla la IA en vivo: el sistema sigue funcionando solo con las reglas (sección 3).
Las métricas del modelo están en `ia/FICHA_MODELO.md`.
