package hu.unideb.inf.kiadaskezelo.service.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.*;
import hu.unideb.inf.kiadaskezelo.data.entity.enums.SzamlaTipus;
import hu.unideb.inf.kiadaskezelo.data.repository.*;
import hu.unideb.inf.kiadaskezelo.service.dto.domain.*;
import hu.unideb.inf.kiadaskezelo.service.exception.*;
import hu.unideb.inf.kiadaskezelo.service.mapper.PenzugyiMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.math.BigDecimal;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SzamlaService {
    private final SzamlaRepository accounts;
    private final KartyaRepository cards;
    private final IsmetlodoPenzmozgasRepository schedules;
    private final DomainLookup lookup;
    private final EgyenlegService balances;
    private final PenzugyiMapper mapper;

    public Page<SzamlaResponse> list(Long owner, Pageable pageable) {
        lookup.user(owner);
        var changes = balances.valtozasok(owner);
        return accounts.findAllByFelhasznalo_Id(owner, Lapozas.korlatoz(pageable, "id", "nev", "penznem", "tipus"))
                .map(s -> mapper.szamla(s, s.getNyitoEgyenleg().add(changes.getOrDefault(s.getId(), BigDecimal.ZERO))));
    }
    public SzamlaResponse get(Long owner, Long id) {
        lookup.user(owner);
        var entity = lookup.account(owner, id);
        return mapper.szamla(entity, balances.egyenleg(entity));
    }
    @Transactional
    public SzamlaResponse create(Long owner, @NotNull @Valid SzamlaCreateRequest request) {
        var user = lookup.activeUser(owner);
        lookup.currency(request.penznem());
        bankAdatok(request.tipus(), request.bankNev(), request.iban());
        var entity = new SzamlaEntity();
        entity.setFelhasznalo(user);
        entity.setNev(request.nev().strip());
        entity.setTipus(request.tipus());
        entity.setPenznem(request.penznem());
        entity.setNyitoEgyenleg(request.nyitoEgyenleg());
        entity.setNyitasDatum(request.nyitasDatum());
        entity.setBankNev(request.bankNev());
        entity.setIban(request.iban());
        accounts.saveAndFlush(entity);
        return mapper.szamla(entity, entity.getNyitoEgyenleg());
    }
    @Transactional
    public SzamlaResponse update(Long owner, Long id, @NotNull @Valid SzamlaUpdateRequest request) {
        var entity = lookup.lockAccounts(owner, id).getFirst();
        bankAdatok(entity.getTipus(), request.bankNev(), request.iban());
        entity.setNev(request.nev().strip());
        entity.setBankNev(request.bankNev());
        entity.setIban(request.iban());
        return mapper.szamla(entity, balances.egyenleg(entity));
    }
    @Transactional
    public void aktiv(Long owner, Long id, boolean active) {
        var entity = lookup.lockAccounts(owner, id).getFirst();
        if (!active) {
            if (balances.egyenleg(entity).signum() != 0) {
                throw new UzletiSzabalyException("Csak nulla egyenlegű számla archiválható.");
            }
            if (schedules.existsBySzamla_IdAndAktivTrue(id)) {
                throw new UzletiSzabalyException("Előbb állítsa le a számla aktív ismétlődéseit.");
            }
            cards.findAllBySzamla_Id(id).forEach(card -> card.setAktiv(false));
        }
        entity.setAktiv(active);
    }
    private void bankAdatok(SzamlaTipus type, String bank, String iban) {
        if (type == SzamlaTipus.KESZPENZ && ((bank != null && !bank.isBlank()) || (iban != null && !iban.isBlank()))) {
            throw new HibasKeresException("Készpénzes számlához nem tartozhat bank vagy IBAN.");
        }
    }
}
