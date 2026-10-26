package hu.unideb.inf.kiadaskezelo.data.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Optional;
import hu.unideb.inf.kiadaskezelo.data.entity.enums.PenzmozgasTipus;

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

public interface PenzmozgasRepository extends JpaRepository<PenzmozgasEntity, Long>, JpaSpecificationExecutor<PenzmozgasEntity> {

    @Override
    @EntityGraph(attributePaths = {"szamla", "kategoria", "kartya", "ismetles"})
    Page<PenzmozgasEntity> findAll(Specification<PenzmozgasEntity> spec, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PenzmozgasEntity p where p.id = :id")
    Optional<PenzmozgasEntity> lockById(@Param("id") Long id);

    boolean existsBySzamla_Id(Long id);
    boolean existsByKategoria_Id(Long id);
    boolean existsByIsmetles_Id(Long id);

    interface SzamlaOsszeg {
        Long getSzamlaId();
        BigDecimal getOsszeg();
    }

    @Query("select p.szamla.id as szamlaId, sum(case when p.tipus = :bevetel then p.osszeg else -p.osszeg end) as osszeg "
            + "from PenzmozgasEntity p where p.szamla.felhasznalo.id = :owner and p.allapot = :allapot group by p.szamla.id")
    List<SzamlaOsszeg> egyenlegValtozasok(@Param("owner") Long owner,
            @Param("allapot") PenzmozgasAllapot allapot, @Param("bevetel") PenzmozgasTipus bevetel);

    interface KategoriaOsszesites {
        Long getKategoriaId();
        String getKategoriaNev();
        PenzmozgasTipus getTipus();
        String getPenznem();
        BigDecimal getOsszeg();
        long getTetelszam();
    }

    @Query("select k.id as kategoriaId, k.nev as kategoriaNev, p.tipus as tipus, s.penznem as penznem, "
            + "sum(p.osszeg) as osszeg, count(p) as tetelszam from PenzmozgasEntity p join p.szamla s join p.kategoria k "
            + "where s.felhasznalo.id = :owner and p.allapot = :allapot and p.tipus <> :atvezetes "
            + "and p.datum >= :tol and p.datum < :ig group by k.id, k.nev, p.tipus, s.penznem order by s.penznem, k.id")
    List<KategoriaOsszesites> statisztika(@Param("owner") Long owner, @Param("tol") Instant tol, @Param("ig") Instant ig,
            @Param("allapot") PenzmozgasAllapot allapot, @Param("atvezetes") PenzmozgasTipus atvezetes);
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
