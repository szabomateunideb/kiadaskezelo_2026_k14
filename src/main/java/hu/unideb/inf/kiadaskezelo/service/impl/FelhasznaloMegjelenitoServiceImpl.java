package hu.unideb.inf.kiadaskezelo.service.impl;

import hu.unideb.inf.kiadaskezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.kiadaskezelo.data.repository.FelhasznaloRepository;
import hu.unideb.inf.kiadaskezelo.service.FelhasznaloMegjelenitoService;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloDisplayDto;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class FelhasznaloMegjelenitoServiceImpl
    implements FelhasznaloMegjelenitoService {

    private final FelhasznaloRepository repo;

    @Override
    public List<FelhasznaloDisplayDto> findAllFelhasznalo() {
        ArrayList<FelhasznaloDisplayDto> dto = new ArrayList <>();
        for(FelhasznaloEntity e : repo.findAll()){
            FelhasznaloDisplayDto d = new FelhasznaloDisplayDto();
            d.setNev(e.getNev());
            d.setEmail(e.getEmail());
            d.setNem(e.getNem());
            d.setSzuletesiDatum(e.getSzuletesiDatum());
            dto.add(d);
        }
        return dto;
    }

    @Override
    public FelhasznaloDisplayDto findFelhasznaloByNev(String nev) {
        /*return repo.findAll()
                .stream()
                .filter(f ->
                        f.getNev().equals(nev))
                .findFirst().orElse(null);*/

        return repo.findFelhasznaloEntityByNev(nev);
    }
}
