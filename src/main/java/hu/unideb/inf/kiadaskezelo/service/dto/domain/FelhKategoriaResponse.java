package hu.unideb.inf.kiadaskezelo.service.dto.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

public record FelhKategoriaResponse(
        Long kategoriaId, String nev, KategoriaTipus tipus, String sajatElnevezes, boolean aktiv
) {
}
