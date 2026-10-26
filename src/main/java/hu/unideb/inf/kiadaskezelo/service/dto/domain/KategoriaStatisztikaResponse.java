package hu.unideb.inf.kiadaskezelo.service.dto.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

public record KategoriaStatisztikaResponse(
        Long kategoriaId, String kategoriaNev, PenzmozgasTipus tipus,
        String penznem, BigDecimal osszeg, long tetelszam
) {
}
