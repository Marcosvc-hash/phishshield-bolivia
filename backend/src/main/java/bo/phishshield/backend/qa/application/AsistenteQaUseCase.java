package bo.phishshield.backend.qa.application;

import bo.phishshield.backend.qa.domain.model.AnalisisFallo;
import bo.phishshield.backend.qa.domain.model.AsistenteNoDisponibleException;
import bo.phishshield.backend.qa.domain.model.CapacidadesQa;
import bo.phishshield.backend.qa.domain.model.ContextoFallo;
import bo.phishshield.backend.qa.domain.model.Ejecucion;
import bo.phishshield.backend.qa.domain.model.EjecucionNoEncontradaException;
import bo.phishshield.backend.qa.domain.model.EstadoCaso;
import bo.phishshield.backend.qa.domain.model.ResultadoCaso;
import bo.phishshield.backend.qa.domain.model.SeleccionInvalidaException;
import bo.phishshield.backend.qa.domain.port.out.AsistenteIAPort;
import bo.phishshield.backend.qa.domain.port.out.EjecucionRepositoryPort;
import bo.phishshield.backend.qa.domain.port.out.NotificadorCorreoPort;
import bo.phishshield.backend.shared.domain.model.AlcanceDatos;
import bo.phishshield.backend.shared.domain.model.UsuarioActual;

import java.util.Locale;
import java.util.UUID;

/**
 * Casos de uso del asistente de IA (opcional) y de las capacidades del
 * Centro de Pruebas. El asistente solo explica: el estado de cada caso lo
 * decidio la prueba y no se modifica. Solo ADMIN.
 */
public class AsistenteQaUseCase {

    private final EjecucionRepositoryPort ejecuciones;
    private final AsistenteIAPort asistente;
    private final NotificadorCorreoPort correo;

    public AsistenteQaUseCase(EjecucionRepositoryPort ejecuciones, AsistenteIAPort asistente,
                              NotificadorCorreoPort correo) {
        this.ejecuciones = ejecuciones;
        this.asistente = asistente;
        this.correo = correo;
    }

    public CapacidadesQa capacidades(UsuarioActual quien) {
        AlcanceDatos.soloPlataforma(quien);
        return new CapacidadesQa(correo.estaConfigurado(), asistente.estaDisponible());
    }

    /** Causa probable de un caso en FAIL, con la evidencia que la respalda. */
    public AnalisisFallo analizarFallo(UsuarioActual quien, UUID idEjecucion, String codigo) {
        AlcanceDatos.soloPlataforma(quien);
        exigirDisponible();
        Ejecucion ejecucion = buscar(idEjecucion);
        String buscado = codigo == null ? "" : codigo.strip().toUpperCase(Locale.ROOT);
        ResultadoCaso resultado = ejecucion.getResultados().stream()
                .filter(r -> r.getCodigo().equals(buscado))
                .findFirst()
                .orElseThrow(() -> new SeleccionInvalidaException("El caso " + buscado + " no forma parte de esta ejecución"));
        if (resultado.getEstado() != EstadoCaso.FAIL) {
            throw new SeleccionInvalidaException("El asistente solo analiza casos en FAIL (" + buscado + " está en "
                    + resultado.getEstado().name() + ")");
        }
        return asistente.analizarFallo(ContextoFallo.de(resultado))
                .orElseThrow(() -> new AsistenteNoDisponibleException("El asistente de IA no respondió; intente de nuevo más tarde"));
    }

    /** Resumen ejecutivo de una ejecucion terminada. */
    public String resumir(UsuarioActual quien, UUID idEjecucion) {
        AlcanceDatos.soloPlataforma(quien);
        exigirDisponible();
        Ejecucion ejecucion = buscar(idEjecucion);
        if (ejecucion.getEstado().estaActiva()) {
            throw new SeleccionInvalidaException("La ejecución todavía no terminó");
        }
        return asistente.resumirEjecucion(ejecucion)
                .orElseThrow(() -> new AsistenteNoDisponibleException("El asistente de IA no respondió; intente de nuevo más tarde"));
    }

    private void exigirDisponible() {
        if (!asistente.estaDisponible()) {
            throw new AsistenteNoDisponibleException(
                    "El asistente de IA no está configurado (falta la variable ANTHROPIC_API_KEY)");
        }
    }

    private Ejecucion buscar(UUID id) {
        return ejecuciones.buscarPorUuid(id).orElseThrow(() -> new EjecucionNoEncontradaException(id));
    }
}
