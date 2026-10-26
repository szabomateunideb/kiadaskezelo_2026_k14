package hu.unideb.inf.kiadaskezelo.service.dto.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

public record KulsoUtalasRequest(
        @NotNull @Valid PenzmozgasRequest penzmozgas,
        @NotBlank @Size(max=150) String partnerNev, @NotBlank @Size(max=34) String partnerIban,
        @Size(max=500) String kozlemeny, @Size(max=100) String kulsoAzonosito
) {
}
