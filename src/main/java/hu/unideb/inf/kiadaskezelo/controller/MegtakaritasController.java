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
@RequestMapping("/api/felhasznalok/{felhasznaloId}/megtakaritasok")
@RequiredArgsConstructor
@Tag(name="Megtakaritas")
public class MegtakaritasController {
    private final MegtakaritasService service;
    @GetMapping
    public Page<MegtakaritasResponse> list(@PathVariable @Positive Long felhasznaloId, @PageableDefault(size=20,sort="id") Pageable page) {
        return service.list(felhasznaloId,page);
    }
    @GetMapping("/{id}")
    public MegtakaritasResponse get(@PathVariable @Positive Long felhasznaloId, @PathVariable @Positive Long id) { return service.get(felhasznaloId,id); }
    @PostMapping
    public ResponseEntity<MegtakaritasResponse> create(@PathVariable @Positive Long felhasznaloId, @Valid @RequestBody MegtakaritasRequest request) {
        var value = service.create(felhasznaloId,request); return RestResponses.created(value,value.id());
    }
    @PutMapping("/{id}")
    public MegtakaritasResponse update(@PathVariable @Positive Long felhasznaloId, @PathVariable @Positive Long id, @Valid @RequestBody MegtakaritasRequest request) {
        return service.update(felhasznaloId,id,request);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long felhasznaloId, @PathVariable @Positive Long id) {
        service.delete(felhasznaloId,id);
    }


    @GetMapping("/{id}/kalkulacio")
    public KalkulacioResponse calculate(@PathVariable @Positive Long felhasznaloId, @PathVariable @Positive Long id) {
        return service.kalkulalMentett(felhasznaloId,id);
    }

}
