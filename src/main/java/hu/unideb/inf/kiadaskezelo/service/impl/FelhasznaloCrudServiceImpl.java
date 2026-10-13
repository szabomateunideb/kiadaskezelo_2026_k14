package hu.unideb.inf.kiadaskezelo.service.impl;

import hu.unideb.inf.kiadaskezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.kiadaskezelo.data.entity.JogEntity;
import hu.unideb.inf.kiadaskezelo.data.repository.FelhasznaloRepository;
import hu.unideb.inf.kiadaskezelo.service.FelhasznaloCrudService;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloSaveDto;
import hu.unideb.inf.kiadaskezelo.service.mapper.FelhasznaloMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.cfg.MapperBuilder;

import java.util.List;

@Service
@AllArgsConstructor
public class FelhasznaloCrudServiceImpl
        implements FelhasznaloCrudService {

    private final FelhasznaloRepository frepo;
    private final FelhasznaloMapper fmapper;

    @Override
    public FelhasznaloSaveDto save(FelhasznaloSaveDto dto) {
        System.out.println(dto.toString());
        System.out.println(fmapper.felhasznaloSaveDtoToEntity(dto));
        FelhasznaloEntity e = frepo.save(
                fmapper.felhasznaloSaveDtoToEntity(dto));

        return fmapper.fEntityToSaveDto(e);
    }

    @Override
    public FelhasznaloSaveDto update(FelhasznaloSaveDto dto) {

        FelhasznaloEntity e = frepo.findFelhasznaloEntityByNev(dto.getNev());
        JogEntity j = e.getJog();
        e = fmapper.felhasznaloSaveDtoToEntity(dto);
        e.setJog(j);
        e = frepo.save(e);
        return fmapper.fEntityToSaveDto(e);
    }

    @Override
    public FelhasznaloSaveDto findByNev(String nev) {
        FelhasznaloEntity e =frepo.findFelhasznaloEntityByNev(nev);

        return fmapper.fEntityToSaveDto(e);
    }

    @Override
    public List<FelhasznaloSaveDto> findAll() {
        return fmapper.fEntityListToSaveDtoList(frepo.findAll());
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
