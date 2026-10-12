"""Pruebas de caracteristicas.py: no necesitan modelo ni red."""

import caracteristicas as c


def test_normaliza_esquema_www_puerto_y_mayusculas():
    u = c.normalizar("HTTPS://WWW.BNB.com.bo:443/Login?x=1")
    assert u.host == "bnb.com.bo"
    assert u.ruta == "/login?x=1"
    assert u.esquema == "https"
    assert u.registrable == "bnb.com.bo"
    assert u.sufijo == "com.bo"


def test_acepta_url_sin_esquema():
    assert c.normalizar("bancounion.com.bo/x").host == "bancounion.com.bo"


def test_dominio_registrable_ignora_subdominios():
    u = c.normalizar("http://bnb.com.bo.verificar.segura-login.xyz/")
    assert u.registrable == "segura-login.xyz"
    assert c.lexicas(u)["subdominios"] == 4


def test_ip_literal():
    u = c.normalizar("http://192.168.1.50/banca/login")
    feats = c.lexicas(u)
    assert feats["usa_ip"] == 1
    assert u.registrable == "192.168.1.50"


def test_tld_abusado_y_palabras_sensibles():
    feats = c.lexicas(c.normalizar("https://secure-login.xyz/verificar"))
    assert feats["tld_abusado"] == 1
    assert feats["palabras_sensibles_host"] == 2   # secure, login
    assert feats["palabras_sensibles_ruta"] == 1   # verificar
    assert feats["guiones_host"] == 1


def test_entropia():
    assert c.entropia("") == 0.0
    assert c.entropia("aaaa") == 0.0
    assert abs(c.entropia("ab") - 1.0) < 1e-9


def test_url_sin_host():
    assert c.normalizar("http://").host == ""
