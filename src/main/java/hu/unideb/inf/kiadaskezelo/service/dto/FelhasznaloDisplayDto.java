package hu.unideb.inf.kiadaskezelo.service.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class FelhasznaloDisplayDto {
    private String nev;
    private String email;
    private String nem;
    private LocalDate szuletesiDatum;
}
