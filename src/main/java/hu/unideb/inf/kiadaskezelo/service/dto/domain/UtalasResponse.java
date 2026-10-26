package hu.unideb.inf.kiadaskezelo.service.dto.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

public record UtalasResponse(
        Long id, PenzmozgasResponse penzmozgas, Long celSzamlaId, BigDecimal celOsszeg,
        String partnerNev, String partnerIban, String kozlemeny, String kulsoAzonosito
) {
}
