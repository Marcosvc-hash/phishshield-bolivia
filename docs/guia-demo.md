# Guía de la demo — PhishShield Bolivia con el módulo de IA

Comandos para **Windows PowerShell**, en orden. Cada servicio corre en su propia ventana.
Los resultados esperados se verificaron el 2026-10-12 con los servicios reales, sobre una base
creada desde `01_schema.sql` + `02_seed.sql` (incluye V5 a V7).

## 0. Antes de empezar (una sola vez)

```powershell
cd C:\proyectos\phishshield-bolivia
git checkout main
git pull

# Entorno de la IA y modelo (si ia\modelos\1.0.0\modelo.joblib no existe)
cd ia
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe descargar_datos.py
.\.venv\Scripts\python.exe entrenar.py          # unos 7 a 10 minutos
cd ..

# Migraciones (si la base se creó antes de estos cambios; se pueden repetir sin problema)
Get-Content database\migraciones\V5__modelo_ia_url.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
Get-Content database\migraciones\V6__lista_blanca_ende.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
Get-Content database\migraciones\V7__entidad_ende.sql | docker exec -i phishshield-db psql -U phishshield -d phishshield
```

## 1. Levantar y verificar la IA antes de analizar cualquier URL

> **Regla de la demo:** no analizar ninguna URL hasta que el paso 1.5 dé "ok".
> El backend guarda cada análisis durante 24 horas. Una URL analizada con la IA apagada queda
> guardada **sin la IA** todo ese tiempo, y en la demo mostraría otro resultado.

**1.1 Ventana 1: base de datos**
```powershell
docker start phishshield-db
docker ps          # debe decir "phishshield-db ... (healthy)"
```

**1.2 Ventana 2: servicio de IA (puerto 8000)**
```powershell
cd C:\proyectos\phishshield-bolivia\ia
.\.venv\Scripts\python.exe -m uvicorn servicio:app --port 8000
```
Esperar la línea `Modelo phishshield-url-ia 1.0.0 cargado ... (umbral 0.5527)`.

**1.3 Ventana 3: backend (puerto 8080)**
```powershell
cd C:\proyectos\phishshield-bolivia\backend
$env:DB_PASSWORD = (docker exec phishshield-db printenv POSTGRES_PASSWORD)
.\mvnw.cmd spring-boot:run
```
Esperar `Modelo de IA en http://localhost:8000 (timeout 2000 ms, habilitado)` y `Started BackendApplication`.

**1.4 Ventana 4: frontend (puerto 4200)**
```powershell
cd C:\proyectos\phishshield-bolivia\frontend
npx ng serve --port 4200
```

**1.5 Verificación, en otra ventana. Las dos líneas deben responder sin error:**
```powershell
Invoke-RestMethod http://localhost:8000/salud            # estado = ok, version = 1.0.0
Invoke-RestMethod -Method Post http://localhost:8000/predecir -ContentType "application/json" -Body '{"url": "https://pagos-ende.xyz/"}' | Select-Object probabilidad_phishing, es_phishing
```
La segunda debe dar `probabilidad_phishing 0.9433` y `es_phishing True`. Recién entonces se
abre http://localhost:4200.

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
| 10 | `https://pagos-ende.xyz/` | **phishing** | 94,73 | +37,73 | Imitación de Ende (entidad agregada en V7): typosquatting 45 + TLD 12 + IA |

En la tarjeta del resultado, bajo "Por que se marco asi", el indicador de la IA muestra su
porcentaje y sus señales. Este es el texto real de la fila 6:
*"Modelo de IA 1.0.0: 100% de probabilidad de phishing. Senales: tiene 5 subdominios
(bnb.com.bo.verificar.cuenta); la extension .xyz se usa mucho en phishing; el dominio contiene
'login', 'verificar', 'cuenta'"* (+40).

En la fila 10, typosquatting muestra: *"El dominio usa la marca 'ende' de Ende sin ser su
dominio oficial"*.

## 3. Demostrar que el análisis sigue sin la IA

1. En la ventana 2, apretar `Ctrl+C` para apagar el servicio de IA.
2. Analizar `https://tigo-money-bolivia.top/premio`, una URL que **no** está en la tabla de
   arriba. No usarla antes en la demo: quedaría guardada con el resultado de ese momento.
3. Resultado esperado: **sospechoso, 57**, solo con reglas (typosquatting 45 + TLD 12). Con la
   IA encendida, la misma URL da phishing 96,57: esa diferencia es el aporte de la IA.
4. En la ventana 3 aparece:
   `WARN ... ModeloIaRestAdapter : Servicio de IA no disponible (...); se sigue solo con las reglas`.
5. Volver a levantar la IA (paso 1.2) y repetir la verificación del paso 1.5.

## 4. Si algo falla

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| El backend no arranca: error de contraseña | Falta `DB_PASSWORD` en esa ventana | Repetir la línea `$env:DB_PASSWORD = ...` en la misma ventana |
| `WARN La regla IA_MODELO_URL existe en Java pero no en reglas_deteccion` | Falta la migración V5 | Aplicar las migraciones (paso 0) y reiniciar el backend |
| `pagos-ende.xyz` da sospechoso 57 con la IA encendida | Falta la migración V7, o se analizó antes con la IA apagada | Aplicar V7; si ya estaba, ver el plan de emergencia |
| La fila 1 da seguro **4** sin indicadores | La base se creó hace menos de 24 h: `02_seed.sql` trae un análisis de ejemplo de esa URL, con puntaje escrito a mano, que el backend toma como reciente | Es esperado en una base nueva; el veredicto (seguro) no cambia. Pasadas 24 h da 0 |
| El servicio de IA dice `No existe ...modelo.joblib` | No se entrenó en esta máquina | `descargar_datos.py` y `entrenar.py` (paso 0) |
| El análisis funciona pero nunca aparece la IA | Servicio de IA apagado (hay `WARN` en el backend) | Levantar la ventana 2 y repetir el paso 1.5 |
| El frontend no está en 4200 | El puerto está ocupado o existe la variable `PORT` | Usar la dirección que imprime `ng serve` |
| `docker compose ps` sale vacío | El contenedor se creó desde otra carpeta | Usar `docker ps` |
| Puerto 8000 u 8080 ocupado | Quedó un proceso de una ejecución anterior | `Get-NetTCPConnection -LocalPort 8000` y cerrar ese proceso |

**Plan de emergencia (solo si una URL ya quedó guardada con un resultado distinto al de la
tabla):** agregar `?demo=1` (o 2, 3...) al final de la URL. Es otra URL para el backend, así que
se analiza de nuevo, y el parámetro no cambia el puntaje: las reglas miran el dominio y la
ruta, y la IA solo el dominio. Cada uso deja un análisis más en la base.

Si la IA falla en vivo y no se puede levantar, el sistema sigue funcionando solo con las
reglas (sección 3). Las métricas del modelo están en `ia/FICHA_MODELO.md`.
