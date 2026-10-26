package hu.unideb.inf.kiadaskezelo.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

import hu.unideb.inf.kiadaskezelo.data.entity.MegtakaritasiTervEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MegtakaritasiTervRepository extends JpaRepository<MegtakaritasiTervEntity, Long> {

    Page<MegtakaritasiTervEntity> findAllByFelhasznalo_Id(Long owner, Pageable pageable);
    Optional<MegtakaritasiTervEntity> findByIdAndFelhasznalo_Id(Long id, Long owner);
    List<MegtakaritasiTervEntity> findAllByFelhasznalo_Id(Long felhasznaloId);
}
