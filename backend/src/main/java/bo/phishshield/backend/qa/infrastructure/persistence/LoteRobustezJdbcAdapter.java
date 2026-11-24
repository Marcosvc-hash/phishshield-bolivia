package bo.phishshield.backend.qa.infrastructure.persistence;

import bo.phishshield.backend.qa.domain.model.UrlEtiquetada;
import bo.phishshield.backend.qa.domain.port.out.LoteRobustezPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;

/** Lote de robustez con SQL directo. */
@Component
public class LoteRobustezJdbcAdapter implements LoteRobustezPort {

    private final JdbcClient jdbc;

    public LoteRobustezJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<UrlEtiquetada> listarActivasAprobadas() {
        return jdbc.sql("""
                        SELECT url, etiqueta, categoria FROM qa_lote_robustez
                        WHERE activo AND estado_revision = 'aprobada'
                        ORDER BY id_url
                        """)
                .query((rs, n) -> new UrlEtiquetada(rs.getString("url"),
                        "phishing".equals(rs.getString("etiqueta")), rs.getString("categoria")))
                .list();
    }
}
