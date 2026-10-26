package hu.unideb.inf.kiadaskezelo.service.dto.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

public record IsmetlesResponse(
        Long id, Long szamlaId, Long kategoriaId, String megnevezes, KategoriaTipus tipus,
        BigDecimal osszeg, IsmetlodesGyakorisag gyakorisag, int intervallum, LocalDate kezdoDatum,
        LocalDate kovetkezoDatum, LocalDate vegdatum, boolean aktiv
) {
}
