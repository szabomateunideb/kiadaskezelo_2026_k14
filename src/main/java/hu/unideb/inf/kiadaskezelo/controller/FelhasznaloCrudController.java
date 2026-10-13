package hu.unideb.inf.kiadaskezelo.controller;

import hu.unideb.inf.kiadaskezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.kiadaskezelo.service.FelhasznaloCrudService;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloSaveDto;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/felhasznalo")
@AllArgsConstructor
public class FelhasznaloCrudController {

    private final FelhasznaloCrudService cService;

    @PostMapping
    FelhasznaloSaveDto save(@RequestBody FelhasznaloSaveDto dto) {
        return cService.save(dto);
    }

    /*@GetMapping
    List<FelhasznaloSaveDto> findAll() {
        return cService.findAll();
    }

    @GetMapping
    FelhasznaloSaveDto findByNev(@RequestParam String nev){
        return cService.findByNev(nev);
    }*/

    @GetMapping
    List<FelhasznaloSaveDto> findAllByNev(
            @RequestParam(required = false) String nev){
        System.out.println("Nev: " + nev);
        if(nev == null){
            return cService.findAll();
        } else {
            return List.of(cService.findByNev(nev));
        }
    }

    @GetMapping("/{id}")
    FelhasznaloSaveDto findOneById(@PathVariable Long id){
        return cService.findAll()
                .stream()
                .filter(f -> f.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    FelhasznaloSaveDto update(@RequestBody FelhasznaloSaveDto dto) {
        return null;
    }

    void deleteById(Long id){}
    void delete(FelhasznaloSaveDto dto){}
    void deleteAll(){}

}
