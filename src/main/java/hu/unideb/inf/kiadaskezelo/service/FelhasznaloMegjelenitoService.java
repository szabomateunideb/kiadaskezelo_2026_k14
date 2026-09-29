package hu.unideb.inf.kiadaskezelo.service;

import hu.unideb.inf.kiadaskezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloDisplayDto;

import java.util.List;

public interface FelhasznaloMegjelenitoService {

    List<FelhasznaloDisplayDto> findAllFelhasznalo();

    FelhasznaloDisplayDto findFelhasznaloByNev(String nev);
}
