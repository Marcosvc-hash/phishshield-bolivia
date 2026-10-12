"""Evalua el modelo entrenado sobre un CSV con columnas url,etiqueta (1 = phishing).

Escribe <nombre>_resultados.csv al lado del original, con la probabilidad, el
veredicto segun el umbral, las 3 senales principales y si el host ya estaba en
PhiUSIIL (en ese caso el modelo pudo haberlo visto al entrenar).

Uso:  python evaluar_conjunto.py datos_evaluacion\bolivia_legitimos_fuera_lista_blanca.csv
"""

import sys
from pathlib import Path

import joblib
import pandas as pd

import caracteristicas as c

RAIZ = Path(__file__).resolve().parent
MODELO = RAIZ / "modelos" / "1.0.0" / "modelo.joblib"
DATASET = RAIZ / "datos" / "PhiUSIIL_Phishing_URL_Dataset.csv"


def main(ruta_csv: str) -> int:
    det = joblib.load(MODELO)
    df = pd.read_csv(ruta_csv)
    preds = [det.predecir(u) for u in df["url"]]
    df["host"] = [c.normalizar(u).host for u in df["url"]]
    df["probabilidad"] = [round(p.probabilidad, 4) for p in preds]
    df["prediccion"] = [int(p.es_phishing) for p in preds]
    df["senales"] = [" | ".join(s.texto for s in p.senales[:3]) for p in preds]
    if DATASET.exists():
        hosts = {c.normalizar(u).host for u in pd.read_csv(DATASET, usecols=["URL"])["URL"]}
        df["host_en_phiusiil"] = df["host"].isin(hosts).astype(int)
    salida = Path(ruta_csv).with_name(Path(ruta_csv).stem + "_resultados.csv")
    df.to_csv(salida, index=False)
    print(df[["url", "probabilidad", "prediccion"] +
             (["host_en_phiusiil"] if "host_en_phiusiil" in df else [])].to_string(index=False))
    print(f"\numbral {det.umbral:.4f}; falsos positivos (etiqueta 0, prediccion 1): "
          f"{int(((df['etiqueta'] == 0) & (df['prediccion'] == 1)).sum())} de {int((df['etiqueta'] == 0).sum())}")
    print(f"Guardado en {salida}")
    return 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(__doc__)
        sys.exit(1)
    sys.exit(main(sys.argv[1]))
