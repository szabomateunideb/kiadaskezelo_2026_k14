package hu.unideb.inf.kiadaskezelo.service.dto.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

public record KartyaRequest(
        @NotNull @Positive Long szamlaId, @NotBlank @Size(max=100) String elnevezes,
        @NotBlank @Size(max=100) String birtokosNeve, @NotNull KartyaTarsasag kartyaTarsasag,
        @NotBlank @Pattern(regexp="[0-9]{4}") String utolsoNegySzamjegy,
        @Min(1) @Max(12) int lejaratiHonap, @Min(2000) @Max(9999) int lejaratiEv
) {
}
