package hu.unideb.inf.kiadaskezelo.data.repository;

import hu.unideb.inf.kiadaskezelo.data.entity.UtalasEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtalasRepository extends JpaRepository<UtalasEntity, Long> {
    @EntityGraph(attributePaths = {"penzmozgas", "celSzamla"})
    Optional<UtalasEntity> findByPenzmozgas_Id(Long penzmozgasId);

    @EntityGraph(attributePaths = {"penzmozgas", "celSzamla"})
    List<UtalasEntity> findAllByCelSzamla_Id(Long celSzamlaId);
}
