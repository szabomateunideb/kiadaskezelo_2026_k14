package hu.unideb.inf.kiadaskezelo.data.repository;

import hu.unideb.inf.kiadaskezelo.data.entity.PenzmozgasEntity;
import hu.unideb.inf.kiadaskezelo.data.entity.enums.PenzmozgasAllapot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface PenzmozgasRepository extends JpaRepository<PenzmozgasEntity, Long> {
    @EntityGraph(attributePaths = {"szamla", "kategoria", "kartya", "ismetles"})
    Page<PenzmozgasEntity> findAllBySzamla_Id(Long szamlaId, Pageable pageable);

    @EntityGraph(attributePaths = {"szamla", "kategoria", "kartya", "ismetles"})
    Page<PenzmozgasEntity> findAllBySzamla_Felhasznalo_Id(Long felhasznaloId, Pageable pageable);

    @EntityGraph(attributePaths = {"szamla", "kategoria"})
    @Query("select p from PenzmozgasEntity p where p.szamla.id = :szamlaId "
            + "and p.allapot = :allapot and p.datum >= :tol and p.datum < :ig order by p.datum, p.id")
    List<PenzmozgasEntity> findInPeriod(@Param("szamlaId") Long szamlaId,
            @Param("allapot") PenzmozgasAllapot allapot, @Param("tol") Instant tol, @Param("ig") Instant ig);

    boolean existsByIsmetles_IdAndEsedekesseg(Long ismetlesId, LocalDate esedekesseg);
}
