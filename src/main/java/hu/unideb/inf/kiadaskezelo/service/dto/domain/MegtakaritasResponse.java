package hu.unideb.inf.kiadaskezelo.service.dto.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

public record MegtakaritasResponse(
        Long id, Long felhasznaloId, String nev, String penznem, BigDecimal kezdoToke,
        BigDecimal haviBefizetes, BigDecimal evesKamatSzazalek, int futamidoHonap, BigDecimal celosszeg
) {
}
