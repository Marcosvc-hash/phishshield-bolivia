package bo.phishshield.backend.qa.infrastructure.ejecutor;

import org.opentest4j.TestAbortedException;

/**
 * El caso no puede correr en este entorno (por ejemplo, falta QA_TEST_USER):
 * queda NOT_RUN con este mensaje, no FAIL. Hereda de TestAbortedException
 * para que JUnit lo trate como "abortado" y no como fallo.
 */
public class CasoNoEjecutableException extends TestAbortedException {

    public CasoNoEjecutableException(String mensaje) {
        super(mensaje);
    }
}
