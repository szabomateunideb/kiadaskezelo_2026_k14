package hu.unideb.inf.kiadaskezelo.controller;

import hu.unideb.inf.kiadaskezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.kiadaskezelo.service.FelhasznaloCrudService;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloSaveDto;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/felhasznalo")
@AllArgsConstructor
public class FelhasznaloCrudController {

    private final FelhasznaloCrudService cService;

    @PostMapping
    FelhasznaloSaveDto save(@Valid
                            @RequestBody FelhasznaloSaveDto dto) {
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

    @PutMapping
    FelhasznaloSaveDto update(@Valid
                              @RequestBody FelhasznaloSaveDto dto) {
        return cService.update(dto);
    }

    @DeleteMapping("/{id}")
    void deleteById(@PathVariable Long id){cService.deleteById(id);}
    @DeleteMapping
    ResponseEntity delete(@RequestBody FelhasznaloSaveDto dto){
        cService.delete(dto);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }
    @DeleteMapping("/felhasznalok")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAll(){ cService.deleteAll();}

}
