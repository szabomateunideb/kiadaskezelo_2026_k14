package hu.unideb.inf.kiadaskezelo.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import hu.unideb.inf.kiadaskezelo.data.entity.IsmetlodoPenzmozgasEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface IsmetlodoPenzmozgasRepository extends JpaRepository<IsmetlodoPenzmozgasEntity, Long> {

    @EntityGraph(attributePaths = {"szamla", "kategoria"})
    @Query("select i from IsmetlodoPenzmozgasEntity i where i.aktiv = true and i.kovetkezoDatum <= :nap "
            + "and i.szamla.aktiv = true and i.szamla.felhasznalo.aktiv = true order by i.kovetkezoDatum, i.id")
    List<IsmetlodoPenzmozgasEntity> feldolgozhatoEsedekesek(@Param("nap") LocalDate nap, Pageable page);

    @EntityGraph(attributePaths = {"szamla", "kategoria"})
    Page<IsmetlodoPenzmozgasEntity> findAllBySzamla_Felhasznalo_Id(Long owner, Pageable pageable);
    boolean existsBySzamla_IdAndAktivTrue(Long id);
    boolean existsBySzamla_Felhasznalo_IdAndKategoria_IdAndAktivTrue(Long owner, Long category);
    boolean existsByKategoria_Id(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from IsmetlodoPenzmozgasEntity i where i.id = :id")
    Optional<IsmetlodoPenzmozgasEntity> lockById(@Param("id") Long id);
    @EntityGraph(attributePaths = {"szamla", "kategoria"})
    List<IsmetlodoPenzmozgasEntity> findAllByAktivTrueAndKovetkezoDatumLessThanEqual(LocalDate datum);

    @EntityGraph(attributePaths = {"szamla", "kategoria"})
    List<IsmetlodoPenzmozgasEntity> findAllBySzamla_Felhasznalo_Id(Long felhasznaloId);
}
