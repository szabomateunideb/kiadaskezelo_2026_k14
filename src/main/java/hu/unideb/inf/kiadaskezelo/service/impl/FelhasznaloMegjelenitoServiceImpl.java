package hu.unideb.inf.kiadaskezelo.service.impl;

import hu.unideb.inf.kiadaskezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.kiadaskezelo.data.repository.FelhasznaloRepository;
import hu.unideb.inf.kiadaskezelo.service.FelhasznaloMegjelenitoService;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloDisplayDto;
import hu.unideb.inf.kiadaskezelo.service.mapper.FelhasznaloMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class FelhasznaloMegjelenitoServiceImpl
    implements FelhasznaloMegjelenitoService {

    private final FelhasznaloRepository repo;
    private final FelhasznaloMapper mapper;

    @Override
    public List<FelhasznaloDisplayDto> findAllFelhasznalo() {
        return mapper.fEntityListToDtoList(repo.findAll());
    }

    @Override
    public FelhasznaloDisplayDto findFelhasznaloByNev(String nev) {
        return mapper
                .felhasznaloEntityToDto(repo
                        .findFelhasznaloEntityByNev(nev));
        /*return repo.findAll()
                .stream()
                .filter(f ->
                        f.getNev().equals(nev))
                .findFirst().orElse(null);*/

        //return null;
    }
}
