package bo.phishshield.backend.analisis.infrastructure.config;

import bo.phishshield.backend.analisis.application.AnalizarUrlUseCase;
import bo.phishshield.backend.analisis.domain.port.out.ReglaDeteccionRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Al arrancar, avisa si una regla implementada en Java no tiene fila en
 * reglas_deteccion. Sin esa fila la regla aporta 0 puntos y su indicador
 * no se guarda, sin ningun error visible (por ejemplo, si falta aplicar
 * una migracion). No detiene el arranque: solo deja el aviso en el log.
 */
@Component
public class VerificacionReglasAlArrancar implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(VerificacionReglasAlArrancar.class);

    private final AnalizarUrlUseCase analizarUrl;
    private final ReglaDeteccionRepositoryPort reglaRepository;

    public VerificacionReglasAlArrancar(AnalizarUrlUseCase analizarUrl,
                                        ReglaDeteccionRepositoryPort reglaRepository) {
        this.analizarUrl = analizarUrl;
        this.reglaRepository = reglaRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> faltantes = codigosSinRegistrar(
                analizarUrl.codigosDeReglas(), reglaRepository.codigosRegistrados());

        for (String codigo : faltantes) {
            log.warn("La regla {} existe en Java pero no en reglas_deteccion: aportara 0 puntos "
                    + "y su indicador no se guardara. Revise las migraciones de database/migraciones.",
                    codigo);
        }
    }

    /** Codigos implementados en Java que no aparecen en la base, en el mismo orden. */
    static List<String> codigosSinRegistrar(Collection<String> enJava, Set<String> enBase) {
        return enJava.stream()
                .filter(codigo -> !enBase.contains(codigo))
                .toList();
    }
}
