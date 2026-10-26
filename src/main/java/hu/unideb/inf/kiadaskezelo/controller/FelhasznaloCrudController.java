package hu.unideb.inf.kiadaskezelo.controller;

import hu.unideb.inf.kiadaskezelo.service.FelhasznaloCrudService;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloCreateRequest;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloResponse;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/felhasznalo")
@RequiredArgsConstructor
public class FelhasznaloCrudController {

    private final FelhasznaloCrudService cService;

    @PatchMapping("/{id}/aktiv")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void aktiv(@PathVariable @Positive Long id,
            @Valid @RequestBody hu.unideb.inf.kiadaskezelo.service.dto.domain.AktivRequest request) {
        cService.aktiv(id, request.aktiv());
    }

    @PostMapping
    public ResponseEntity<FelhasznaloResponse> save(@Valid @RequestBody FelhasznaloCreateRequest request) {
        FelhasznaloResponse response = cService.save(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping({"", "/felhasznalok"})
    public Page<FelhasznaloResponse> findAllByNev(
            @RequestParam(required = false) @Size(max = 100) String nev,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return cService.findAll(nev, pageable);
    }

    @GetMapping("/{id}")
    public FelhasznaloResponse findOneById(@PathVariable @Positive Long id) {
        return cService.findById(id);
    }

    @PutMapping("/{id}")
    public FelhasznaloResponse update(@PathVariable @Positive Long id,
                                      @Valid @RequestBody FelhasznaloUpdateRequest request) {
        return cService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable @Positive Long id) {
        cService.deleteById(id);
    }

    @DeleteMapping("/felhasznalok")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAll() {
        cService.deleteAll();
    }
}
