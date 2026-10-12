"""Entrena, calibra y evalua el modelo de phishing por URL.

Pasos (todos con la semilla SEMILLA):
  1. Carga PhiUSIIL y pone phishing = 1 (en el original 0 es phishing).
  2. Limpieza: solo la columna URL, sin duplicados, un registro por host.
  3. Division 70/15/15 agrupada por dominio registrable (sin fuga).
  4. Compara Regresion Logistica (lexicas + n-gramas) y HistGradientBoosting
     (lexicas) en validacion; elige por F1 y PR-AUC.
  5. Calibra (Platt) con una mitad de validacion y elige el umbral con la otra.
  6. Evalua UNA sola vez en prueba, y aparte en el conjunto boliviano.
  7. Experimento de control: el mismo modelo con la URL completa, para medir
     el sesgo del dataset (no se usa en produccion).
  8. Guarda modelo, metadata.json y reporte_metricas.md.

Uso:  python entrenar.py
"""

from __future__ import annotations

import json
import platform
import sys
import time
from datetime import date, datetime, timezone
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
import sklearn
from scipy import sparse
from sklearn.ensemble import HistGradientBoostingClassifier
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (accuracy_score, average_precision_score, brier_score_loss,
                             confusion_matrix, f1_score, precision_recall_curve,
                             precision_score, recall_score, roc_auc_score)
from sklearn.preprocessing import StandardScaler

import caracteristicas as c
from modelo import Detector, sigmoide

SEMILLA = 42
VERSION = "1.0.0"
NOMBRE = "phishshield-url-ia"
PRECISION_MINIMA = 0.90
MARGEN_F1_PREFERIR_LR = 0.02
MAX_NGRAMAS = 20_000

RAIZ = Path(__file__).resolve().parent
CSV = RAIZ / "datos" / "PhiUSIIL_Phishing_URL_Dataset.csv"
INFO_DATASET = RAIZ / "datos" / "dataset_info.json"
BOLIVIA = RAIZ / "datos_evaluacion" / "bolivia.csv"
CARPETA_MODELO = RAIZ / "modelos" / VERSION
REPORTE = RAIZ / "reporte_metricas.md"


def log(msg: str) -> None:
    print(f"[{datetime.now():%H:%M:%S}] {msg}", flush=True)


# --- 1 y 2: carga y limpieza --------------------------------------------------

def cargar_y_limpiar() -> tuple[pd.DataFrame, dict]:
    df = pd.read_csv(CSV, usecols=["URL", "label"])
    assert set(df["label"].unique()) == {0, 1}
    # Documentacion UCI: "Label 1 corresponds to a legitimate URL, label 0 to a phishing URL".
    df["y"] = 1 - df["label"]
    pasos = {"filas_originales": len(df),
             "balance_original": df["y"].value_counts().sort_index().to_dict()}

    df = df.drop_duplicates(subset="URL")
    pasos["tras_quitar_url_duplicadas"] = len(df)

    norm = [c.normalizar(u) for u in df["URL"]]
    df["host"] = [u.host for u in norm]
    df["grupo"] = [u.registrable for u in norm]
    df["_u"] = norm
    df = df[df["host"] != ""]
    pasos["tras_quitar_host_vacio"] = len(df)

    # Un mismo host con las dos etiquetas no se puede aprender solo con el host.
    etiquetas_por_host = df.groupby("host")["y"].nunique()
    conflictivos = set(etiquetas_por_host[etiquetas_por_host > 1].index)
    df = df[~df["host"].isin(conflictivos)]
    pasos["hosts_con_etiqueta_contradictoria"] = len(conflictivos)
    pasos["tras_quitar_contradictorios"] = len(df)

    # El modelo solo ve el host: dos filas con el mismo host son la misma entrada.
    df = df.drop_duplicates(subset="host").reset_index(drop=True)
    pasos["tras_un_registro_por_host"] = len(df)
    pasos["balance_final"] = df["y"].value_counts().sort_index().to_dict()
    pasos["dominios_registrables"] = int(df["grupo"].nunique())
    return df, pasos


# --- 3: division agrupada -----------------------------------------------------

def dividir_por_grupo(grupos: pd.Series, fracciones: list[float], semilla: int) -> np.ndarray:
    """Asigna cada grupo entero a una particion, buscando las fracciones en filas.

    Equivale a GroupShuffleSplit, pero GroupShuffleSplit reparte por cantidad de
    grupos y aqui los grupos tienen tamanos muy distintos (web.app tiene miles de
    hosts), asi que se reparte por cantidad de filas.
    """
    tamanos = grupos.value_counts()
    orden = np.random.default_rng(semilla).permutation(tamanos.index.to_numpy())
    acumulado = np.cumsum(tamanos.loc[orden].to_numpy()) / len(grupos)
    cortes = np.cumsum(fracciones)[:-1]
    particion_de_grupo = dict(zip(orden, np.searchsorted(cortes, acumulado, side="left")))
    return grupos.map(particion_de_grupo).to_numpy()


# --- metricas -----------------------------------------------------------------

def metricas(y, prob, umbral, peso=None) -> dict:
    """Con `peso`, la matriz de confusion queda en filas ponderadas (no enteras)."""
    pred = (prob >= umbral).astype(int)
    tn, fp, fn, tp = confusion_matrix(y, pred, labels=[0, 1], sample_weight=peso).ravel()
    entero = int if peso is None else float
    return {
        "exactitud": accuracy_score(y, pred, sample_weight=peso),
        "precision": precision_score(y, pred, zero_division=0, sample_weight=peso),
        "recall": recall_score(y, pred, zero_division=0, sample_weight=peso),
        "f1": f1_score(y, pred, zero_division=0, sample_weight=peso),
        "roc_auc": roc_auc_score(y, prob, sample_weight=peso) if len(set(y)) == 2 else float("nan"),
        "pr_auc": average_precision_score(y, prob, sample_weight=peso) if len(set(y)) == 2 else float("nan"),
        "matriz_confusion": {"vn": entero(tn), "fp": entero(fp), "fn": entero(fn), "vp": entero(tp)},
        "n": int(len(y)),
    }


def ece(y, prob, bins=10) -> float:
    """Error de calibracion esperado: diferencia media entre confianza y acierto."""
    cortes = np.linspace(0, 1, bins + 1)
    idx = np.clip(np.digitize(prob, cortes[1:-1]), 0, bins - 1)
    total = 0.0
    for b in range(bins):
        m = idx == b
        if m.any():
            total += m.mean() * abs(prob[m].mean() - y[m].mean())
    return float(total)


def redondear(d):
    if isinstance(d, dict):
        return {k: redondear(v) for k, v in d.items()}
    if isinstance(d, (float, np.floating)):
        return round(float(d), 4)
    if isinstance(d, (np.integer,)):
        return int(d)
    return d


# --- modelos ------------------------------------------------------------------

def nuevo_detector() -> Detector:
    vect = TfidfVectorizer(analyzer="char_wb", ngram_range=(3, 5), max_features=MAX_NGRAMAS,
                           min_df=5, sublinear_tf=True, lowercase=False, dtype=np.float32)
    return Detector(vectorizador=vect, escalador=StandardScaler(), clasificador=None)


def ajustar_detector(det: Detector, urls_ent, y_ent, C: float) -> Detector:
    lex = np.array([[c.lexicas(u)[n] for n in det.nombres_lexicas] for u in urls_ent], dtype=float)
    det.escalador.fit(lex)
    det.vectorizador.fit([c.texto_host(u) for u in urls_ent])
    clf = LogisticRegression(C=C, max_iter=3000, random_state=SEMILLA)
    clf.fit(det.matriz(urls_ent), y_ent)
    det.clasificador = clf
    return det


def matriz_lexica(urls, nombres) -> np.ndarray:
    return np.array([[c.lexicas(u)[n] for n in nombres] for u in urls], dtype=float)


def ajustar_platt(logits, y) -> tuple[float, float]:
    lr = LogisticRegression(C=1e6, max_iter=1000)
    lr.fit(logits.reshape(-1, 1), y)
    return float(lr.coef_[0, 0]), float(lr.intercept_[0])


def elegir_umbral(y, prob) -> tuple[float, dict]:
    """Mayor recall con precision >= PRECISION_MINIMA. Empate: el umbral mas alto."""
    prec, rec, umbrales = precision_recall_curve(y, prob)
    prec, rec = prec[:-1], rec[:-1]  # el ultimo punto no tiene umbral
    validos = np.where(prec >= PRECISION_MINIMA)[0]
    if len(validos) == 0:
        raise RuntimeError(f"Ningun umbral alcanza precision {PRECISION_MINIMA} en validacion")
    mejor_recall = rec[validos].max()
    candidatos = validos[rec[validos] == mejor_recall]
    i = candidatos[np.argmax(umbrales[candidatos])]
    return float(umbrales[i]), {"precision_en_validacion": float(prec[i]),
                                "recall_en_validacion": float(rec[i])}


# --- experimento de control: URL completa -------------------------------------

def control_url_completa(urls_ent, y_ent, conjuntos: dict) -> dict:
    nombres = c.CARACTERISTICAS_HOST + c.CARACTERISTICAS_URL
    esc = StandardScaler().fit(matriz_lexica(urls_ent, nombres))
    vect = TfidfVectorizer(analyzer="char_wb", ngram_range=(3, 5), max_features=MAX_NGRAMAS,
                           min_df=5, sublinear_tf=True, lowercase=False, dtype=np.float32)
    vect.fit([c.texto_url_completa(u) for u in urls_ent])

    def X(urls):
        return sparse.hstack([sparse.csr_matrix(esc.transform(matriz_lexica(urls, nombres))),
                              vect.transform([c.texto_url_completa(u) for u in urls])], format="csr")

    clf = LogisticRegression(C=1.0, max_iter=3000, random_state=SEMILLA).fit(X(urls_ent), y_ent)
    return {nombre: metricas(y, clf.predict_proba(X(urls))[:, 1], 0.5)
            for nombre, (urls, y) in conjuntos.items()}


# --- principal ----------------------------------------------------------------

def main() -> int:
    inicio = time.time()
    if not CSV.exists():
        print("Falta el dataset. Ejecuta primero: python descargar_datos.py", file=sys.stderr)
        return 1
    info_dataset = json.loads(INFO_DATASET.read_text(encoding="utf-8"))

    log("Cargando y limpiando PhiUSIIL...")
    df, limpieza = cargar_y_limpiar()
    log(f"Limpieza: {limpieza}")

    df["particion"] = dividir_por_grupo(df["grupo"], [0.70, 0.15, 0.15], SEMILLA)
    ent, val, pru = (df[df["particion"] == i] for i in range(3))
    # Validacion se parte en dos mitades, tambien por grupo:
    # una para calibrar y otra para elegir el umbral.
    mitad = dividir_por_grupo(val["grupo"], [0.5, 0.5], SEMILLA + 1)
    val_cal, val_umb = val[mitad == 0], val[mitad == 1]

    for a, b in [(ent, val), (ent, pru), (val, pru), (val_cal, val_umb)]:
        assert not set(a["grupo"]) & set(b["grupo"]), "Fuga: un dominio en dos particiones"
    division = {nombre: {"filas": len(p), "fraccion": len(p) / len(df),
                         "phishing": int(p["y"].sum()), "dominios": int(p["grupo"].nunique())}
                for nombre, p in [("entrenamiento", ent), ("validacion", val), ("prueba", pru),
                                  ("validacion_calibracion", val_cal), ("validacion_umbral", val_umb)]}
    log(f"Division: { {k: v['filas'] for k, v in division.items()} } (sin dominios compartidos)")

    u_ent, y_ent = list(ent["_u"]), ent["y"].to_numpy()
    u_val, y_val = list(val["_u"]), val["y"].to_numpy()

    # --- 4. comparacion de modelos en validacion ---
    candidatos = {}
    detectores = {}
    for C in (0.3, 1.0, 3.0):
        t = time.time()
        det = ajustar_detector(nuevo_detector(), u_ent, y_ent, C)
        prob = sigmoide(det.logit(u_val))
        nombre = f"RegresionLogistica(lexicas+ngramas, C={C})"
        candidatos[nombre] = metricas(y_val, prob, 0.5) | {"segundos_entrenamiento": time.time() - t}
        detectores[nombre] = det
        log(f"{nombre}: F1={candidatos[nombre]['f1']:.4f} PR-AUC={candidatos[nombre]['pr_auc']:.4f}")

    t = time.time()
    hgb = HistGradientBoostingClassifier(max_iter=300, random_state=SEMILLA)
    hgb.fit(matriz_lexica(u_ent, c.CARACTERISTICAS_HOST), y_ent)
    prob_hgb = hgb.predict_proba(matriz_lexica(u_val, c.CARACTERISTICAS_HOST))[:, 1]
    nombre_hgb = "HistGradientBoosting(lexicas)"
    candidatos[nombre_hgb] = metricas(y_val, prob_hgb, 0.5) | {"segundos_entrenamiento": time.time() - t}
    log(f"{nombre_hgb}: F1={candidatos[nombre_hgb]['f1']:.4f} PR-AUC={candidatos[nombre_hgb]['pr_auc']:.4f}")

    mejor_lr = max((n for n in candidatos if n.startswith("Regresion")), key=lambda n: candidatos[n]["f1"])
    dif_f1 = candidatos[nombre_hgb]["f1"] - candidatos[mejor_lr]["f1"]
    if dif_f1 >= MARGEN_F1_PREFERIR_LR:
        print(f"HistGradientBoosting supera a la Regresion Logistica por {dif_f1:.4f} de F1. "
              "Este script solo sabe explicar la Regresion Logistica: revisar antes de seguir.",
              file=sys.stderr)
        return 2
    elegido = mejor_lr
    motivo = (f"{mejor_lr} tiene el mejor F1 entre las regresiones logisticas; "
              f"HistGradientBoosting queda a {dif_f1:+.4f} de F1 "
              f"(regla: se prefiere la Regresion Logistica salvo que la otra gane por 0.02 o mas).")
    log(f"Elegido: {motivo}")
    det = detectores[elegido]

    # --- 5. calibracion y umbral ---
    u_cal, y_cal = list(val_cal["_u"]), val_cal["y"].to_numpy()
    u_umb, y_umb = list(val_umb["_u"]), val_umb["y"].to_numpy()
    logit_umb = det.logit(u_umb)
    sin_calibrar = sigmoide(logit_umb)
    det.calibracion = ajustar_platt(det.logit(u_cal), y_cal)
    calibrada = det.probabilidades(u_umb)
    calibracion = {
        "metodo": "Platt (sigmoide sobre el logit), ajustado en validacion_calibracion",
        "a": det.calibracion[0], "b": det.calibracion[1],
        "brier_antes": brier_score_loss(y_umb, sin_calibrar),
        "brier_despues": brier_score_loss(y_umb, calibrada),
        "ece_antes": ece(y_umb, sin_calibrar), "ece_despues": ece(y_umb, calibrada),
    }
    det.umbral, info_umbral = elegir_umbral(y_umb, calibrada)
    info_umbral["umbral"] = det.umbral
    info_umbral["criterio"] = (f"mayor recall con precision >= {PRECISION_MINIMA} en validacion_umbral; "
                               "en empate, el umbral mas alto")
    log(f"Calibracion: {redondear(calibracion)}")
    log(f"Umbral: {redondear(info_umbral)}")

    # --- 6. evaluacion final: UNA sola vez sobre prueba ---
    u_pru, y_pru = list(pru["_u"]), pru["y"].to_numpy()
    prob_pru = det.probabilidades(u_pru)
    prueba = metricas(y_pru, prob_pru, det.umbral)
    log(f"PRUEBA: {redondear(prueba)}")
    # Desgloses de ESTAS MISMAS predicciones (no cambian el modelo ni el umbral).
    # Unas pocas plataformas (web.app, workers.dev...) aportan miles de hosts y
    # dominan las metricas por fila; aqui cada dominio registrable pesa lo mismo.
    tam_grupo = pru["grupo"].map(pru["grupo"].value_counts()).to_numpy()
    grandes = list(pru["grupo"].value_counts().index[:3])
    sin_grandes = ~pru["grupo"].isin(grandes).to_numpy()
    prueba_desgloses = {
        "ponderada_por_dominio": metricas(y_pru, prob_pru, det.umbral, peso=1.0 / tam_grupo),
        "sin_3_grupos_mas_grandes": metricas(y_pru[sin_grandes], prob_pru[sin_grandes], det.umbral),
        "3_grupos_mas_grandes": {g: int(n) for g, n in pru["grupo"].value_counts().head(3).items()},
    }
    log(f"PRUEBA desgloses: {redondear(prueba_desgloses)}")

    # Evaluacion boliviana (aparte)
    bo = pd.read_csv(BOLIVIA)
    t = time.time()
    preds = [det.predecir(u) for u in bo["url"]]
    ms_por_url = (time.time() - t) * 1000 / len(bo)
    bo["probabilidad"] = [round(p.probabilidad, 4) for p in preds]
    bo["prediccion"] = [int(p.es_phishing) for p in preds]
    bo["senales"] = [" | ".join(s.texto for s in p.senales[:3]) for p in preds]
    bolivia = metricas(bo["etiqueta"].to_numpy(), bo["probabilidad"].to_numpy(), det.umbral)
    bolivia_por_tipo = (bo.assign(acierto=bo["prediccion"] == bo["etiqueta"])
                        .groupby("tipo")["acierto"].agg(["sum", "count"]).to_dict("index"))
    bo.to_csv(RAIZ / "datos_evaluacion" / "bolivia_resultados.csv", index=False)
    log(f"BOLIVIA: {redondear(bolivia)}")

    # --- 7. experimento de control ---
    log("Experimento de control (URL completa)...")
    bo_u = [c.normalizar(u) for u in bo["url"]]
    control = control_url_completa(u_ent, y_ent, {
        "validacion": (u_val, y_val),
        "bolivia": (bo_u, bo["etiqueta"].to_numpy()),
        "bolivia_solo_legitimas_con_ruta": (
            [u for u, e in zip(bo_u, bo["etiqueta"]) if e == 0 and u.ruta],
            np.array([0] * sum(1 for u, e in zip(bo_u, bo["etiqueta"]) if e == 0 and u.ruta))),
    })
    # El mismo subconjunto con el modelo elegido, para comparar
    legit_ruta = bo[(bo["etiqueta"] == 0) & bo["url"].map(lambda x: bool(c.normalizar(x).ruta))]
    control["modelo_elegido_en_legitimas_con_ruta"] = {
        "falsos_positivos": int(legit_ruta["prediccion"].sum()), "n": len(legit_ruta)}
    log(f"Control: {redondear(control)}")

    # --- 8. guardar ---
    CARPETA_MODELO.mkdir(parents=True, exist_ok=True)
    joblib.dump(det, CARPETA_MODELO / "modelo.joblib", compress=3)
    metadata = redondear({
        "nombre": NOMBRE,
        "version": VERSION,
        "fecha_entrenamiento": date.today().isoformat(),
        "algoritmo": "Regresion Logistica + calibracion Platt",
        "modelo_elegido": elegido,
        "motivo_eleccion": motivo,
        "dataset": {"nombre": "PhiUSIIL Phishing URL Dataset (UCI, CC BY 4.0)",
                    "fuente": info_dataset["fuente"], "sha256": info_dataset["sha256"],
                    "etiqueta_original": "1 = legitima, 0 = phishing (se invierte: 1 = phishing)"},
        "entrada_del_modelo": "host normalizado (sin esquema, sin 'www.', sin puerto ni ruta)",
        "caracteristicas": {"lexicas": c.CARACTERISTICAS_HOST,
                            "ngramas": f"TF-IDF char_wb 3-5, maximo {MAX_NGRAMAS}, min_df 5",
                            "ngramas_en_vocabulario": len(det.vectorizador.vocabulary_)},
        "semilla": SEMILLA,
        "limpieza": limpieza,
        "division": division,
        "comparacion_validacion": candidatos,
        "calibracion": calibracion,
        "umbral": det.umbral,
        "umbral_detalle": info_umbral,
        "metricas_prueba": prueba,
        "metricas_prueba_desgloses": prueba_desgloses,
        "metricas_bolivia": bolivia,
        "bolivia_aciertos_por_tipo": bolivia_por_tipo,
        "experimento_control_url_completa": control,
        "latencia_ms_por_url": ms_por_url,
        "entorno": {"python": platform.python_version(), "scikit_learn": sklearn.__version__,
                    "numpy": np.__version__, "pandas": pd.__version__},
        "segundos_totales": time.time() - inicio,
    })
    (CARPETA_MODELO / "metadata.json").write_text(json.dumps(metadata, indent=2, ensure_ascii=False),
                                                  encoding="utf-8")
    REPORTE.write_text(reporte_markdown(metadata, bo), encoding="utf-8")
    log(f"Modelo guardado en {CARPETA_MODELO.relative_to(RAIZ)} "
        f"({(CARPETA_MODELO / 'modelo.joblib').stat().st_size / 1e6:.1f} MB)")
    return 0


# --- reporte ------------------------------------------------------------------

def _fila(nombre, m):
    return (f"| {nombre} | {m['exactitud']:.4f} | {m['precision']:.4f} | {m['recall']:.4f} | "
            f"{m['f1']:.4f} | {m['roc_auc']:.4f} | {m['pr_auc']:.4f} |")


def _matriz(m):
    mc = m["matriz_confusion"]
    return ("| | Predicho legitimo | Predicho phishing |\n|---|---|---|\n"
            f"| **Real legitimo** | {mc['vn']} (VN) | {mc['fp']} (FP) |\n"
            f"| **Real phishing** | {mc['fn']} (FN) | {mc['vp']} (VP) |")


def reporte_markdown(md: dict, bo: pd.DataFrame) -> str:
    cab = "| Conjunto | Exactitud | Precision | Recall | F1 | ROC-AUC | PR-AUC |\n|---|---|---|---|---|---|---|"
    L = [f"# Reporte de metricas - {md['nombre']} {md['version']}", "",
         f"Generado automaticamente por `entrenar.py` el {md['fecha_entrenamiento']}. "
         "No editar a mano: volver a ejecutar el script.", "",
         f"- Dataset: {md['dataset']['nombre']}, SHA-256 `{md['dataset']['sha256']}`",
         f"- Entrada del modelo: {md['entrada_del_modelo']}",
         f"- Semilla: {md['semilla']}. Entorno: {md['entorno']}", "",
         "## Limpieza", "", "| Paso | Valor |", "|---|---|"]
    L += [f"| {k} | {v} |" for k, v in md["limpieza"].items()]
    L += ["", "Balance: 0 = legitimo, 1 = phishing.", "",
          "## Division (agrupada por dominio registrable)", "",
          "| Particion | Filas | Fraccion | Phishing | Dominios |", "|---|---|---|---|---|"]
    L += [f"| {k} | {v['filas']} | {v['fraccion']:.3f} | {v['phishing']} | {v['dominios']} |"
          for k, v in md["division"].items()]
    L += ["", "Ningun dominio registrable aparece en dos particiones (verificado con `assert`).", "",
          "## Comparacion de modelos (validacion, umbral 0.5, sin calibrar)", "", cab]
    L += [_fila(k, v) for k, v in md["comparacion_validacion"].items()]
    L += ["", f"**Elegido:** {md['motivo_eleccion']}", "",
          "## Calibracion", "", f"- {md['calibracion']['metodo']}",
          f"- Brier: {md['calibracion']['brier_antes']:.4f} -> {md['calibracion']['brier_despues']:.4f}",
          f"- ECE: {md['calibracion']['ece_antes']:.4f} -> {md['calibracion']['ece_despues']:.4f}", "",
          "## Umbral", "", f"- Umbral elegido: **{md['umbral']:.4f}**",
          f"- Criterio: {md['umbral_detalle']['criterio']}",
          f"- En validacion_umbral: precision {md['umbral_detalle']['precision_en_validacion']:.4f}, "
          f"recall {md['umbral_detalle']['recall_en_validacion']:.4f}", "",
          "## Evaluacion final en PRUEBA (una sola vez)", "", cab,
          _fila("prueba", md["metricas_prueba"]), "", _matriz(md["metricas_prueba"]), "",
          "### Desgloses de las mismas predicciones de prueba", "",
          "Unas pocas plataformas de hosting aportan miles de hosts cada una "
          f"({', '.join(f'{g}: {n}' for g, n in md['metricas_prueba_desgloses']['3_grupos_mas_grandes'].items())}) "
          "y son faciles de reconocer, asi que inflan las metricas por fila. Estos desgloses "
          "no cambian el modelo ni el umbral; solo muestran el rendimiento sin ese efecto.", "", cab,
          _fila("ponderada por dominio", md["metricas_prueba_desgloses"]["ponderada_por_dominio"]),
          _fila("sin los 3 grupos mas grandes", md["metricas_prueba_desgloses"]["sin_3_grupos_mas_grandes"]), "",
          "## Evaluacion boliviana (44 URL etiquetadas a mano)", "",
          "Conjunto pequeno y construido a mano: sirve como prueba de humo del contexto local, "
          "no como estimacion estadistica.", "", cab, _fila("bolivia", md["metricas_bolivia"]), "",
          _matriz(md["metricas_bolivia"]), "", "Aciertos por tipo:", "", "| Tipo | Aciertos | Total |",
          "|---|---|---|"]
    L += [f"| {k} | {v['sum']} | {v['count']} |" for k, v in md["bolivia_aciertos_por_tipo"].items()]
    L += ["", "Detalle por URL:", "", "| URL | Etiqueta | Probabilidad | Prediccion | Senales principales |",
          "|---|---|---|---|---|"]
    L += [f"| `{r.url}` | {r.etiqueta} | {r.probabilidad:.4f} | {r.prediccion} | {r.senales} |"
          for r in bo.itertuples()]
    ctl = md["experimento_control_url_completa"]
    L += ["", "## Experimento de control: la misma Regresion Logistica con la URL completa", "",
          "No se usa en produccion. Mide cuanto se aprovecha un modelo del sesgo del dataset "
          "(todas las legitimas son `https://www.dominio.tld` sin ruta). Umbral 0.5.", "", cab,
          _fila("control - validacion", ctl["validacion"]), _fila("control - bolivia", ctl["bolivia"]), "",
          f"- Legitimas bolivianas con ruta marcadas como phishing por el control: "
          f"{ctl['bolivia_solo_legitimas_con_ruta']['matriz_confusion']['fp']} de "
          f"{ctl['bolivia_solo_legitimas_con_ruta']['n']}",
          f"- Las mismas URL con el modelo elegido (solo host): "
          f"{ctl['modelo_elegido_en_legitimas_con_ruta']['falsos_positivos']} de "
          f"{ctl['modelo_elegido_en_legitimas_con_ruta']['n']}", "",
          f"Latencia media de prediccion (con explicacion): {md['latencia_ms_por_url']:.2f} ms por URL.", ""]
    return "\n".join(L)


if __name__ == "__main__":
    sys.exit(main())
