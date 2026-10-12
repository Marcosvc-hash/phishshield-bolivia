# Reporte de metricas - phishshield-url-ia 1.0.0

Generado automaticamente por `entrenar.py` el 2026-10-12. No editar a mano: volver a ejecutar el script.

- Dataset: PhiUSIIL Phishing URL Dataset (UCI, CC BY 4.0), SHA-256 `a236549cd369cd80bd478ff8e1779cbf44c58d5c3f79f7a51a1adbed7d06d1c6`
- Entrada del modelo: host normalizado (sin esquema, sin 'www.', sin puerto ni ruta)
- Semilla: 42. Entorno: {'python': '3.14.3', 'scikit_learn': '1.9.1', 'numpy': '2.5.4', 'pandas': '3.0.6'}

## Limpieza

| Paso | Valor |
|---|---|
| filas_originales | 235795 |
| balance_original | {0: 134850, 1: 100945} |
| tras_quitar_url_duplicadas | 235370 |
| tras_quitar_host_vacio | 235370 |
| hosts_con_etiqueta_contradictoria | 58 |
| tras_quitar_contradictorios | 235211 |
| tras_un_registro_por_host | 219036 |
| balance_final | {0: 134792, 1: 84244} |
| dominios_registrables | 175451 |

Balance: 0 = legitimo, 1 = phishing.

## Division (agrupada por dominio registrable)

| Particion | Filas | Fraccion | Phishing | Dominios |
|---|---|---|---|---|
| entrenamiento | 153283 | 0.700 | 59088 | 122807 |
| validacion | 32897 | 0.150 | 10033 | 29936 |
| prueba | 32856 | 0.150 | 15123 | 22708 |
| validacion_calibracion | 16448 | 0.075 | 4977 | 14980 |
| validacion_umbral | 16449 | 0.075 | 5056 | 14956 |

Ningun dominio registrable aparece en dos particiones (verificado con `assert`).

## Comparacion de modelos (validacion, umbral 0.5, sin calibrar)

| Conjunto | Exactitud | Precision | Recall | F1 | ROC-AUC | PR-AUC |
|---|---|---|---|---|---|---|
| RegresionLogistica(lexicas+ngramas, C=0.3) | 0.8569 | 0.9182 | 0.5828 | 0.7130 | 0.8806 | 0.8375 |
| RegresionLogistica(lexicas+ngramas, C=1.0) | 0.8622 | 0.9011 | 0.6157 | 0.7315 | 0.8839 | 0.8429 |
| RegresionLogistica(lexicas+ngramas, C=3.0) | 0.8605 | 0.8765 | 0.6317 | 0.7342 | 0.8830 | 0.8426 |
| HistGradientBoosting(lexicas) | 0.8297 | 0.8851 | 0.5076 | 0.6452 | 0.7926 | 0.7500 |

**Elegido:** RegresionLogistica(lexicas+ngramas, C=3.0) tiene el mejor F1 entre las regresiones logisticas; HistGradientBoosting queda a -0.0890 de F1 (regla: se prefiere la Regresion Logistica salvo que la otra gane por 0.02 o mas).

## Calibracion

- Platt (sigmoide sobre el logit), ajustado en validacion_calibracion
- Brier: 0.1074 -> 0.1074
- ECE: 0.0168 -> 0.0150

## Umbral

- Umbral elegido: **0.5527**
- Criterio: mayor recall con precision >= 0.9 en validacion_umbral; en empate, el umbral mas alto
- En validacion_umbral: precision 0.9001, recall 0.6007

## Evaluacion final en PRUEBA (una sola vez)

| Conjunto | Exactitud | Precision | Recall | F1 | ROC-AUC | PR-AUC |
|---|---|---|---|---|---|---|
| prueba | 0.8955 | 0.9610 | 0.8057 | 0.8766 | 0.9413 | 0.9512 |

| | Predicho legitimo | Predicho phishing |
|---|---|---|
| **Real legitimo** | 17239 (VN) | 494 (FP) |
| **Real phishing** | 2938 (FN) | 12185 (VP) |

### Desgloses de las mismas predicciones de prueba

Unas pocas plataformas de hosting concentran muchos hosts de prueba (las tres mayores: web.app: 5527, workers.dev: 1317, glitch.me: 468) y son faciles de reconocer, asi que inflan las metricas por fila. Estos desgloses no cambian el modelo ni el umbral; solo muestran el rendimiento sin ese efecto.

| Conjunto | Exactitud | Precision | Recall | F1 | ROC-AUC | PR-AUC |
|---|---|---|---|---|---|---|
| ponderada por dominio | 0.8539 | 0.8570 | 0.4839 | 0.6186 | 0.8418 | 0.7424 |
| sin los 3 grupos mas grandes | 0.8657 | 0.9080 | 0.6240 | 0.7397 | 0.8866 | 0.8484 |

## Evaluacion boliviana (44 URL etiquetadas a mano)

Conjunto pequeno y construido a mano: sirve como prueba de humo del contexto local, no como estimacion estadistica.

| Conjunto | Exactitud | Precision | Recall | F1 | ROC-AUC | PR-AUC |
|---|---|---|---|---|---|---|
| bolivia | 0.8409 | 0.8235 | 0.7778 | 0.8000 | 0.8825 | 0.9021 |

| | Predicho legitimo | Predicho phishing |
|---|---|---|
| **Real legitimo** | 23 (VN) | 3 (FP) |
| **Real phishing** | 4 (FN) | 14 (VP) |

Aciertos por tipo:

| Tipo | Aciertos | Total |
|---|---|---|
| lista_negra | 3 | 3 |
| oficial | 23 | 26 |
| subdominio_enganoso | 4 | 4 |
| typosquatting | 1 | 5 |
| variante | 6 | 6 |

Detalle por URL:

| URL | Etiqueta | Probabilidad | Prediccion | Senales principales |
|---|---|---|---|---|
| `https://www.bnb.com.bo/` | 0 | 0.5318 | 0 | no tiene subdominios | el dominio contiene 'nb.', secuencia propia de sitios legitimos | el dominio tiene 2 puntos |
| `https://bnb.com.bo` | 0 | 0.5318 | 0 | no tiene subdominios | el dominio contiene 'nb.', secuencia propia de sitios legitimos | el dominio tiene 2 puntos |
| `https://www.bnb.com.bo/PortalBNB/Principal/BancaPersonas` | 0 | 0.5318 | 0 | no tiene subdominios | el dominio contiene 'nb.', secuencia propia de sitios legitimos | el dominio tiene 2 puntos |
| `https://www.bancounion.com.bo/` | 0 | 0.2871 | 0 | no tiene subdominios | el dominio tiene 2 puntos | el dominio no tiene digitos |
| `https://bancounion.com.bo` | 0 | 0.2871 | 0 | no tiene subdominios | el dominio tiene 2 puntos | el dominio no tiene digitos |
| `https://www.bancounion.com.bo/BancaDigital/UninetPlus` | 0 | 0.2871 | 0 | no tiene subdominios | el dominio tiene 2 puntos | el dominio no tiene digitos |
| `https://www.bancounion.com.bo/AgenciasAtm/AgenciasAtms` | 0 | 0.2871 | 0 | no tiene subdominios | el dominio tiene 2 puntos | el dominio no tiene digitos |
| `https://www.bmsc.com.bo/` | 0 | 0.4873 | 0 | no tiene subdominios | el dominio tiene 2 puntos | el dominio es corto (11 caracteres) |
| `https://bmsc.com.bo` | 0 | 0.4873 | 0 | no tiene subdominios | el dominio tiene 2 puntos | el dominio es corto (11 caracteres) |
| `https://www.bmsc.com.bo/help/questions` | 0 | 0.4873 | 0 | no tiene subdominios | el dominio tiene 2 puntos | el dominio es corto (11 caracteres) |
| `https://www.bisa.com/home` | 0 | 0.1915 | 0 | no tiene subdominios | el dominio es corto (8 caracteres) | el dominio no tiene digitos |
| `https://bisa.com` | 0 | 0.1915 | 0 | no tiene subdominios | el dominio es corto (8 caracteres) | el dominio no tiene digitos |
| `https://www.bisa.com/empresas/comercio-exterior` | 0 | 0.1915 | 0 | no tiene subdominios | el dominio es corto (8 caracteres) | el dominio no tiene digitos |
| `https://www.bg.com.bo/personas/` | 0 | 0.6287 | 1 | el dominio contiene '.bo', secuencia frecuente en phishing | el dominio contiene '.com.', secuencia frecuente en phishing | el dominio contiene 'bg', secuencia frecuente en phishing |
| `https://bg.com.bo` | 0 | 0.6287 | 1 | el dominio contiene '.bo', secuencia frecuente en phishing | el dominio contiene '.com.', secuencia frecuente en phishing | el dominio contiene 'bg', secuencia frecuente en phishing |
| `https://www.bg.com.bo/canales-digitales/cajeros-automaticos/` | 0 | 0.6287 | 1 | el dominio contiene '.bo', secuencia frecuente en phishing | el dominio contiene '.com.', secuencia frecuente en phishing | el dominio contiene 'bg', secuencia frecuente en phishing |
| `https://www.tigo.com.bo/` | 0 | 0.2965 | 0 | no tiene subdominios | el dominio tiene 2 puntos | el dominio contiene 'ti', secuencia propia de sitios legitimos |
| `https://tigo.com.bo` | 0 | 0.2965 | 0 | no tiene subdominios | el dominio tiene 2 puntos | el dominio contiene 'ti', secuencia propia de sitios legitimos |
| `https://www.impuestos.gob.bo/` | 0 | 0.0236 | 0 | no tiene subdominios | el dominio contiene '.go', secuencia propia de sitios legitimos | el dominio tiene 2 puntos |
| `https://impuestos.gob.bo` | 0 | 0.0236 | 0 | no tiene subdominios | el dominio contiene '.go', secuencia propia de sitios legitimos | el dominio tiene 2 puntos |
| `https://www.impuestos.gob.bo/index.php/memoria-intitucional/` | 0 | 0.0236 | 0 | no tiene subdominios | el dominio contiene '.go', secuencia propia de sitios legitimos | el dominio tiene 2 puntos |
| `https://www.entel.bo/` | 0 | 0.2191 | 0 | no tiene subdominios | el dominio es corto (8 caracteres) | el dominio no tiene digitos |
| `https://entel.bo` | 0 | 0.2191 | 0 | no tiene subdominios | el dominio es corto (8 caracteres) | el dominio no tiene digitos |
| `https://www.segip.gob.bo/` | 0 | 0.1290 | 0 | no tiene subdominios | el dominio contiene '.go', secuencia propia de sitios legitimos | el dominio tiene 2 puntos |
| `https://segip.gob.bo` | 0 | 0.1290 | 0 | no tiene subdominios | el dominio contiene '.go', secuencia propia de sitios legitimos | el dominio tiene 2 puntos |
| `https://www.segip.gob.bo/comunicados/` | 0 | 0.1290 | 0 | no tiene subdominios | el dominio contiene '.go', secuencia propia de sitios legitimos | el dominio tiene 2 puntos |
| `http://bnb-bo-seguro.com/login/verificar-cuenta` | 1 | 0.8468 | 1 | el dominio contiene 'seguro' | el dominio tiene 2 guiones | el dominio tiene 1 punto |
| `https://bancounion-verificacion.net/` | 1 | 0.9575 | 1 | el dominio contiene 'verificacion' | el dominio es largo (27 caracteres) | el dominio tiene 1 punto |
| `https://impuestos-gob-bo.info/verificar-nit` | 1 | 0.8733 | 1 | la extension .info se usa mucho en phishing | el dominio tiene 2 guiones | el dominio contiene '-go', secuencia frecuente en phishing |
| `https://premios-tigo-bo.xyz/sorteo` | 1 | 0.9701 | 1 | la extension .xyz se usa mucho en phishing | el dominio tiene 2 guiones | el dominio tiene 1 punto |
| `http://bnb-banca-segura.com/ingresar` | 1 | 0.9650 | 1 | el dominio contiene 'banca' | el dominio tiene 2 guiones | el dominio tiene 1 punto |
| `https://bancounion-bo-actualizacion.com/datos` | 1 | 0.7653 | 1 | el dominio es largo (31 caracteres) | el dominio tiene 2 guiones | el dominio tiene 1 punto |
| `https://bmsc-verificar-cuenta.online/` | 1 | 0.9995 | 1 | la extension .online se usa mucho en phishing | el dominio contiene 'verificar', 'cuenta' | el dominio es largo (28 caracteres) |
| `https://tigo-money-bolivia.top/premio` | 1 | 0.9892 | 1 | la extension .top se usa mucho en phishing | el dominio tiene 2 guiones | el dominio contiene '.to', secuencia frecuente en phishing |
| `https://segip-gob-bo.site/cedula` | 1 | 0.9952 | 1 | la extension .site se usa mucho en phishing | el dominio tiene 2 guiones | el dominio contiene '-go', secuencia frecuente en phishing |
| `http://bnbb.com.bo/login` | 1 | 0.7010 | 1 | el dominio contiene '.bo', secuencia frecuente en phishing | el dominio contiene '.com.', secuencia frecuente en phishing | el dominio contiene 'b.com', secuencia frecuente en phishing |
| `https://bancounlon.com.bo/` | 1 | 0.3605 | 0 | no tiene subdominios | el dominio tiene 2 puntos | el dominio no tiene digitos |
| `https://www.bmcs.com.bo/` | 1 | 0.3657 | 0 | no tiene subdominios | el dominio tiene 2 puntos | el dominio es corto (11 caracteres) |
| `https://blsa.com/banca` | 1 | 0.1868 | 0 | no tiene subdominios | el dominio es corto (8 caracteres) | el dominio contiene 'sa.c', secuencia propia de sitios legitimos |
| `https://entel-bo.com/recarga` | 1 | 0.1951 | 0 | no tiene subdominios | el dominio contiene 'el-', secuencia propia de sitios legitimos | el dominio es corto (12 caracteres) |
| `http://bnb.com.bo.verificar.cuenta.segura-login.xyz/` | 1 | 1.0000 | 1 | tiene 5 subdominios (bnb.com.bo.verificar.cuenta) | la extension .xyz se usa mucho en phishing | el dominio contiene 'login', 'verificar', 'cuenta' |
| `https://bancounion.com.bo.validacion-clientes.com/` | 1 | 1.0000 | 1 | tiene 3 subdominios (bancounion.com.bo) | el dominio es largo (41 caracteres) | el dominio contiene '.bo', secuencia frecuente en phishing |
| `https://impuestos.gob.bo.pagos-nit.net/` | 1 | 0.9987 | 1 | tiene 3 subdominios (impuestos.gob.bo) | el dominio es largo (30 caracteres) | el dominio contiene '.bo', secuencia frecuente en phishing |
| `https://www.tigo.com.bo.billetera-movil.club/` | 1 | 1.0000 | 1 | tiene 3 subdominios (tigo.com.bo) | la extension .club se usa mucho en phishing | el dominio es largo (32 caracteres) |

## Experimento de control: la misma Regresion Logistica con la URL completa

No se usa en produccion. Mide cuanto se aprovecha un modelo del sesgo del dataset (todas las legitimas son `https://www.dominio.tld` sin ruta). Umbral 0.5.

| Conjunto | Exactitud | Precision | Recall | F1 | ROC-AUC | PR-AUC |
|---|---|---|---|---|---|---|
| control - validacion | 0.9893 | 0.9975 | 0.9674 | 0.9822 | 0.9962 | 0.9953 |
| control - bolivia | 0.7273 | 0.6154 | 0.8889 | 0.7273 | 0.7799 | 0.6503 |

- Legitimas bolivianas con ruta marcadas como phishing por el control: 10 de 10
- Las mismas URL con el modelo elegido (solo host): 2 de 10

Latencia media de prediccion (con explicacion): 23.20 ms por URL.
