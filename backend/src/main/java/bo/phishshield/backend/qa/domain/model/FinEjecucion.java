package bo.phishshield.backend.qa.domain.model;

/**
 * Como termino el recorrido del ejecutor de pruebas.
 *
 * COMPLETA: se recorrieron todos los casos del plan (pasaran o no).
 * DETENIDA: alguien pidio detenerla y los casos que faltaban no corrieron.
 */
public enum FinEjecucion {
    COMPLETA,
    DETENIDA
}
