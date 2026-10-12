"""Pruebas del servicio de prediccion. Necesitan el modelo entrenado (python entrenar.py)."""

import pytest
from fastapi.testclient import TestClient

import servicio

if not (servicio.MODELO_DIR / "modelo.joblib").exists():
    pytest.skip("Falta el modelo entrenado: ejecuta python entrenar.py", allow_module_level=True)


@pytest.fixture(scope="module")
def cliente():
    with TestClient(servicio.app) as c:
        yield c


def predecir(cliente, url):
    return cliente.post("/predecir", json={"url": url})


# --- casos validos -----------------------------------------------------------

def test_respuesta_tiene_todos_los_campos(cliente):
    r = predecir(cliente, "https://www.bnb.com.bo/")
    assert r.status_code == 200
    cuerpo = r.json()
    assert set(cuerpo) == {"probabilidad_phishing", "es_phishing", "umbral", "senales", "modelo", "version"}
    assert 0.0 <= cuerpo["probabilidad_phishing"] <= 1.0
    assert cuerpo["modelo"] == "phishshield-url-ia"


def test_es_phishing_respeta_el_umbral(cliente):
    for url in ["https://www.bnb.com.bo/", "http://bnb-banca-segura.com/ingresar",
                "https://secure-login-verify-account.web.app/"]:
        cuerpo = predecir(cliente, url).json()
        assert cuerpo["es_phishing"] == (cuerpo["probabilidad_phishing"] >= cuerpo["umbral"])


def test_url_sin_esquema_es_valida(cliente):
    assert predecir(cliente, "bnb.com.bo").status_code == 200


def test_www_y_ruta_no_cambian_la_prediccion(cliente):
    # El modelo solo mira el host normalizado (ver FICHA_MODELO.md).
    a = predecir(cliente, "https://www.bnb.com.bo/").json()
    b = predecir(cliente, "http://bnb.com.bo/PortalBNB/Principal/BancaPersonas").json()
    assert a["probabilidad_phishing"] == b["probabilidad_phishing"]


# --- URL invalidas -------------------------------------------------------------

@pytest.mark.parametrize("cuerpo", [
    {"url": ""},
    {"url": "   "},
    {"url": "https://" + "a" * 3000 + ".com"},
    {"url": "http://"},
    {"url": "no es una url"},
    {},
    {"url": None},
    {"url": 123},
])
def test_entradas_invalidas_dan_422(cliente, cuerpo):
    assert cliente.post("/predecir", json=cuerpo).status_code == 422


# --- salud y modelo ---------------------------------------------------------------

def test_salud(cliente):
    r = cliente.get("/salud")
    assert r.status_code == 200
    assert r.json()["estado"] == "ok"


def test_modelo_devuelve_metadata_sin_rutas_internas(cliente):
    r = cliente.get("/modelo")
    assert r.status_code == 200
    md = r.json()
    assert md["version"] == "1.0.0"
    assert "metricas_prueba" in md and "umbral" in md
    texto = r.text
    assert ":\\\\" not in texto and "C:/" not in texto and "/home/" not in texto


# --- explicabilidad -------------------------------------------------------------

def test_devuelve_entre_3_y_5_senales_con_texto(cliente):
    for url in ["https://www.bnb.com.bo/", "http://bnb.com.bo.verificar.cuenta.segura-login.xyz/"]:
        senales = predecir(cliente, url).json()["senales"]
        assert 3 <= len(senales) <= 5
        for s in senales:
            assert s["texto"] and s["direccion"] in {"phishing", "legitimo"}


def test_senales_apuntan_al_veredicto(cliente):
    cuerpo = predecir(cliente, "http://bnb.com.bo.verificar.cuenta.segura-login.xyz/").json()
    assert cuerpo["es_phishing"]
    assert cuerpo["senales"][0]["direccion"] == "phishing"
    assert cuerpo["senales"][0]["aporte"] > 0


def test_senales_mencionan_rasgos_concretos(cliente):
    cuerpo = predecir(cliente, "http://bnb.com.bo.verificar.cuenta.segura-login.xyz/").json()
    textos = " ".join(s["texto"] for s in cuerpo["senales"])
    assert any(p in textos for p in ["subdominio", ".xyz", "guion", "punto", "contiene"])
