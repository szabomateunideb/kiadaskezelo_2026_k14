package hu.unideb.inf.kiadaskezelo.service.dto.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

public record MegtakaritasRequest(
        @NotBlank @Size(max=100) String nev, @NotBlank @Pattern(regexp="[A-Z]{3}") String penznem,
        @NotNull @PositiveOrZero @Digits(integer=15,fraction=4) BigDecimal kezdoToke,
        @NotNull @PositiveOrZero @Digits(integer=15,fraction=4) BigDecimal haviBefizetes,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") @Digits(integer=5,fraction=4) BigDecimal evesKamatSzazalek,
        @Min(1) @Max(1200) int futamidoHonap, @Positive @Digits(integer=15,fraction=4) BigDecimal celosszeg
) {
}
