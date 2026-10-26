package hu.unideb.inf.kiadaskezelo.service.dto.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

public record IsmetlesRequest(
        @NotNull @Positive Long szamlaId, @NotNull @Positive Long kategoriaId,
        @NotBlank @Size(max=150) String megnevezes,
        @NotNull @Positive @Digits(integer=15,fraction=4) BigDecimal osszeg,
        @NotNull IsmetlodesGyakorisag gyakorisag, @Min(1) @Max(120) int intervallum,
        @NotNull LocalDate kezdoDatum, LocalDate vegdatum
) {
}
