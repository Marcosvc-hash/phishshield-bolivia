package bo.phishshield.backend.empresa.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaJpaRepository extends JpaRepository<EmpresaEntity, Long> {

    boolean existsByNit(String nit);
}