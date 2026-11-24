package bo.phishshield.backend.qa.domain.model;

import java.util.Set;

/**
 * Ciclo de vida de una ejecucion (chk_qa_ejec_estado):
 *
 *   PENDIENTE --> EN_CURSO --> FINALIZADA
 *        \            \
 *         \------------\--> FALLIDA
 *
 * FINALIZADA significa que todos los casos llegaron a correr (el resultado
 * global dice si pasaron). FALLIDA significa que la propia ejecucion se
 * rompio o se interrumpio: los casos que no alcanzaron a correr quedan en
 * NOT_RUN. Esta es la UNICA definicion de las transiciones validas.
 */
public enum EstadoEjecucion {

    PENDIENTE,
    EN_CURSO,
    FINALIZADA,
    FALLIDA;

    public boolean puedePasarA(EstadoEjecucion destino) {
        return siguientes().contains(destino);
    }

    public Set<EstadoEjecucion> siguientes() {
        return switch (this) {
            case PENDIENTE -> Set.of(EN_CURSO, FALLIDA);
            case EN_CURSO -> Set.of(FINALIZADA, FALLIDA);
            case FINALIZADA, FALLIDA -> Set.of();
        };
    }

    public boolean esFinal() {
        return siguientes().isEmpty();
    }

    /** Pendiente o en curso: mientras haya una asi, no se lanza otra (uq_qa_ejec_activa). */
    public boolean estaActiva() {
        return !esFinal();
    }

    public String aValorBd() {
        return ValorBd.de(this);
    }

    public static EstadoEjecucion desdeValorBd(String valor) {
        return ValorBd.convertir(EstadoEjecucion.class, valor, "el estado de la ejecución");
    }
}
