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

import java.util.List;

@RestController
@RequestMapping("/api/felhasznalok/{felhasznaloId}/ismetlesek")
@RequiredArgsConstructor
@Tag(name="Ismetlodes")
public class IsmetlodesController {
    private final IsmetlodesService service;
    @GetMapping
    public Page<IsmetlesResponse> list(@PathVariable @Positive Long felhasznaloId, @PageableDefault(size=20,sort="id") Pageable page) {
        return service.list(felhasznaloId,page);
    }
    @GetMapping("/{id}")
    public IsmetlesResponse get(@PathVariable @Positive Long felhasznaloId, @PathVariable @Positive Long id) { return service.get(felhasznaloId,id); }
    @PostMapping
    public ResponseEntity<IsmetlesResponse> create(@PathVariable @Positive Long felhasznaloId, @Valid @RequestBody IsmetlesRequest request) {
        var value = service.create(felhasznaloId,request); return RestResponses.created(value,value.id());
    }
    @PutMapping("/{id}")
    public IsmetlesResponse update(@PathVariable @Positive Long felhasznaloId, @PathVariable @Positive Long id, @Valid @RequestBody IsmetlesRequest request) {
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

    @PostMapping("/{id}/feldolgozas")
    public List<PenzmozgasResponse> process(@PathVariable @Positive Long felhasznaloId, @PathVariable @Positive Long id) {
        return service.feldolgoz(felhasznaloId,id);
    }


}
