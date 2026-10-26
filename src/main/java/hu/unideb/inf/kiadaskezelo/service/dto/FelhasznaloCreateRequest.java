package hu.unideb.inf.kiadaskezelo.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record FelhasznaloCreateRequest(
        @NotBlank @Size(max = 100) String nev,
        @NotNull @Past LocalDate szuletesiDatum,
        @NotBlank @Size(max = 60) String felhasznalonev,
        @NotBlank @Size(max = 100) String jelszo,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 10) String nem
) {
}
