package hu.unideb.inf.kiadaskezelo.data.repository;

import hu.unideb.inf.kiadaskezelo.data.entity.MegtakaritasiTervEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MegtakaritasiTervRepository extends JpaRepository<MegtakaritasiTervEntity, Long> {
    List<MegtakaritasiTervEntity> findAllByFelhasznalo_Id(Long felhasznaloId);
}
