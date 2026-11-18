package bo.phishshield.backend.qa.domain.port.out;

import bo.phishshield.backend.qa.domain.model.CorreoReporte;

/** Envia los reportes por correo. La configuracion SMTP sale solo de variables de entorno. */
public interface NotificadorCorreoPort {

    /** false si faltan las variables MAIL_*: el envio se omite sin intentar conectarse. */
    boolean estaConfigurado();

    /**
     * @throws bo.phishshield.backend.qa.domain.model.EnvioCorreoException si el envio falla;
     *         su mensaje se guarda en la ejecucion y no lleva credenciales
     */
    void enviar(CorreoReporte correo);
}
