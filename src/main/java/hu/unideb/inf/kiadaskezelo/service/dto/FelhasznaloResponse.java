package hu.unideb.inf.kiadaskezelo.service.dto;

import java.time.LocalDate;

public record FelhasznaloResponse(
        Long id,
        String nev,
        LocalDate szuletesiDatum,
        String felhasznalonev,
        String email,
        String nem,
        String jogNev
) {
}
