"""Servicio de prediccion de phishing por URL (FastAPI).

Endpoints:
  POST /predecir  {"url": "..."}  -> probabilidad, veredicto segun el umbral y senales
  GET  /salud                     -> estado del servicio
  GET  /modelo                    -> metadata.json del modelo (sin rutas internas)

Configuracion por variables de entorno:
  IA_MODELO_DIR     carpeta del modelo (por defecto ia/modelos/1.0.0)
  IA_URL_MAX        largo maximo aceptado para la URL (por defecto 2048)
  IA_MAX_SENALES    senales devueltas por prediccion, entre 3 y 5 (por defecto 5)
  IA_LOG_LEVEL      nivel de log (por defecto INFO)

Levantar:  uvicorn servicio:app --port 8000
"""

from __future__ import annotations

import json
import logging
import os
import time
from contextlib import asynccontextmanager
from pathlib import Path

import joblib
from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel, Field, field_validator

import caracteristicas as c

RAIZ = Path(__file__).resolve().parent
MODELO_DIR = Path(os.environ.get("IA_MODELO_DIR", RAIZ / "modelos" / "1.0.0"))
URL_MAX = int(os.environ.get("IA_URL_MAX", "2048"))
MAX_SENALES = min(5, max(3, int(os.environ.get("IA_MAX_SENALES", "5"))))

logging.basicConfig(level=os.environ.get("IA_LOG_LEVEL", "INFO"),
                    format="%(asctime)s %(levelname)s [ia] %(message)s")
log = logging.getLogger("phishshield.ia")


def _sin_rutas_internas(dato):
    """Quita valores que parezcan rutas del disco antes de exponer la metadata."""
    if isinstance(dato, dict):
        return {k: _sin_rutas_internas(v) for k, v in dato.items()
                if not (isinstance(v, str) and (":\\" in v or v.startswith("/")))}
    if isinstance(dato, list):
        return [_sin_rutas_internas(v) for v in dato]
    return dato


@asynccontextmanager
async def ciclo_de_vida(app: FastAPI):
    # El modelo se carga una sola vez, al iniciar.
    inicio = time.perf_counter()
    archivo = MODELO_DIR / "modelo.joblib"
    if not archivo.exists():
        raise RuntimeError(f"No existe {archivo}. Ejecuta primero: python entrenar.py")
    app.state.detector = joblib.load(archivo)
    app.state.metadata = json.loads((MODELO_DIR / "metadata.json").read_text(encoding="utf-8"))
    md = app.state.metadata
    log.info("Modelo %s %s cargado en %.0f ms (umbral %.4f)",
             md["nombre"], md["version"], (time.perf_counter() - inicio) * 1000,
             app.state.detector.umbral)
    yield
    log.info("Servicio detenido")


app = FastAPI(title="PhishShield Bolivia - IA de URL", version="1.0.0", lifespan=ciclo_de_vida)


class SolicitudPrediccion(BaseModel):
    url: str = Field(min_length=1, max_length=URL_MAX, examples=["https://www.bnb.com.bo/"])

    @field_validator("url")
    @classmethod
    def validar_url(cls, valor: str) -> str:
        valor = valor.strip()
        if not valor:
            raise ValueError("la URL no puede estar vacia")
        if any(ch.isspace() for ch in valor):
            raise ValueError("la URL no puede contener espacios")
        if not c.normalizar(valor).host:
            raise ValueError("no se pudo extraer un dominio de la URL")
        return valor


class SenalRespuesta(BaseModel):
    texto: str
    aporte: float
    direccion: str


class RespuestaPrediccion(BaseModel):
    probabilidad_phishing: float
    es_phishing: bool
    umbral: float
    senales: list[SenalRespuesta]
    modelo: str
    version: str


@app.post("/predecir", response_model=RespuestaPrediccion)
def predecir(solicitud: SolicitudPrediccion, request: Request) -> RespuestaPrediccion:
    det = request.app.state.detector
    md = request.app.state.metadata
    inicio = time.perf_counter()
    try:
        pred = det.predecir(solicitud.url, max_senales=MAX_SENALES)
    except Exception:
        log.exception("Error al predecir")
        raise HTTPException(status_code=500, detail="error interno al predecir")
    log.info("prediccion host=%s prob=%.4f phishing=%s %.1f ms",
             c.normalizar(solicitud.url).host, pred.probabilidad, pred.es_phishing,
             (time.perf_counter() - inicio) * 1000)
    return RespuestaPrediccion(
        probabilidad_phishing=round(pred.probabilidad, 4),
        es_phishing=pred.es_phishing,
        umbral=round(det.umbral, 4),
        senales=[SenalRespuesta(texto=s.texto, aporte=s.aporte, direccion=s.direccion)
                 for s in pred.senales],
        modelo=md["nombre"],
        version=md["version"],
    )


@app.get("/salud")
def salud(request: Request) -> dict:
    md = request.app.state.metadata
    return {"estado": "ok", "modelo": md["nombre"], "version": md["version"]}


@app.get("/modelo")
def modelo(request: Request) -> dict:
    return _sin_rutas_internas(request.app.state.metadata)
