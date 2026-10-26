package hu.unideb.inf.kiadaskezelo.data.repository;

import hu.unideb.inf.kiadaskezelo.data.entity.KartyaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KartyaRepository extends JpaRepository<KartyaEntity, Long> {
    List<KartyaEntity> findAllBySzamla_Id(Long szamlaId);

    @EntityGraph(attributePaths = "szamla")
    List<KartyaEntity> findAllBySzamla_Felhasznalo_IdAndAktivTrue(Long felhasznaloId);
}
