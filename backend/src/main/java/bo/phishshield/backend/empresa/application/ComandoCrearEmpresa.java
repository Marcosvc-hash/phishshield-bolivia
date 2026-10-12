package bo.phishshield.backend.empresa.application;

/**
 * Entrada del caso de uso. Desacopla el caso de uso del DTO web.
 */
public record ComandoCrearEmpresa(String nombre,
                                  String nit,
                                  String sector,
                                  String ciudad,
                                  String plan) {
}
