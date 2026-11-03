package hu.unideb.inf.kiadaskezelo.service.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record FelhasznaloLoginRequest(
        @NotBlank @Size(max = 60) String felhasznalonev,
        @NotBlank @Size(max = 100) String jelszo
) {
}
