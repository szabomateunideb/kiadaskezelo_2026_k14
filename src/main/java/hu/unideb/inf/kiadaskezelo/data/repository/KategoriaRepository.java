package hu.unideb.inf.kiadaskezelo.data.repository;

import hu.unideb.inf.kiadaskezelo.data.entity.KategoriaEntity;
import hu.unideb.inf.kiadaskezelo.data.entity.enums.KategoriaTipus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KategoriaRepository extends JpaRepository<KategoriaEntity, Long> {
    Optional<KategoriaEntity> findByNevAndTipus(String nev, KategoriaTipus tipus);
    List<KategoriaEntity> findAllByTipusOrderByNevAsc(KategoriaTipus tipus);
}
