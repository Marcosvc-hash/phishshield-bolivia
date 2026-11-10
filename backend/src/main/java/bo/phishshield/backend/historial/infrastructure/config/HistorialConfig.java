package bo.phishshield.backend.historial.infrastructure.config;

import bo.phishshield.backend.historial.application.ListarHistorialUseCase;
import bo.phishshield.backend.historial.application.ObtenerDetalleAnalisisUseCase;
import bo.phishshield.backend.historial.domain.port.out.HistorialQueryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cablea los casos de uso del historial sin anotar la capa application.
 */
@Configuration
public class HistorialConfig {

    @Bean
    public ListarHistorialUseCase listarHistorialUseCase(HistorialQueryPort consultas) {
        return new ListarHistorialUseCase(consultas);
    }

    @Bean
    public ObtenerDetalleAnalisisUseCase obtenerDetalleAnalisisUseCase(HistorialQueryPort consultas) {
        return new ObtenerDetalleAnalisisUseCase(consultas);
    }
}
