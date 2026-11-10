package bo.phishshield.backend.usuario.domain.model;

/** La contrasena era correcta, pero la cuenta no esta activa. */
public class CuentaNoHabilitadaException extends RuntimeException {

    public CuentaNoHabilitadaException(EstadoUsuario estado) {
        super("La cuenta no esta habilitada (estado: " + estado.aValorBd() + ")");
    }
}
