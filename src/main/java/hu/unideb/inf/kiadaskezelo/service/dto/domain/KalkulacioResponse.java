package hu.unideb.inf.kiadaskezelo.service.dto.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public record KalkulacioResponse(
        String penznem, BigDecimal befizetesOsszesen, BigDecimal varhatoVegosszeg,
        BigDecimal kamatOsszesen, BigDecimal celhozHianyzoOsszeg, List<HaviEgyenleg> honapok
) {
}
