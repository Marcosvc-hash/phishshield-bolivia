package bo.phishshield.backend.analisis.domain.service;

import bo.phishshield.backend.analisis.domain.model.Indicador;
import bo.phishshield.backend.analisis.domain.model.UrlAnalizada;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Aplica todas las reglas activas sobre una URL y arma la lista de indicadores.
 * Los pesos llegan desde la tabla reglas_deteccion: el motor no los conoce
 * de antemano y una regla sin peso registrado simplemente no aporta puntos.
 */
public class MotorReglasUrl {

    private final List<ReglaUrl> reglas;

    public MotorReglasUrl(List<ReglaUrl> reglas) {
        if (reglas == null || reglas.isEmpty()) {
            throw new IllegalArgumentException("El motor necesita al menos una regla");
        }
        this.reglas = List.copyOf(reglas);
    }

    /**
     * @param pesosPorCodigo pesos leidos de reglas_deteccion, indexados por codigo
     */
    public List<Indicador> evaluar(UrlAnalizada url, Map<String, BigDecimal> pesosPorCodigo) {
        List<Indicador> indicadores = new ArrayList<>();

        for (ReglaUrl regla : reglas) {
            regla.evaluar(url).ifPresent(evidencia -> {
                BigDecimal peso = pesosPorCodigo.getOrDefault(regla.codigo(), BigDecimal.ZERO);
                indicadores.add(new Indicador(regla.codigo(), evidencia, peso));
            });
        }

        return List.copyOf(indicadores);
    }
}