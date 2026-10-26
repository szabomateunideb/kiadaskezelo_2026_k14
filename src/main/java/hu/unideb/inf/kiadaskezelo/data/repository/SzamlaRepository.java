package hu.unideb.inf.kiadaskezelo.data.repository;

import hu.unideb.inf.kiadaskezelo.data.entity.SzamlaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SzamlaRepository extends JpaRepository<SzamlaEntity, Long> {
    List<SzamlaEntity> findAllByFelhasznalo_IdAndAktivTrueOrderByNevAsc(Long felhasznaloId);
    Optional<SzamlaEntity> findByIdAndFelhasznalo_Id(Long id, Long felhasznaloId);
}
