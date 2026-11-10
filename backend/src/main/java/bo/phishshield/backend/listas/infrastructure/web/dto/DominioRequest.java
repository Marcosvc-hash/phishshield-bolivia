package bo.phishshield.backend.listas.infrastructure.web.dto;

import bo.phishshield.backend.listas.domain.model.DominioEnLista;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/admin/dominios (y de PUT, donde dominio y lista se ignoran).
 * Ejemplo: {"dominio": "bnb-pagos.xyz", "lista": "negra",
 *           "motivo": "Reportado por un cliente", "entidad": "Banco Nacional de Bolivia"}
 *
 * Aca solo se revisa lo basico; el formato del dominio lo valida el dominio
 * (NombreDominio), que es donde vive la regla.
 */
public record DominioRequest(

        @Size(max = 255, message = "El dominio no puede superar los 255 caracteres")
        String dominio,

        String lista,

        @NotBlank(message = "Indique el motivo")
        @Size(max = DominioEnLista.MAXIMO_MOTIVO,
                message = "El motivo no puede superar los " + DominioEnLista.MAXIMO_MOTIVO + " caracteres")
        String motivo,

        @Size(max = 120, message = "El nombre de la entidad no puede superar los 120 caracteres")
        String entidad
) {
}
