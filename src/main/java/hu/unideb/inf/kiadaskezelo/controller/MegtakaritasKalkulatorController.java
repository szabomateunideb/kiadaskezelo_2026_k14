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
@RequestMapping("/api/megtakaritas-kalkulator")
@RequiredArgsConstructor
@Tag(name="Megtakarítás kalkulátor")
public class MegtakaritasKalkulatorController {
    private final MegtakaritasService service;
    @PostMapping
    public KalkulacioResponse calculate(@Valid @RequestBody MegtakaritasRequest request) { return service.kalkulal(request); }
}
