"""Caracteristicas de una URL, calculadas sin usar la red.

Lo usan tanto entrenar.py como servicio.py: asi el modelo ve en produccion
exactamente lo mismo que vio al entrenar.

Decision importante (ver FICHA_MODELO.md, "Sesgo del dataset"):
en PhiUSIIL todas las URL legitimas tienen la forma https://www.dominio.tld,
sin ruta. Por eso el modelo SOLO usa caracteristicas del host normalizado
(sin esquema, sin "www.", sin puerto). Las caracteristicas de ruta y de https
se calculan igual (CARACTERISTICAS_URL), pero solo se usan en el experimento
de control que demuestra el sesgo; en produccion las cubren las reglas del
backend (URL_RUTA_SENSIBLE y URL_SIN_HTTPS).
"""

from __future__ import annotations

import ipaddress
import math
import re
import unicodedata
from collections import Counter
from dataclasses import dataclass
from functools import lru_cache
from urllib.parse import urlsplit

import tldextract

# Lista de sufijos publicos incluida en el paquete: no descarga nada de internet.
_EXTRACTOR = tldextract.TLDExtract(suffix_list_urls=(), cache_dir=None)

# Mismas extensiones que ReglaTldSospechoso del backend.
TLDS_ABUSADOS = frozenset({
    "xyz", "top", "info", "tk", "ml", "ga", "cf", "gq",
    "club", "online", "site", "click", "link", "work", "loan",
})

# Palabras de ReglaRutaSensible del backend, mas las
# equivalentes en ingles que abundan en campanas internacionales.
PALABRAS_SENSIBLES = (
    "login", "signin", "verificar", "verificacion", "verify", "cuenta", "account",
    "banca", "banking", "password", "contrasena", "clave", "pin", "token",
    "secure", "seguro", "update", "actualizar", "confirm", "wallet", "support",
)

ESPECIALES = re.compile(r"[^a-z0-9.\-]")


@dataclass(frozen=True)
class UrlNormalizada:
    original: str
    esquema: str
    host: str          # minusculas, sin "www.", sin puerto ni punto final
    ruta: str          # ruta + consulta, en minusculas
    registrable: str   # dominio registrable (bnb.com.bo); el host si es una IP
    subdominio: str
    sufijo: str        # sufijo publico: "com.bo", "com", ...


def normalizar(url: str) -> UrlNormalizada:
    """Parsea la URL sin tocar la red. Acepta URL sin esquema ("bnb.com.bo/x")."""
    texto = url.strip()
    if "://" not in texto:
        texto = "http://" + texto
    try:
        partes = urlsplit(texto)
        host = (partes.hostname or "").rstrip(".")
    except ValueError:
        partes, host = None, ""
    if host.startswith("www."):
        host = host[4:]
    ruta = ""
    if partes is not None:
        ruta = partes.path + (("?" + partes.query) if partes.query else "")
    ruta = ruta.lower()
    if ruta == "/":
        ruta = ""
    esquema = partes.scheme.lower() if partes is not None else ""

    if _es_ip(host):
        registrable, subdominio, sufijo = host, "", ""
    else:
        ext = _extraer(host)
        registrable = ext.top_domain_under_public_suffix or host
        subdominio, sufijo = ext.subdomain, ext.suffix
    return UrlNormalizada(url, esquema, host, ruta, registrable, subdominio, sufijo)


@lru_cache(maxsize=200_000)
def _extraer(host: str):
    return _EXTRACTOR(host)


def _es_ip(host: str) -> bool:
    try:
        ipaddress.ip_address(host.strip("[]"))
        return True
    except ValueError:
        return False


def entropia(texto: str) -> float:
    """Entropia de Shannon en bits por caracter (alta = parece aleatorio)."""
    if not texto:
        return 0.0
    n = len(texto)
    return -sum((c / n) * math.log2(c / n) for c in Counter(texto).values())


def _palabras_en(texto: str) -> list[str]:
    sin_tildes = "".join(ch for ch in unicodedata.normalize("NFD", texto)
                         if unicodedata.category(ch) != "Mn")
    return [p for p in PALABRAS_SENSIBLES if p in sin_tildes]


# --- Caracteristicas lexicas -------------------------------------------------

# Las que usa el modelo: solo dependen del host.
CARACTERISTICAS_HOST = [
    "largo_host", "puntos_host", "guiones_host", "digitos_host", "especiales_host",
    "subdominios", "usa_ip", "tld_abusado", "entropia_host", "palabras_sensibles_host",
]

# Solo para el experimento de control: dependen de la URL completa.
CARACTERISTICAS_URL = [
    "largo_url", "largo_ruta", "usa_https", "palabras_sensibles_ruta",
    "puntos_url", "guiones_url", "digitos_url",
]


def lexicas(u: UrlNormalizada) -> dict[str, float]:
    h = u.host
    completa = h + u.ruta
    subdominios = 0 if not u.subdominio else u.subdominio.count(".") + 1
    return {
        "largo_host": len(h),
        "puntos_host": h.count("."),
        "guiones_host": h.count("-"),
        "digitos_host": sum(ch.isdigit() for ch in h),
        "especiales_host": len(ESPECIALES.findall(h)),
        "subdominios": subdominios,
        "usa_ip": int(_es_ip(h)),
        "tld_abusado": int(u.sufijo.rsplit(".", 1)[-1] in TLDS_ABUSADOS),
        "entropia_host": round(entropia(h), 4),
        "palabras_sensibles_host": len(_palabras_en(h)),
        "largo_url": len(u.original),
        "largo_ruta": len(u.ruta),
        "usa_https": int(u.esquema == "https"),
        "palabras_sensibles_ruta": len(_palabras_en(u.ruta)),
        "puntos_url": completa.count("."),
        "guiones_url": completa.count("-"),
        "digitos_url": sum(ch.isdigit() for ch in completa),
    }


def texto_host(u: UrlNormalizada) -> str:
    """Texto que ven los n-gramas de caracteres del modelo: el host normalizado."""
    return u.host


def texto_url_completa(u: UrlNormalizada) -> str:
    """Solo para el experimento de control."""
    return u.esquema + "://" + u.host + u.ruta


# --- Textos para explicar una prediccion -------------------------------------

def describir(nombre: str, valor: float, u: UrlNormalizada, hacia_phishing: bool) -> str:
    """Frase clara para una caracteristica lexica y su valor."""
    v = int(valor) if float(valor).is_integer() else valor
    match nombre:
        case "largo_host":
            return f"el dominio {'es largo' if hacia_phishing else 'es corto'} ({v} caracteres)"
        case "puntos_host":
            return f"el dominio tiene {v} punto{'s' if v != 1 else ''}"
        case "guiones_host":
            return f"el dominio tiene {v} guion{'es' if v != 1 else ''}" if v else "el dominio no tiene guiones"
        case "digitos_host":
            return f"el dominio tiene {v} digito{'s' if v != 1 else ''}" if v else "el dominio no tiene digitos"
        case "especiales_host":
            return f"el dominio tiene {v} caracter{'es' if v != 1 else ''} poco comun{'es' if v != 1 else ''}"
        case "subdominios":
            return f"tiene {v} subdominio{'s' if v != 1 else ''} ({u.subdominio})" if v else "no tiene subdominios"
        case "usa_ip":
            return "usa una direccion IP en lugar de un dominio" if v else "usa un nombre de dominio, no una IP"
        case "tld_abusado":
            return f"la extension .{u.sufijo} se usa mucho en phishing" if v else f"la extension .{u.sufijo} no es de las abusadas"
        case "entropia_host":
            return (f"el dominio parece aleatorio (entropia {v:.2f})" if hacia_phishing
                    else f"el dominio no parece aleatorio (entropia {v:.2f})")
        case "palabras_sensibles_host":
            palabras = ", ".join(f"'{p}'" for p in _palabras_en(u.host))
            return f"el dominio contiene {palabras}" if v else "el dominio no contiene palabras sensibles"
    return f"{nombre} = {v}"
