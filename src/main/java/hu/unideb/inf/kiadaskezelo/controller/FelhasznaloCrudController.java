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

    @GetMapping
    List<FelhasznaloSaveDto> findAll() {
        return cService.findAll();
    }

    FelhasznaloSaveDto findByNev(String nev){
        return null;
    }

    FelhasznaloSaveDto update(@RequestBody FelhasznaloSaveDto dto) {
        return null;
    }

    void deleteById(Long id){}
    void delete(FelhasznaloSaveDto dto){}
    void deleteAll(){}

}
