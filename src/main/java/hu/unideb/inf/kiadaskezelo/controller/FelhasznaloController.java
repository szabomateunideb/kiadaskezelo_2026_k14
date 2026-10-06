package hu.unideb.inf.kiadaskezelo.controller;

import hu.unideb.inf.kiadaskezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.kiadaskezelo.data.entity.JogEntity;
import hu.unideb.inf.kiadaskezelo.data.repository.FelhasznaloRepository;
import hu.unideb.inf.kiadaskezelo.data.repository.JogRepository;
import hu.unideb.inf.kiadaskezelo.service.FelhasznaloMegjelenitoService;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloDisplayDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("api/felhasznalo")
public class FelhasznaloController {

    //@Autowired   -- field injection - mező
    //private FelhasznaloRepository felhasznaloRepository;
    final FelhasznaloRepository repo;
    final JogRepository jogRepo;
    final FelhasznaloMegjelenitoService service;

    public FelhasznaloController(FelhasznaloRepository repo
            ,  JogRepository jogRepo
            , FelhasznaloMegjelenitoService service) {
        this.repo = repo;
        this.jogRepo = jogRepo;
        this.service = service;
    }

    @GetMapping("/felhasznalok")
    public List<FelhasznaloDisplayDto> findAll() {
        return service.findAllFelhasznalo();
    }

    @GetMapping("/init")
    public FelhasznaloEntity saveMock(){

        JogEntity jogEntity = new JogEntity();
        jogEntity.setNev("user");
        jogEntity.setLeiras("minden felhasznalo megkapja");
        jogEntity = jogRepo.save(jogEntity);

        FelhasznaloEntity entity = new FelhasznaloEntity();
        entity.setJog(jogEntity);
        entity.setEmail("xy@mail.com");
        entity.setNev("Józsi");
        entity.setFelhasznalonev("jozsi01");
        entity.setJelszo("password");
        entity.setSzuletesiDatum(LocalDate.now());

        //van id
        entity = repo.save(entity);
        return entity;

    }
}
