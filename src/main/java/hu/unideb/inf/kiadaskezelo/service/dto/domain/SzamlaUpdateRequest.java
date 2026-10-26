package hu.unideb.inf.kiadaskezelo.service.dto.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

public record SzamlaUpdateRequest(
        @NotBlank @Size(max=100) String nev, @Size(max=100) String bankNev, @Size(max=34) String iban
) {
}
