package hu.unideb.inf.kiadaskezelo.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

import hu.unideb.inf.kiadaskezelo.data.entity.KartyaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KartyaRepository extends JpaRepository<KartyaEntity, Long> {

    @EntityGraph(attributePaths = "szamla")
    Page<KartyaEntity> findAllBySzamla_Felhasznalo_Id(Long owner, Pageable pageable);
    Optional<KartyaEntity> findByIdAndSzamla_Felhasznalo_Id(Long id, Long owner);
    List<KartyaEntity> findAllBySzamla_Id(Long szamlaId);

    @EntityGraph(attributePaths = "szamla")
    List<KartyaEntity> findAllBySzamla_Felhasznalo_IdAndAktivTrue(Long felhasznaloId);
}
