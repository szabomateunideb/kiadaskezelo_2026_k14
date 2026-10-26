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

import hu.unideb.inf.kiadaskezelo.data.entity.enums.KategoriaTipus;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name="Kategóriák")
public class KategoriaController {
    private final KategoriaService service;
    @GetMapping("/api/kategoriak")
    public Page<KategoriaResponse> list(@RequestParam(required=false) KategoriaTipus tipus,
            @PageableDefault(size=20, sort="nev") Pageable page) { return service.list(tipus, page); }
    @GetMapping("/api/kategoriak/{id}")
    public KategoriaResponse get(@PathVariable @Positive Long id) { return service.get(id); }
    @PostMapping("/api/kategoriak")
    public ResponseEntity<KategoriaResponse> create(@Valid @RequestBody KategoriaRequest request) {
        var value = service.create(request); return RestResponses.created(value, value.id());
    }
    @PutMapping("/api/kategoriak/{id}")
    public KategoriaResponse update(@PathVariable @Positive Long id, @Valid @RequestBody KategoriaRequest request) { return service.update(id, request); }
    @DeleteMapping("/api/kategoriak/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long id) { service.delete(id); }
    @GetMapping("/api/felhasznalok/{felhasznaloId}/kategoriak")
    public List<FelhKategoriaResponse> assigned(@PathVariable @Positive Long felhasznaloId) { return service.assigned(felhasznaloId); }
    @PutMapping("/api/felhasznalok/{felhasznaloId}/kategoriak/{id}")
    public FelhKategoriaResponse assign(@PathVariable @Positive Long felhasznaloId, @PathVariable @Positive Long id,
            @Valid @RequestBody FelhKategoriaRequest request) { return service.assign(felhasznaloId, id, request); }
}
