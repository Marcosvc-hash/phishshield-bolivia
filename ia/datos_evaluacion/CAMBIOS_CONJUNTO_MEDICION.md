# Cambios en el conjunto de medición de falsos positivos

Archivo: `bolivia_legitimos_fuera_lista_blanca.csv`. Son 18 sitios bolivianos legítimos que
**nunca deben estar en la lista blanca**, probados sin ruta y con `/login` (36 URL). Miden si
las reglas y la IA marcan como sospechosos sitios reales. Un sitio que entra a la lista blanca
ya no pasa por las reglas ni por la IA, y deja de medir.

## 2026-10-12: sale `ende.bo`, entra `dismac.com.bo`

- **Sale:** `https://www.ende.bo/` y `https://www.ende.bo/login` (ENDE, empresa estatal de
  electricidad).
- **Motivo:** la migración V6 agregó `ende.bo` a la lista blanca, porque `URL_TYPOSQUATTING` lo
  confundía con `entel.bo` (distancia 2) y lo marcaba como sospechoso (45). Desde ese momento ya
  no medía nada. Además, ENDE va a entrar como entidad oficial (`feature/entidad-ende`, V7).
- **Entra:** `https://www.dismac.com.bo/` y `https://www.dismac.com.bo/login` (Dismac, comercio
  privado de electrónica y electrodomésticos).
- **Por qué este sitio:**
  - es privado y no es candidato a entidad oficial ni a la lista blanca;
  - es `.com.bo`, como la mayoría del conjunto;
  - mantiene el tipo `comercio` que tenía la fila de ENDE;
  - respondió 200 el 2026-10-12;
  - no está en PhiUSIIL, así que el modelo no lo vio al entrenar;
  - no está en `dominios_lista`.
- **Cómo se eligió:** entre tres candidatos verificados con esos criterios (`dismac.com.bo`,
  `farmacorp.com` y `eju.tv`). **No se les pasó el modelo antes de elegir**, para no sesgar la
  medición.
- **Resultado después del cambio:** el modelo le da 0.4516 a `dismac.com.bo` (umbral 0.5527).
  En el motor completo queda "seguro" (0 y 10 con `/login`) con peso 40, 30 y 0.
- **Archivos regenerados:**
  - `bolivia_legitimos_fuera_lista_blanca_resultados.csv`, con `evaluar_conjunto.py`;
  - `puntajes_peso_ia.md`, con el motor real y una prueba temporal.

## Candidatos descartados (para no repetir el error)

- `elapas.com.bo`: es candidata a entidad oficial (agua, Sucre). Si entra a la lista blanca,
  pasaría lo mismo que con `ende.bo`.
- `ypfb.gob.bo` **sigue en el conjunto**, pero también es candidata a entidad (ver
  `docs/plan-ia-v2.md`). Si YPFB entra a `entidades_suplantadas`, hay que reemplazarla siguiendo
  este mismo procedimiento.
