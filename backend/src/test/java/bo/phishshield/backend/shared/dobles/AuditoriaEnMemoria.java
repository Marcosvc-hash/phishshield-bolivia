package bo.phishshield.backend.shared.dobles;

import bo.phishshield.backend.shared.domain.model.AccionAuditoria;
import bo.phishshield.backend.shared.domain.model.EventoAuditoria;
import bo.phishshield.backend.shared.domain.port.out.AuditoriaPort;

import java.util.ArrayList;
import java.util.List;

/** Doble de AuditoriaPort: guarda los eventos en una lista para revisarlos. */
public class AuditoriaEnMemoria implements AuditoriaPort {

    public final List<EventoAuditoria> eventos = new ArrayList<>();

    @Override
    public void registrar(EventoAuditoria evento) {
        eventos.add(evento);
    }

    public EventoAuditoria ultimo() {
        return eventos.get(eventos.size() - 1);
    }

    public List<AccionAuditoria> acciones() {
        return eventos.stream().map(EventoAuditoria::accion).toList();
    }
}
