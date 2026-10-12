"""Descarga el PhiUSIIL Phishing URL Dataset (UCI, CC BY 4.0) a ia/datos/.

Fuente: https://archive.ics.uci.edu/dataset/967/phiusiil+phishing+url+dataset
Cita: Prasad, A. & Chandra, S. (2024). PhiUSIIL Phishing URL (Website) [Dataset].
      UCI Machine Learning Repository. https://doi.org/10.24432/C5Q90X

Segun la documentacion oficial: "Label 1 corresponds to a legitimate URL,
label 0 to a phishing URL". El resto del proyecto usa 1 = phishing, asi que
la inversion se hace en entrenar.py.

Uso:  python descargar_datos.py
"""

import hashlib
import json
import sys
import urllib.request
import zipfile
from datetime import datetime, timezone
from pathlib import Path

URL_ZIP = "https://archive.ics.uci.edu/static/public/967/phiusiil+phishing+url+dataset.zip"
CARPETA = Path(__file__).resolve().parent / "datos"
NOMBRE_CSV = "PhiUSIIL_Phishing_URL_Dataset.csv"


def sha256_de(ruta: Path) -> str:
    h = hashlib.sha256()
    with ruta.open("rb") as f:
        for bloque in iter(lambda: f.read(1 << 20), b""):
            h.update(bloque)
    return h.hexdigest()


def main() -> int:
    CARPETA.mkdir(exist_ok=True)
    ruta_zip = CARPETA / "phiusiil.zip"
    print(f"Descargando {URL_ZIP} ...")
    try:
        urllib.request.urlretrieve(URL_ZIP, ruta_zip)
    except OSError as e:
        print(f"ERROR: no se pudo descargar el dataset de UCI: {e}", file=sys.stderr)
        return 1

    with zipfile.ZipFile(ruta_zip) as z:
        # Solo se extrae el CSV esperado, por nombre exacto (nunca rutas arbitrarias del zip).
        miembros = [n for n in z.namelist() if Path(n).name == NOMBRE_CSV]
        if not miembros:
            print(f"ERROR: el zip no contiene {NOMBRE_CSV}: {z.namelist()}", file=sys.stderr)
            return 1
        ruta_csv = CARPETA / NOMBRE_CSV
        ruta_csv.write_bytes(z.read(miembros[0]))
    ruta_zip.unlink()

    info = {
        "fuente": URL_ZIP,
        "archivo": NOMBRE_CSV,
        "sha256": sha256_de(ruta_csv),
        "bytes": ruta_csv.stat().st_size,
        "descargado_utc": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "licencia": "CC BY 4.0",
    }
    (CARPETA / "dataset_info.json").write_text(json.dumps(info, indent=2), encoding="utf-8")
    print(json.dumps(info, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
