package hu.unideb.inf.kiadaskezelo.service.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FelhasznaloSaveDto {

    private Long id;
    @NotBlank
    @Size(min = 1, max = 100)
    private String nev;
    @NotNull
    @Past
    private LocalDate szuletesiDatum;
    @NotBlank
    @Size(min = 1, max = 20)
    private String felhasznalonev;
    @NotBlank
    @Size(min = 1, max = 20)
    private String jelszo;
    @NotBlank
    @Size(min = 1, max = 30)
    @Email
    private String email;
    @Max(10)
    private String nem;
}
