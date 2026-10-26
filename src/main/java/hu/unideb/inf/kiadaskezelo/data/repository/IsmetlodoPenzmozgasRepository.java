package hu.unideb.inf.kiadaskezelo.data.repository;

import hu.unideb.inf.kiadaskezelo.data.entity.IsmetlodoPenzmozgasEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface IsmetlodoPenzmozgasRepository extends JpaRepository<IsmetlodoPenzmozgasEntity, Long> {
    @EntityGraph(attributePaths = {"szamla", "kategoria"})
    List<IsmetlodoPenzmozgasEntity> findAllByAktivTrueAndKovetkezoDatumLessThanEqual(LocalDate datum);

    @EntityGraph(attributePaths = {"szamla", "kategoria"})
    List<IsmetlodoPenzmozgasEntity> findAllBySzamla_Felhasznalo_Id(Long felhasznaloId);
}
