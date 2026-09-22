package hu.unideb.inf.kiadaskezelo.controller;

import hu.unideb.inf.kiadaskezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.kiadaskezelo.data.repository.FelhasznaloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("api/felhasznalo")
public class FelhasznaloController {

    //@Autowired   -- field injection - mező
    //private FelhasznaloRepository felhasznaloRepository;
    final FelhasznaloRepository repo;

    public FelhasznaloController(FelhasznaloRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/init")
    public FelhasznaloEntity saveMock(){

        FelhasznaloEntity entity = new FelhasznaloEntity();
        entity.setEmail("xy@mail.com");
        entity.setNev("Józsi");
        entity.setFelhasznalonev("jozsi01");
        entity.setJelszo("password");
        entity.setSzuletesiDatum(LocalDate.now());

        return repo.save(entity);

    }
}
