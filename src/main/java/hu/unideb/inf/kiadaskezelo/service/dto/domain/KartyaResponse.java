package hu.unideb.inf.kiadaskezelo.service.dto.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

public record KartyaResponse(
        Long id, Long szamlaId, String elnevezes, String birtokosNeve, KartyaTarsasag kartyaTarsasag,
        String utolsoNegySzamjegy, int lejaratiHonap, int lejaratiEv, boolean aktiv
) {
}
