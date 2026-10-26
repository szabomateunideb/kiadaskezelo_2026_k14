package hu.unideb.inf.kiadaskezelo.controller;
import hu.unideb.inf.kiadaskezelo.service.domain.*;
import hu.unideb.inf.kiadaskezelo.service.dto.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.time.*;
import java.util.List;

@RestController
@RequestMapping("/api/felhasznalok/{felhasznaloId}")
@RequiredArgsConstructor
@Tag(name="Pénzmozgások és utalások")
public class PenzmozgasController {
    private final PenzmozgasService service;

    @GetMapping("/penzmozgasok")
    public Page<PenzmozgasResponse> list(@PathVariable @Positive Long felhasznaloId,
            @RequestParam(required=false) @Positive Long szamlaId,
            @RequestParam(required=false) @Positive Long kategoriaId,
            @RequestParam(required=false) PenzmozgasTipus tipus,
            @RequestParam(required=false) PenzmozgasAllapot allapot,
            @RequestParam(required=false) Instant tol, @RequestParam(required=false) Instant ig,
            @PageableDefault(size=20,sort="datum",direction=Sort.Direction.DESC) Pageable page) {
        return service.list(felhasznaloId, szamlaId, kategoriaId, tipus, allapot, tol, ig, page);
    }
    @GetMapping("/penzmozgasok/{id}")
    public PenzmozgasResponse get(@PathVariable @Positive Long felhasznaloId, @PathVariable @Positive Long id) { return service.get(felhasznaloId,id); }
    @PostMapping("/penzmozgasok")
    public ResponseEntity<PenzmozgasResponse> create(@PathVariable @Positive Long felhasznaloId,
            @Valid @RequestBody PenzmozgasRequest request) {
        var value = service.create(felhasznaloId,request); return RestResponses.created(value,value.id());
    }
    @PutMapping("/penzmozgasok/{id}")
    public PenzmozgasResponse update(@PathVariable @Positive Long felhasznaloId, @PathVariable @Positive Long id,
            @Valid @RequestBody PenzmozgasRequest request) { return service.update(felhasznaloId,id,request); }
    @PostMapping("/penzmozgasok/{id}/konyveles")
    public PenzmozgasResponse book(@PathVariable @Positive Long felhasznaloId, @PathVariable @Positive Long id) { return service.konyvel(felhasznaloId,id); }
    @PostMapping("/penzmozgasok/{id}/sztorno")
    public PenzmozgasResponse cancel(@PathVariable @Positive Long felhasznaloId, @PathVariable @Positive Long id) { return service.sztorno(felhasznaloId,id); }
    @PatchMapping("/penzmozgasok/{id}/kategoria")
    public PenzmozgasResponse categorize(@PathVariable @Positive Long felhasznaloId, @PathVariable @Positive Long id,
            @Valid @RequestBody KategorizalasRequest request) { return service.kategorizal(felhasznaloId,id,request.kategoriaId()); }
    @PostMapping("/atvezetesek")
    public ResponseEntity<UtalasResponse> transfer(@PathVariable @Positive Long felhasznaloId, @Valid @RequestBody AtvezetesRequest request) {
        return createdTransfer(felhasznaloId, service.atvezetes(felhasznaloId,request));
    }
    @PostMapping("/utalasok")
    public ResponseEntity<UtalasResponse> external(@PathVariable @Positive Long felhasznaloId, @Valid @RequestBody KulsoUtalasRequest request) {
        return createdTransfer(felhasznaloId, service.kulsoUtalas(felhasznaloId,request));
    }
    @GetMapping("/utalasok")
    public Page<UtalasResponse> transfers(@PathVariable @Positive Long felhasznaloId, @PageableDefault(size=20,sort="id") Pageable page) { return service.utalasok(felhasznaloId,page); }
    @GetMapping("/utalasok/{id}")
    public UtalasResponse transfer(@PathVariable @Positive Long felhasznaloId, @PathVariable @Positive Long id) { return service.utalas(felhasznaloId,id); }
    @GetMapping("/statisztika")
    public List<KategoriaStatisztikaResponse> stats(@PathVariable @Positive Long felhasznaloId,
            @RequestParam LocalDate tol, @RequestParam LocalDate ig) { return service.statisztika(felhasznaloId,tol,ig); }

    private ResponseEntity<UtalasResponse> createdTransfer(Long owner, UtalasResponse response) {
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/felhasznalok/{owner}/utalasok/{id}").buildAndExpand(owner,response.id()).toUri()).body(response);
    }
}
