package hu.unideb.inf.kiadaskezelo.service.mapper;

import hu.unideb.inf.kiadaskezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloCreateRequest;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloResponse;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FelhasznaloMapper {

    @Mapping(target = "jogNev", source = "jog.nev")
    FelhasznaloResponse toResponse(FelhasznaloEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jog", ignore = true)
    FelhasznaloEntity toEntity(FelhasznaloCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jog", ignore = true)
    @Mapping(target = "jelszo", ignore = true)
    void updateEntity(FelhasznaloUpdateRequest request, @MappingTarget FelhasznaloEntity entity);
}
