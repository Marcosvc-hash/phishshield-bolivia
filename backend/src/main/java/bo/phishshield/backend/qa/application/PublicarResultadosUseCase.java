package bo.phishshield.backend.qa.application;

import bo.phishshield.backend.qa.domain.model.ArchivosReporte;
import bo.phishshield.backend.qa.domain.model.Ejecucion;
import bo.phishshield.backend.qa.domain.port.out.EjecucionRepositoryPort;
import bo.phishshield.backend.qa.domain.port.out.GeneradorReportePort;

import java.lang.System.Logger.Level;

/**
 * Caso de uso: lo que se hace cuando una ejecucion termina. Genera el PDF
 * detallado y el HTML resumido y guarda sus rutas en la ejecucion.
 *
 * Nunca lanza excepciones: si el reporte falla, el resultado de las
 * pruebas no cambia y el reporte se puede volver a pedir despues (se
 * genera al descargarlo).
 */
public class PublicarResultadosUseCase implements TrasEjecucion {

    private static final System.Logger log = System.getLogger(PublicarResultadosUseCase.class.getName());

    private final EjecucionRepositoryPort ejecuciones;
    private final GeneradorReportePort generador;

    public PublicarResultadosUseCase(EjecucionRepositoryPort ejecuciones, GeneradorReportePort generador) {
        this.ejecuciones = ejecuciones;
        this.generador = generador;
    }

    @Override
    public void alTerminar(Ejecucion ejecucion) {
        try {
            ArchivosReporte archivos = generador.generar(ejecucion);
            ejecucion.registrarReportes(archivos);
            ejecuciones.guardar(ejecucion);
        } catch (RuntimeException ex) {
            log.log(Level.WARNING, "No se pudieron generar los reportes de la ejecucion "
                    + ejecucion.getUuidPublico() + ": " + ex.getMessage(), ex);
        }
    }
}
