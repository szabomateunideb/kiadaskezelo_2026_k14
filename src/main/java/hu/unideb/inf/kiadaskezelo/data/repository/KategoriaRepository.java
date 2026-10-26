package hu.unideb.inf.kiadaskezelo.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import hu.unideb.inf.kiadaskezelo.data.entity.KategoriaEntity;
import hu.unideb.inf.kiadaskezelo.data.entity.enums.KategoriaTipus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KategoriaRepository extends JpaRepository<KategoriaEntity, Long> {

    Page<KategoriaEntity> findAllByTipus(KategoriaTipus tipus, Pageable pageable);
    boolean existsByNevAndTipusAndIdNot(String nev, KategoriaTipus tipus, Long id);
    Optional<KategoriaEntity> findByNevAndTipus(String nev, KategoriaTipus tipus);
    List<KategoriaEntity> findAllByTipusOrderByNevAsc(KategoriaTipus tipus);
}
