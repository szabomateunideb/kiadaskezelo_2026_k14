package hu.unideb.inf.kiadaskezelo.service.dto;

import jakarta.persistence.Column;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FelhasznaloSaveDto {

    private Long id;
    private String nev;
    private LocalDate szuletesiDatum;
    private String felhasznalonev;
    private String jelszo;
    private String email;
    private String nem;
}
