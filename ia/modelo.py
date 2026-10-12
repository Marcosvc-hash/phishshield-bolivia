"""Detector de phishing por URL: prediccion calibrada y explicacion.

Lo crea entrenar.py y lo carga servicio.py. Encapsula todo el camino
URL -> caracteristicas -> probabilidad, para que no haya dos versiones
del preprocesamiento.

Explicacion: en una Regresion Logistica el logit es la suma de
coeficiente x valor de cada caracteristica, asi que cada termino es el
aporte exacto de esa caracteristica a la prediccion (no una aproximacion).
"""

from __future__ import annotations

from dataclasses import dataclass, field

import numpy as np
from scipy import sparse

import caracteristicas as c


def sigmoide(z):
    return 1.0 / (1.0 + np.exp(-z))


@dataclass
class Senal:
    texto: str
    aporte: float      # en logit; positivo empuja hacia phishing
    direccion: str     # "phishing" o "legitimo"


@dataclass
class Prediccion:
    probabilidad: float
    es_phishing: bool
    senales: list[Senal]


@dataclass
class Detector:
    """Regresion Logistica sobre [lexicas del host escaladas | n-gramas TF-IDF del host]."""

    vectorizador: object            # TfidfVectorizer ajustado
    escalador: object               # StandardScaler ajustado
    clasificador: object            # LogisticRegression ajustada
    calibracion: tuple[float, float] = (1.0, 0.0)  # Platt: p = sigmoide(a*logit + b)
    umbral: float = 0.5
    nombres_lexicas: list[str] = field(default_factory=lambda: list(c.CARACTERISTICAS_HOST))

    # --- matrices -----------------------------------------------------------

    def matriz(self, urls: list[c.UrlNormalizada]):
        lex = np.array([[c.lexicas(u)[n] for n in self.nombres_lexicas] for u in urls], dtype=float)
        lex = self.escalador.transform(lex)
        ngr = self.vectorizador.transform([c.texto_host(u) for u in urls])
        return sparse.hstack([sparse.csr_matrix(lex), ngr], format="csr")

    def logit(self, urls: list[c.UrlNormalizada]) -> np.ndarray:
        return self.clasificador.decision_function(self.matriz(urls))

    def probabilidades(self, urls: list[c.UrlNormalizada]) -> np.ndarray:
        a, b = self.calibracion
        return sigmoide(a * self.logit(urls) + b)

    # --- una URL, con explicacion --------------------------------------------

    def predecir(self, url: str, max_senales: int = 5) -> Prediccion:
        u = c.normalizar(url)
        x = self.matriz([u])
        prob = float(self.probabilidades([u])[0])
        es_phishing = prob >= self.umbral
        return Prediccion(prob, es_phishing, self.explicar(u, x, es_phishing, max_senales))

    def explicar(self, u: c.UrlNormalizada, x, es_phishing: bool, max_senales: int) -> list[Senal]:
        coef = self.clasificador.coef_[0]
        fila = x.tocsr()
        indices, valores = fila.indices, fila.data
        aportes = coef[indices] * valores
        n_lex = len(self.nombres_lexicas)
        vocab = self.vectorizador.get_feature_names_out()

        candidatas: list[tuple[float, str, str]] = []  # (aporte, tipo, clave)
        for i, a in zip(indices, aportes):
            if a == 0:
                continue
            if i < n_lex:
                candidatas.append((float(a), "lex", self.nombres_lexicas[i]))
            else:
                candidatas.append((float(a), "ngr", vocab[i - n_lex]))

        # Primero las que empujan en la direccion del veredicto; si faltan, las otras.
        signo = 1 if es_phishing else -1
        a_favor = sorted((t for t in candidatas if t[0] * signo > 0), key=lambda t: -abs(t[0]))
        en_contra = sorted((t for t in candidatas if t[0] * signo <= 0), key=lambda t: -abs(t[0]))

        lex_crudas = c.lexicas(u)
        elegidas: list[Senal] = []
        ngramas_usados: list[str] = []
        for grupo in (a_favor, en_contra):
            for aporte, tipo, clave in grupo:
                if len(elegidas) >= max_senales or (grupo is en_contra and len(elegidas) >= 3):
                    break
                hacia_phishing = aporte > 0
                if tipo == "lex":
                    texto = c.describir(clave, lex_crudas[clave], u, hacia_phishing)
                else:
                    # n-gramas solapados ("logi", "login", "ogin") cuentan como uno solo
                    if any(clave in g or g in clave for g in ngramas_usados):
                        continue
                    ngramas_usados.append(clave)
                    texto = (f"el dominio contiene '{clave.strip()}', secuencia "
                             f"{'frecuente en phishing' if hacia_phishing else 'propia de sitios legitimos'}")
                elegidas.append(Senal(texto, round(aporte, 3), "phishing" if hacia_phishing else "legitimo"))
        return elegidas
