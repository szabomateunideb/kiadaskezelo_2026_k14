package hu.unideb.inf.kiadaskezelo.service.mapper;

import hu.unideb.inf.kiadaskezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloDisplayDto;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloSaveDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FelhasznaloMapper {

    FelhasznaloDisplayDto felhasznaloEntityToDto(FelhasznaloEntity felhasznaloEntity);

    List<FelhasznaloDisplayDto> fEntityListToDtoList(List<FelhasznaloEntity> felhasznaloEntities);

    @Mapping(target = "jog", ignore = true)
    FelhasznaloEntity felhasznaloSaveDtoToEntity(FelhasznaloSaveDto dto);
}
