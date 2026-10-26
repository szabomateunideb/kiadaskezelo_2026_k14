package hu.unideb.inf.kiadaskezelo.service.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.*;
import hu.unideb.inf.kiadaskezelo.data.entity.enums.SzamlaTipus;
import hu.unideb.inf.kiadaskezelo.data.repository.KartyaRepository;
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
import java.time.*;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KartyaService {
    private final KartyaRepository cards;
    private final DomainLookup lookup;
    private final PenzugyiMapper mapper;
    private final Clock clock;
    private final jakarta.persistence.EntityManager entityManager;

    public Page<KartyaResponse> list(Long owner, Pageable page) {
        lookup.user(owner);
        return cards.findAllBySzamla_Felhasznalo_Id(owner, Lapozas.korlatoz(page, "id", "elnevezes", "lejaratiEv"))
                .map(mapper::kartya);
    }
    public KartyaResponse get(Long owner, Long id) {
        lookup.user(owner);
        return mapper.kartya(lookup.card(owner, id));
    }
    @Transactional
    public KartyaResponse create(Long owner, @NotNull @Valid KartyaRequest request) {
        var account = lookup.lockAccounts(owner, request.szamlaId()).getFirst();
        validate(account, request);
        var entity = new KartyaEntity();
        entity.setSzamla(account);
        apply(entity, request);
        return mapper.kartya(cards.saveAndFlush(entity));
    }
    @Transactional
    public KartyaResponse update(Long owner, Long id, @NotNull @Valid KartyaRequest request) {
        var original = lookup.card(owner, id);
        if (!original.getSzamla().getId().equals(request.szamlaId())) {
            throw new HibasKeresException("A kártya nem helyezhető át másik számlára.");
        }
        var account = lookup.lockAccounts(owner, request.szamlaId()).getFirst();
        validate(account, request);
        entityManager.refresh(original);
        apply(original, request);
        return mapper.kartya(original);
    }
    @Transactional
    public void aktiv(Long owner, Long id, boolean active) {
        var entity = lookup.card(owner, id);
        var account = lookup.lockAccounts(owner, entity.getSzamla().getId()).getFirst();
        entityManager.refresh(entity);
        if (active) {
            lookup.activeAccount(account);
            if (YearMonth.of(entity.getLejaratiEv(), entity.getLejaratiHonap()).isBefore(YearMonth.now(clock))) {
                throw new UzletiSzabalyException("Lejárt kártya nem aktiválható.");
            }
        }
        entity.setAktiv(active);
    }
    private void validate(SzamlaEntity account, KartyaRequest request) {
        lookup.activeAccount(account);
        if (account.getTipus() != SzamlaTipus.BANKSZAMLA) {
            throw new HibasKeresException("Kártya csak bankszámlához kapcsolható.");
        }
        if (YearMonth.of(request.lejaratiEv(), request.lejaratiHonap()).isBefore(YearMonth.now(clock))) {
            throw new HibasKeresException("A kártya lejárata nem lehet múltbeli.");
        }
    }
    private void apply(KartyaEntity entity, KartyaRequest request) {
        entity.setElnevezes(request.elnevezes().strip());
        entity.setBirtokosNeve(request.birtokosNeve().strip());
        entity.setKartyaTarsasag(request.kartyaTarsasag());
        entity.setUtolsoNegySzamjegy(request.utolsoNegySzamjegy());
        entity.setLejaratiHonap(request.lejaratiHonap());
        entity.setLejaratiEv(request.lejaratiEv());
    }
}
