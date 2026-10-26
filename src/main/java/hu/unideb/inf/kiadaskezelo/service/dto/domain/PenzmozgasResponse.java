package hu.unideb.inf.kiadaskezelo.service.dto.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

public record PenzmozgasResponse(
        Long id, Long szamlaId, String penznem, Long kategoriaId, String kategoriaNev,
        Long kartyaId, Long ismetlesId, LocalDate esedekesseg, PenzmozgasTipus tipus,
        BigDecimal osszeg, Instant datum, PenzmozgasAllapot allapot, String megnevezes, String megjegyzes
) {
}
