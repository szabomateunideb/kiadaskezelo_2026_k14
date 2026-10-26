package hu.unideb.inf.kiadaskezelo.service.mapper;

import hu.unideb.inf.kiadaskezelo.data.entity.*;
import hu.unideb.inf.kiadaskezelo.service.dto.domain.*;
import org.mapstruct.*;
import java.math.BigDecimal;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PenzugyiMapper {
    @Mapping(target="felhasznaloId", source="entity.felhasznalo.id")
    @Mapping(target="egyenleg", source="egyenleg")
    SzamlaResponse szamla(SzamlaEntity entity, BigDecimal egyenleg);

    @Mapping(target="szamlaId", source="szamla.id")
    KartyaResponse kartya(KartyaEntity entity);

    KategoriaResponse kategoria(KategoriaEntity entity);

    @Mapping(target="kategoriaId", source="kategoria.id")
    @Mapping(target="nev", source="kategoria.nev")
    @Mapping(target="tipus", source="kategoria.tipus")
    FelhKategoriaResponse felhKategoria(FelhKategoriaEntity entity);

    @Mapping(target="szamlaId", source="szamla.id")
    @Mapping(target="penznem", source="szamla.penznem")
    @Mapping(target="kategoriaId", source="kategoria.id")
    @Mapping(target="kategoriaNev", source="kategoria.nev")
    @Mapping(target="kartyaId", source="kartya.id")
    @Mapping(target="ismetlesId", source="ismetles.id")
    PenzmozgasResponse penzmozgas(PenzmozgasEntity entity);

    @Mapping(target="celSzamlaId", source="celSzamla.id")
    UtalasResponse utalas(UtalasEntity entity);

    @Mapping(target="szamlaId", source="szamla.id")
    @Mapping(target="kategoriaId", source="kategoria.id")
    IsmetlesResponse ismetles(IsmetlodoPenzmozgasEntity entity);

    @Mapping(target="felhasznaloId", source="felhasznalo.id")
    MegtakaritasResponse megtakaritas(MegtakaritasiTervEntity entity);

    JogResponse jog(JogEntity entity);
}
