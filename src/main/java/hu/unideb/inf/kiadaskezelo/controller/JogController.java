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
@RequestMapping("/api/jogosultsagok")
@RequiredArgsConstructor
@Tag(name="Jogosultság törzsadatok")
public class JogController {
    private final JogService service;
    @GetMapping
    public Page<JogResponse> list(@PageableDefault(size=20,sort="id") Pageable page) { return service.list(page); }
    @GetMapping("/{id}")
    public JogResponse get(@PathVariable @Positive Long id) { return service.get(id); }
    @PostMapping
    public ResponseEntity<JogResponse> create(@Valid @RequestBody JogRequest request) {
        var value = service.save(null, request); return RestResponses.created(value, value.id());
    }
    @PutMapping("/{id}")
    public JogResponse update(@PathVariable @Positive Long id, @Valid @RequestBody JogRequest request) { return service.save(id, request); }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long id) { service.delete(id); }
}
