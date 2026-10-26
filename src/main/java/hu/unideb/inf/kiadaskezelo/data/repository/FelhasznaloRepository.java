package hu.unideb.inf.kiadaskezelo.data.repository;

import hu.unideb.inf.kiadaskezelo.data.entity.FelhasznaloEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FelhasznaloRepository extends JpaRepository<FelhasznaloEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "jog")
    Page<FelhasznaloEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "jog")
    Page<FelhasznaloEntity> findAllByNevContainingIgnoreCase(String nev, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "jog")
    Optional<FelhasznaloEntity> findById(Long id);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "jog")
    Optional<FelhasznaloEntity> findByFelhasznalonev(String felhasznalonev);

    boolean existsByFelhasznalonev(String felhasznalonev);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByFelhasznalonevAndIdNot(String felhasznalonev, Long id);

    // További órai példák: a név és a jogosultság nem egyedi, ezért a visszatérés lista.
    List<FelhasznaloEntity> findAllByNev(String nev);

    List<FelhasznaloEntity> findAllByNem(String nem);

    List<FelhasznaloEntity> findAllByJog_Nev(String jogNev);

    @Query("SELECT f FROM FelhasznaloEntity f WHERE f.nem = :nem AND f.jog.nev = 'FELHASZNALO'")
    List<FelhasznaloEntity> findByJPQL(@Param("nem") String nem);

    @Query(value = "SELECT * FROM FELHASZNALO f WHERE f.nev = :nev", nativeQuery = true)
    List<FelhasznaloEntity> findByNative(@Param("nev") String nev);
}
