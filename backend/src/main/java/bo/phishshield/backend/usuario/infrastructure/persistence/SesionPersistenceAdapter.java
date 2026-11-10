package bo.phishshield.backend.usuario.infrastructure.persistence;

import bo.phishshield.backend.usuario.domain.model.SesionRefresco;
import bo.phishshield.backend.usuario.domain.port.out.SesionRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class SesionPersistenceAdapter implements SesionRepositoryPort {

    /** sesiones.user_agent es VARCHAR(255); algunos navegadores mandan mas. */
    private static final int LARGO_USER_AGENT = 255;

    private final SesionJpaRepository jpaRepository;

    public SesionPersistenceAdapter(SesionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public SesionRefresco guardar(SesionRefresco sesion) {
        SesionEntity entidad = new SesionEntity();
        entidad.setIdUsuario(sesion.idUsuario());
        entidad.setRefreshTokenHash(sesion.hashToken());
        entidad.setUserAgent(recortar(sesion.userAgent()));
        entidad.setExpiraEn(sesion.expiraEn());
        entidad.setRevocada(sesion.revocada());
        return aDominio(jpaRepository.save(entidad));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SesionRefresco> buscarPorHash(String hashToken) {
        return jpaRepository.findByRefreshTokenHash(hashToken).map(this::aDominio);
    }

    @Override
    @Transactional
    public void revocar(Long idSesion) {
        jpaRepository.findById(idSesion).ifPresent(s -> s.setRevocada(true));
    }

    private SesionRefresco aDominio(SesionEntity e) {
        return new SesionRefresco(e.getIdSesion(), e.getIdUsuario(), e.getRefreshTokenHash(),
                e.getExpiraEn(), e.getRevocada(), e.getUserAgent());
    }

    private static String recortar(String texto) {
        return texto == null || texto.length() <= LARGO_USER_AGENT
                ? texto
                : texto.substring(0, LARGO_USER_AGENT);
    }
}
