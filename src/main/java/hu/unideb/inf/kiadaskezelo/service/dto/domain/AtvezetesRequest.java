package hu.unideb.inf.kiadaskezelo.service.dto.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

public record AtvezetesRequest(
        @NotNull @Positive Long forrasSzamlaId, @NotNull @Positive Long celSzamlaId,
        @NotNull @Positive @Digits(integer=15,fraction=4) BigDecimal osszeg,
        @NotNull @Positive @Digits(integer=15,fraction=4) BigDecimal celOsszeg,
        @NotNull Instant datum, @NotNull PenzmozgasAllapot allapot, @NotBlank @Size(max=150) String megnevezes,
        @Size(max=500) String kozlemeny
) {
}
