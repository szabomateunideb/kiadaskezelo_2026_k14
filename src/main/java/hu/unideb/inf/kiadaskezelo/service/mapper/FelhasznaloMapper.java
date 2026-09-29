package hu.unideb.inf.kiadaskezelo.service.mapper;

import hu.unideb.inf.kiadaskezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloDisplayDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FelhasznaloMapper {

    FelhasznaloDisplayDto felhasznaloEntityToDto(FelhasznaloEntity felhasznaloEntity);

    List<FelhasznaloDisplayDto> fEntityListToDtoList(List<FelhasznaloEntity> felhasznaloEntities);
}
