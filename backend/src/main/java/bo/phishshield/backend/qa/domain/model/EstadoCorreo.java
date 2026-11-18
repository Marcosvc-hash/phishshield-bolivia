package bo.phishshield.backend.qa.domain.model;

/**
 * Situacion del envio de los reportes por correo (chk_qa_ejec_correo_estado).
 * Un error de envio no cambia el resultado de las pruebas: los reportes
 * siguen disponibles para descargar desde la pantalla.
 */
public enum EstadoCorreo {

    PENDIENTE("Pendiente"),
    ENVIADO("Enviado"),
    ERROR("Error de envío"),
    NO_CONFIGURADO("SMTP no configurado");

    private final String etiqueta;

    EstadoCorreo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public String aValorBd() {
        return ValorBd.de(this);
    }

    public static EstadoCorreo desdeValorBd(String valor) {
        return ValorBd.convertir(EstadoCorreo.class, valor, "el estado del correo");
    }
}
