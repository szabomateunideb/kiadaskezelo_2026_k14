package hu.unideb.inf.kiadaskezelo.service.dto.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

public record SzamlaCreateRequest(
        @NotBlank @Size(max=100) String nev, @NotNull SzamlaTipus tipus,
        @NotBlank @Pattern(regexp="[A-Z]{3}") String penznem,
        @NotNull @PositiveOrZero @Digits(integer=15,fraction=4) BigDecimal nyitoEgyenleg,
        @NotNull @PastOrPresent LocalDate nyitasDatum, @Size(max=100) String bankNev, @Size(max=34) String iban
) {
}
