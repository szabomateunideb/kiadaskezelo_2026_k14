package hu.unideb.inf.kiadaskezelo.service.dto.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

public record PenzmozgasRequest(
        @NotNull @Positive Long szamlaId, @Positive Long kategoriaId, @Positive Long kartyaId,
        @NotNull PenzmozgasTipus tipus, @NotNull @Positive @Digits(integer=15,fraction=4) BigDecimal osszeg,
        @NotNull Instant datum, @NotNull PenzmozgasAllapot allapot,
        @NotBlank @Size(max=150) String megnevezes, @Size(max=2000) String megjegyzes
) {
}
