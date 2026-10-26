package hu.unideb.inf.kiadaskezelo.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import hu.unideb.inf.kiadaskezelo.data.entity.SzamlaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SzamlaRepository extends JpaRepository<SzamlaEntity, Long> {

    Page<SzamlaEntity> findAllByFelhasznalo_Id(Long owner, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SzamlaEntity s where s.felhasznalo.id = :owner and s.id in :ids order by s.id")
    List<SzamlaEntity> lockOwned(@Param("owner") Long owner, @Param("ids") List<Long> ids);
    List<SzamlaEntity> findAllByFelhasznalo_IdAndAktivTrueOrderByNevAsc(Long felhasznaloId);
    Optional<SzamlaEntity> findByIdAndFelhasznalo_Id(Long id, Long felhasznaloId);
}
