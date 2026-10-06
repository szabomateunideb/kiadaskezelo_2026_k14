package hu.unideb.inf.kiadaskezelo.service.impl;

import hu.unideb.inf.kiadaskezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.kiadaskezelo.data.repository.FelhasznaloRepository;
import hu.unideb.inf.kiadaskezelo.service.FelhasznaloCrudService;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloSaveDto;
import hu.unideb.inf.kiadaskezelo.service.mapper.FelhasznaloMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FelhasznaloCrudServiceImpl
        implements FelhasznaloCrudService {

    private final FelhasznaloRepository frepo;
    private final FelhasznaloMapper fmapper;

    @Override
    public FelhasznaloSaveDto save(FelhasznaloSaveDto dto) {
        FelhasznaloEntity e = frepo.save(
                fmapper.felhasznaloSaveDtoToEntity(dto));

        return fmapper.fEntityToSaveDto(e);
    }

    @Override
    public FelhasznaloSaveDto update(FelhasznaloSaveDto dto) {
        return null;
    }

    @Override
    public FelhasznaloSaveDto findByNev(String nev) {
        return null;
    }

    @Override
    public FelhasznaloSaveDto findAll() {
        return null;
    }

    @Override
    public void delete(FelhasznaloSaveDto dto) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public void deleteById(Long id) {

    }
}
