package hu.unideb.inf.kiadaskezelo.service;

import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloSaveDto;

public interface FelhasznaloCrudService {

    FelhasznaloSaveDto save(FelhasznaloSaveDto dto);
    FelhasznaloSaveDto update(FelhasznaloSaveDto dto);
    FelhasznaloSaveDto findByNev(String nev);
    FelhasznaloSaveDto findAll();
    void delete(FelhasznaloSaveDto dto);
    void deleteAll();
    void deleteById(Long id);
}
