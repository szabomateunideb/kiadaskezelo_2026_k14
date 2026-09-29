package hu.unideb.inf.kiadaskezelo.data.repository;

import hu.unideb.inf.kiadaskezelo.data.entity.FelhasznaloEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FelhasznaloRepository extends
        JpaRepository<FelhasznaloEntity,Long> {

    FelhasznaloEntity findFelhasznaloEntityByNev(String felhasznaloNev);

    FelhasznaloEntity findAllByNemEmpty();

    //Select * from felhasznalo inner join jog
    //on jog_id = id
    //wher jog.nev = jogNev
    FelhasznaloEntity findAllByJog_Nev(String jogNev);

    @Query("SELECT f FROM FelhasznaloEntity f WHERE f.nem = ?1 " +
            "and f.jog.nev='FELHASZNALO'")
    FelhasznaloEntity findByJPQL(String nem);

    @Query(value = "SELECT * FROM FELHASZNALO f where f.nev =?1"
            , nativeQuery = true)
    FelhasznaloEntity findByNative(String nem);


}
