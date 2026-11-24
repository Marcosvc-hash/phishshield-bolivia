package bo.phishshield.backend.qa.application;

import bo.phishshield.backend.qa.domain.model.ArchivoEvidencia;
import bo.phishshield.backend.qa.domain.model.ArchivosReporte;
import bo.phishshield.backend.qa.domain.model.Ejecucion;
import bo.phishshield.backend.qa.domain.model.EjecucionNoEncontradaException;
import bo.phishshield.backend.qa.domain.model.FormatoReporte;
import bo.phishshield.backend.qa.domain.model.ReporteNoDisponibleException;
import bo.phishshield.backend.qa.domain.port.out.EjecucionRepositoryPort;
import bo.phishshield.backend.qa.domain.port.out.EvidenciasPort;
import bo.phishshield.backend.qa.domain.port.out.GeneradorReportePort;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;

import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: descargar el reporte PDF o HTML de una ejecucion terminada.
 * Si el archivo no existe (fallo al generarlo o se borro la carpeta), se
 * vuelve a generar en ese momento. Solo ADMIN.
 */
public class ObtenerReporteUseCase {

    private final EjecucionRepositoryPort ejecuciones;
    private final GeneradorReportePort generador;
    private final EvidenciasPort evidencias;

    public ObtenerReporteUseCase(EjecucionRepositoryPort ejecuciones, GeneradorReportePort generador,
                                 EvidenciasPort evidencias) {
        this.ejecuciones = ejecuciones;
        this.generador = generador;
        this.evidencias = evidencias;
    }

    public ArchivoEvidencia ejecutar(UsuarioActual quien, UUID id, FormatoReporte formato) {
        AlcanceDatos.soloPlataforma(quien);

        Ejecucion ejecucion = ejecuciones.buscarPorUuid(id)
                .orElseThrow(() -> new EjecucionNoEncontradaException(id));
        if (ejecucion.getEstado().estaActiva()) {
            throw new ReporteNoDisponibleException(
                    "La ejecución todavía no terminó: los reportes se generan al final");
        }

        Optional<ArchivoEvidencia> archivo = Optional.ofNullable(ruta(ejecucion, formato))
                .flatMap(evidencias::leer);
        if (archivo.isPresent()) {
            return archivo.get();
        }

        ArchivosReporte nuevos = generador.generar(ejecucion);
        ejecucion.registrarReportes(nuevos);
        ejecuciones.guardar(ejecucion);
        return evidencias.leer(ruta(ejecucion, formato))
                .orElseThrow(() -> new ReporteNoDisponibleException("No se pudo generar el reporte"));
    }

    private static String ruta(Ejecucion ejecucion, FormatoReporte formato) {
        return formato == FormatoReporte.PDF ? ejecucion.getRutaReportePdf() : ejecucion.getRutaReporteHtml();
    }
}
