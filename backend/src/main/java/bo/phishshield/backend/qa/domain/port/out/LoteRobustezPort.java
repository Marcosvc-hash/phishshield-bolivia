package bo.phishshield.backend.qa.domain.port.out;

import bo.phishshield.backend.qa.domain.model.UrlEtiquetada;

import java.util.List;

/** Lee el lote etiquetado de robustez (qa_lote_robustez). */
public interface LoteRobustezPort {

    /** Las URL activas y aprobadas, en orden de carga. */
    List<UrlEtiquetada> listarActivasAprobadas();
}
