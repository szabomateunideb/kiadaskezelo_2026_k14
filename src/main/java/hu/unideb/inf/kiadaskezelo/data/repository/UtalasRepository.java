package hu.unideb.inf.kiadaskezelo.data.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import hu.unideb.inf.kiadaskezelo.data.entity.enums.PenzmozgasAllapot;
import hu.unideb.inf.kiadaskezelo.data.repository.PenzmozgasRepository.SzamlaOsszeg;

import hu.unideb.inf.kiadaskezelo.data.entity.UtalasEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtalasRepository extends JpaRepository<UtalasEntity, Long>, JpaSpecificationExecutor<UtalasEntity> {

    @Override
    @EntityGraph(attributePaths = {"penzmozgas.szamla", "penzmozgas.kategoria", "penzmozgas.kartya", "penzmozgas.ismetles", "celSzamla"})
    Page<UtalasEntity> findAll(Specification<UtalasEntity> spec, Pageable pageable);

    @Query("select u.celSzamla.id as szamlaId, sum(u.celOsszeg) as osszeg from UtalasEntity u "
            + "where u.celSzamla.felhasznalo.id = :owner and u.penzmozgas.allapot = :allapot group by u.celSzamla.id")
    List<SzamlaOsszeg> beerkezoOsszegek(@Param("owner") Long owner, @Param("allapot") PenzmozgasAllapot allapot);
    @EntityGraph(attributePaths = {"penzmozgas", "celSzamla"})
    Optional<UtalasEntity> findByPenzmozgas_Id(Long penzmozgasId);

    @EntityGraph(attributePaths = {"penzmozgas", "celSzamla"})
    List<UtalasEntity> findAllByCelSzamla_Id(Long celSzamlaId);
}
