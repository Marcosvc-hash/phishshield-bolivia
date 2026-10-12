# Puntaje final con el indicador IA_MODELO_URL: peso 40, 30 y 0

Calculado el 2026-10-12 con el motor real del backend. Se usaron:
- `AnalizarUrlUseCase`, con las reglas y los pesos leídos de la base: IP 30, sin HTTPS 8,
  typosquatting 45, TLD 12, subdominios 10, lista negra 70, ruta sensible 10.
- Las probabilidades reales del servicio de IA 1.0.0 (umbral 0.5527).
- Las mismas listas que tiene la base después de V6 (la lista blanca incluye `ende.bo`).

Se usó una prueba temporal que no se commitea (regla del proyecto). La IA no se consulta para
dominios de la lista blanca ni para IP literales.

Columnas: p40/v40 = puntaje y veredicto con peso 40; p30/v30 con peso 30; p0/v0 sin IA.
Las URL con /login son un caso extremo: activan URL_RUTA_SENSIBLE aunque la página no exista.

Versión 2 de esta tabla: en el conjunto de medición se reemplazó `ende.bo` por `dismac.com.bo`
(ver `CAMBIOS_CONJUNTO_MEDICION.md`).

## bolivia_legitimos_fuera_lista_blanca.csv

| URL | Etiqueta | Prob. IA | Consulta IA | p40 | v40 | p30 | v30 | p0 | v0 | Reglas |
|---|---|---|---|---|---|---|---|---|---|---|
| `https://www.umsa.bo/` | 0 | 0.5463 | si | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.umsa.bo/login` | 0 | 0.5463 | si | 10.00 | seguro | 10.00 | seguro | 10.00 | seguro | [RUTA_SENSIBLE=10] |
| `https://www.umss.edu.bo/` | 0 | 0.1072 | si | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.umss.edu.bo/login` | 0 | 0.1072 | si | 10.00 | seguro | 10.00 | seguro | 10.00 | seguro | [RUTA_SENSIBLE=10] |
| `https://www.uagrm.edu.bo/` | 0 | 0.1753 | si | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.uagrm.edu.bo/login` | 0 | 0.1753 | si | 10.00 | seguro | 10.00 | seguro | 10.00 | seguro | [RUTA_SENSIBLE=10] |
| `https://www.ucb.edu.bo/` | 0 | 0.076 | si | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.ucb.edu.bo/login` | 0 | 0.076 | si | 10.00 | seguro | 10.00 | seguro | 10.00 | seguro | [RUTA_SENSIBLE=10] |
| `https://www.upb.edu/` | 0 | 0.0001 | si | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.upb.edu/login` | 0 | 0.0001 | si | 10.00 | seguro | 10.00 | seguro | 10.00 | seguro | [RUTA_SENSIBLE=10] |
| `https://eldeber.com.bo/` | 0 | 0.0972 | si | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://eldeber.com.bo/login` | 0 | 0.0972 | si | 10.00 | seguro | 10.00 | seguro | 10.00 | seguro | [RUTA_SENSIBLE=10] |
| `https://www.lostiempos.com/` | 0 | 0.1613 | si | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.lostiempos.com/login` | 0 | 0.1613 | si | 10.00 | seguro | 10.00 | seguro | 10.00 | seguro | [RUTA_SENSIBLE=10] |
| `https://www.opinion.com.bo/` | 0 | 0.3108 | si | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.opinion.com.bo/login` | 0 | 0.3108 | si | 10.00 | seguro | 10.00 | seguro | 10.00 | seguro | [RUTA_SENSIBLE=10] |
| `https://correodelsur.com/` | 0 | 0.2391 | si | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://correodelsur.com/login` | 0 | 0.2391 | si | 10.00 | seguro | 10.00 | seguro | 10.00 | seguro | [RUTA_SENSIBLE=10] |
| `https://www.minedu.gob.bo/` | 0 | 0.1222 | si | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.minedu.gob.bo/login` | 0 | 0.1222 | si | 10.00 | seguro | 10.00 | seguro | 10.00 | seguro | [RUTA_SENSIBLE=10] |
| `https://www.economiayfinanzas.gob.bo/` | 0 | 0.0473 | si | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.economiayfinanzas.gob.bo/login` | 0 | 0.0473 | si | 10.00 | seguro | 10.00 | seguro | 10.00 | seguro | [RUTA_SENSIBLE=10] |
| `https://www.minsalud.gob.bo/` | 0 | 0.0285 | si | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.minsalud.gob.bo/login` | 0 | 0.0285 | si | 10.00 | seguro | 10.00 | seguro | 10.00 | seguro | [RUTA_SENSIBLE=10] |
| `https://www.bcb.gob.bo/` | 0 | 0.0204 | si | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.bcb.gob.bo/login` | 0 | 0.0204 | si | 10.00 | seguro | 10.00 | seguro | 10.00 | seguro | [RUTA_SENSIBLE=10] |
| `https://www.asfi.gob.bo/` | 0 | 0.026 | si | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.asfi.gob.bo/login` | 0 | 0.026 | si | 10.00 | seguro | 10.00 | seguro | 10.00 | seguro | [RUTA_SENSIBLE=10] |
| `https://www.boa.bo/` | 0 | 0.2021 | si | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.boa.bo/login` | 0 | 0.2021 | si | 10.00 | seguro | 10.00 | seguro | 10.00 | seguro | [RUTA_SENSIBLE=10] |
| `https://www.fidalga.com/` | 0 | 0.0877 | si | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.fidalga.com/login` | 0 | 0.0877 | si | 10.00 | seguro | 10.00 | seguro | 10.00 | seguro | [RUTA_SENSIBLE=10] |
| `https://www.ypfb.gob.bo/` | 0 | 0.0381 | si | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.ypfb.gob.bo/login` | 0 | 0.0381 | si | 10.00 | seguro | 10.00 | seguro | 10.00 | seguro | [RUTA_SENSIBLE=10] |
| `https://www.dismac.com.bo/` | 0 | 0.4516 | si | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.dismac.com.bo/login` | 0 | 0.4516 | si | 10.00 | seguro | 10.00 | seguro | 10.00 | seguro | [RUTA_SENSIBLE=10] |

## bolivia.csv

| URL | Etiqueta | Prob. IA | Consulta IA | p40 | v40 | p30 | v30 | p0 | v0 | Reglas |
|---|---|---|---|---|---|---|---|---|---|---|
| `https://www.bnb.com.bo/` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://bnb.com.bo` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.bnb.com.bo/PortalBNB/Principal/BancaPersonas` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.bancounion.com.bo/` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://bancounion.com.bo` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.bancounion.com.bo/BancaDigital/UninetPlus` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.bancounion.com.bo/AgenciasAtm/AgenciasAtms` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.bmsc.com.bo/` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://bmsc.com.bo` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.bmsc.com.bo/help/questions` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.bisa.com/home` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://bisa.com` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.bisa.com/empresas/comercio-exterior` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.bg.com.bo/personas/` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://bg.com.bo` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.bg.com.bo/canales-digitales/cajeros-automaticos/` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.tigo.com.bo/` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://tigo.com.bo` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.impuestos.gob.bo/` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://impuestos.gob.bo` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.impuestos.gob.bo/index.php/memoria-intitucional/` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.entel.bo/` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://entel.bo` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.segip.gob.bo/` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://segip.gob.bo` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `https://www.segip.gob.bo/comunicados/` | 0 | - | no (lista blanca) | 0.00 | seguro | 0.00 | seguro | 0.00 | seguro | [] |
| `http://bnb-bo-seguro.com/login/verificar-cuenta` | 1 | 0.8468 | si | 100.00 | phishing | 100.00 | phishing | 100.00 | phishing | [SIN_HTTPS=8, RUTA_SENSIBLE=10, TYPOSQUATTING=45, LISTA_NEGRA=70] |
| `https://bancounion-verificacion.net/` | 1 | 0.9575 | si | 100.00 | phishing | 100.00 | phishing | 100.00 | phishing | [TYPOSQUATTING=45, LISTA_NEGRA=70] |
| `https://impuestos-gob-bo.info/verificar-nit` | 1 | 0.8733 | si | 100.00 | phishing | 100.00 | phishing | 100.00 | phishing | [TLD_SOSPECHOSO=12, RUTA_SENSIBLE=10, TYPOSQUATTING=45, LISTA_NEGRA=70] |
| `https://premios-tigo-bo.xyz/sorteo` | 1 | 0.9701 | si | 95.80 | phishing | 86.10 | phishing | 57.00 | sospechoso | [TLD_SOSPECHOSO=12, TYPOSQUATTING=45] |
| `http://bnb-banca-segura.com/ingresar` | 1 | 0.965 | si | 91.60 | phishing | 81.95 | phishing | 53.00 | sospechoso | [SIN_HTTPS=8, TYPOSQUATTING=45] |
| `https://bancounion-bo-actualizacion.com/datos` | 1 | 0.7653 | si | 75.61 | phishing | 67.96 | sospechoso | 45.00 | sospechoso | [TYPOSQUATTING=45] |
| `https://bmsc-verificar-cuenta.online/` | 1 | 0.9995 | si | 96.98 | phishing | 86.99 | phishing | 57.00 | sospechoso | [TLD_SOSPECHOSO=12, TYPOSQUATTING=45] |
| `https://tigo-money-bolivia.top/premio` | 1 | 0.9892 | si | 96.57 | phishing | 86.68 | phishing | 57.00 | sospechoso | [TLD_SOSPECHOSO=12, TYPOSQUATTING=45] |
| `https://segip-gob-bo.site/cedula` | 1 | 0.9952 | si | 96.81 | phishing | 86.86 | phishing | 57.00 | sospechoso | [TLD_SOSPECHOSO=12, TYPOSQUATTING=45] |
| `http://bnbb.com.bo/login` | 1 | 0.701 | si | 91.04 | phishing | 84.03 | phishing | 63.00 | sospechoso | [SIN_HTTPS=8, RUTA_SENSIBLE=10, TYPOSQUATTING=45] |
| `https://bancounlon.com.bo/` | 1 | 0.3605 | si | 45.00 | sospechoso | 45.00 | sospechoso | 45.00 | sospechoso | [TYPOSQUATTING=45] |
| `https://www.bmcs.com.bo/` | 1 | 0.3657 | si | 45.00 | sospechoso | 45.00 | sospechoso | 45.00 | sospechoso | [TYPOSQUATTING=45] |
| `https://blsa.com/banca` | 1 | 0.1868 | si | 55.00 | sospechoso | 55.00 | sospechoso | 55.00 | sospechoso | [RUTA_SENSIBLE=10, TYPOSQUATTING=45] |
| `https://entel-bo.com/recarga` | 1 | 0.1951 | si | 45.00 | sospechoso | 45.00 | sospechoso | 45.00 | sospechoso | [TYPOSQUATTING=45] |
| `http://bnb.com.bo.verificar.cuenta.segura-login.xyz/` | 1 | 1.0 | si | 100.00 | phishing | 100.00 | phishing | 75.00 | phishing | [SIN_HTTPS=8, TLD_SOSPECHOSO=12, SUBDOMINIOS=10, TYPOSQUATTING=45] |
| `https://bancounion.com.bo.validacion-clientes.com/` | 1 | 1.0 | si | 95.00 | phishing | 85.00 | phishing | 55.00 | sospechoso | [SUBDOMINIOS=10, TYPOSQUATTING=45] |
| `https://impuestos.gob.bo.pagos-nit.net/` | 1 | 0.9987 | si | 94.95 | phishing | 84.96 | phishing | 55.00 | sospechoso | [SUBDOMINIOS=10, TYPOSQUATTING=45] |
| `https://www.tigo.com.bo.billetera-movil.club/` | 1 | 1.0 | si | 100.00 | phishing | 97.00 | phishing | 67.00 | sospechoso | [TLD_SOSPECHOSO=12, SUBDOMINIOS=10, TYPOSQUATTING=45] |

## Conclusiones

- **Legítimos fuera de la lista blanca (18 sitios, 36 URL):** ninguna probabilidad llega al
  umbral (la más alta es `umsa.bo`, 0.5463), así que la IA aporta 0 con peso 40 y con peso 30.
  Las 36 URL quedan como "seguro" con cualquier peso.
- **Phishing (bolivia.csv, 18 URL):** veredicto "phishing" en 4 sin IA, en 13 con peso 30 y en
  14 con peso 40. La diferencia entre 30 y 40 es `bancounion-bo-actualizacion.com` (67.96 frente a 75.61).
- **Legítimas de bolivia.csv (26 URL):** todas son "seguro", porque están en la lista blanca y
  no consultan la IA.
- **Margen teórico:** si un sitio legítimo superara el umbral, la IA sumaría al menos
  0.5527 x 40 = 22.11 (16.58 con peso 30). Junto con ruta sensible (10) y sin HTTPS (8) daría
  40.11, que es "sospechoso" con peso 40 y "seguro" con peso 30. No ocurrió en este conjunto.
- **Versión 1 de la tabla (antes del cambio de conjunto):** `ende.bo` salía "sospechoso" (45 y
  55 con /login) con cualquier peso, incluso sin IA, por `URL_TYPOSQUATTING` (distancia 2 a
  `entel.bo`). Se corrigió con V6 (lista blanca), y por eso salió del conjunto.
