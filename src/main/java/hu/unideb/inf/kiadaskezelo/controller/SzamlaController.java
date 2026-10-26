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


@RestController
@RequestMapping("/api/felhasznalok/{felhasznaloId}/szamlak")
@RequiredArgsConstructor
@Tag(name="Szamla")
public class SzamlaController {
    private final SzamlaService service;
    @GetMapping
    public Page<SzamlaResponse> list(@PathVariable @Positive Long felhasznaloId, @PageableDefault(size=20,sort="id") Pageable page) {
        return service.list(felhasznaloId,page);
    }
    @GetMapping("/{id}")
    public SzamlaResponse get(@PathVariable @Positive Long felhasznaloId, @PathVariable @Positive Long id) { return service.get(felhasznaloId,id); }
    @PostMapping
    public ResponseEntity<SzamlaResponse> create(@PathVariable @Positive Long felhasznaloId, @Valid @RequestBody SzamlaCreateRequest request) {
        var value = service.create(felhasznaloId,request); return RestResponses.created(value,value.id());
    }
    @PutMapping("/{id}")
    public SzamlaResponse update(@PathVariable @Positive Long felhasznaloId, @PathVariable @Positive Long id, @Valid @RequestBody SzamlaUpdateRequest request) {
        return service.update(felhasznaloId,id,request);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long felhasznaloId, @PathVariable @Positive Long id) {
        service.aktiv(felhasznaloId,id,false);
    }
    @PatchMapping("/{id}/aktiv")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@PathVariable @Positive Long felhasznaloId, @PathVariable @Positive Long id, @Valid @RequestBody AktivRequest request) {
        service.aktiv(felhasznaloId,id,request.aktiv());
    }



}
