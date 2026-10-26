package hu.unideb.inf.kiadaskezelo.service.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.SzamlaEntity;
import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import hu.unideb.inf.kiadaskezelo.data.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EgyenlegService {
    private final PenzmozgasRepository movements;
    private final UtalasRepository transfers;

    public Map<Long, BigDecimal> valtozasok(Long owner) {
        Map<Long, BigDecimal> amounts = new HashMap<>();
        movements.egyenlegValtozasok(owner, PenzmozgasAllapot.KONYVELT, PenzmozgasTipus.BEVETEL)
                .forEach(row -> amounts.put(row.getSzamlaId(), row.getOsszeg()));
        transfers.beerkezoOsszegek(owner, PenzmozgasAllapot.KONYVELT)
                .forEach(row -> amounts.merge(row.getSzamlaId(), row.getOsszeg(), BigDecimal::add));
        return amounts;
    }
    public BigDecimal egyenleg(SzamlaEntity account) {
        return account.getNyitoEgyenleg().add(valtozasok(account.getFelhasznalo().getId())
                .getOrDefault(account.getId(), BigDecimal.ZERO));
    }
}
