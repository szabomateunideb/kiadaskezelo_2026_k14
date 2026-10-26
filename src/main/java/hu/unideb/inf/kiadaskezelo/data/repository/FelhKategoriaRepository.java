package hu.unideb.inf.kiadaskezelo.data.repository;

import hu.unideb.inf.kiadaskezelo.data.entity.FelhKategoriaEntity;
import hu.unideb.inf.kiadaskezelo.data.entity.FelhKategoriaId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FelhKategoriaRepository extends JpaRepository<FelhKategoriaEntity, FelhKategoriaId> {

    @EntityGraph(attributePaths = "kategoria")
    List<FelhKategoriaEntity> findAllByFelhasznalo_Id(Long owner);
    boolean existsByKategoria_Id(Long id);
    @EntityGraph(attributePaths = "kategoria")
    List<FelhKategoriaEntity> findAllByFelhasznalo_IdAndAktivTrue(Long felhasznaloId);

    boolean existsByFelhasznalo_IdAndKategoria_Id(Long felhasznaloId, Long kategoriaId);
}
