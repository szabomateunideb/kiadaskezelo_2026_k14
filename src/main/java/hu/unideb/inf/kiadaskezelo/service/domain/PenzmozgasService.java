package hu.unideb.inf.kiadaskezelo.service.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.*;
import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import hu.unideb.inf.kiadaskezelo.data.repository.*;
import hu.unideb.inf.kiadaskezelo.service.dto.domain.*;
import hu.unideb.inf.kiadaskezelo.service.exception.*;
import hu.unideb.inf.kiadaskezelo.service.mapper.PenzugyiMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PenzmozgasService {
    private final PenzmozgasRepository movements;
    private final UtalasRepository transfers;
    private final DomainLookup lookup;
    private final EgyenlegService balances;
    private final PenzugyiMapper mapper;
    private final Clock clock;

    public Page<PenzmozgasResponse> list(Long owner, Long accountId, Long categoryId, PenzmozgasTipus type,
            PenzmozgasAllapot state, Instant from, Instant to, Pageable page) {
        lookup.user(owner);
        if (accountId != null) lookup.account(owner, accountId);
        period(from, to);
        Specification<PenzmozgasEntity> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("szamla").get("felhasznalo").get("id"), owner));
            if (accountId != null) predicates.add(cb.equal(root.get("szamla").get("id"), accountId));
            if (categoryId != null) predicates.add(cb.equal(root.get("kategoria").get("id"), categoryId));
            if (type != null) predicates.add(cb.equal(root.get("tipus"), type));
            if (state != null) predicates.add(cb.equal(root.get("allapot"), state));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("datum"), from));
            if (to != null) predicates.add(cb.lessThan(root.get("datum"), to));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return movements.findAll(spec, Lapozas.korlatoz(page, "id", "datum", "osszeg", "megnevezes", "tipus", "allapot"))
                .map(mapper::penzmozgas);
    }

    public PenzmozgasResponse get(Long owner, Long id) {
        lookup.user(owner);
        return mapper.penzmozgas(owned(owner, id, false));
    }

    @Transactional
    public PenzmozgasResponse create(Long owner, @NotNull @Valid PenzmozgasRequest request) {
        var account = lookup.lockAccounts(owner, request.szamlaId()).getFirst();
        var entity = prepare(owner, account, request);
        checkBooking(entity);
        return mapper.penzmozgas(movements.saveAndFlush(entity));
    }

    @Transactional
    public PenzmozgasResponse update(Long owner, Long id, @NotNull @Valid PenzmozgasRequest request) {
        var entity = owned(owner, id, true);
        if (entity.getAllapot() != PenzmozgasAllapot.TERVEZETT || entity.getIsmetles() != null
                || transfers.findByPenzmozgas_Id(id).isPresent()) {
            throw new UzletiSzabalyException("Csak önálló tervezett tétel módosítható; a többi sztornózható és újrarögzíthető.");
        }
        if (!entity.getSzamla().getId().equals(request.szamlaId()) || request.allapot() != PenzmozgasAllapot.TERVEZETT) {
            throw new HibasKeresException("A számla és az állapot nem írható át; könyveléshez külön művelet tartozik.");
        }
        var account = lookup.lockAccounts(owner, request.szamlaId()).getFirst();
        var values = prepare(owner, account, request);
        entity.setKategoria(values.getKategoria());
        entity.setKartya(values.getKartya());
        entity.setTipus(values.getTipus());
        entity.setOsszeg(values.getOsszeg());
        entity.setDatum(values.getDatum());
        entity.setMegnevezes(values.getMegnevezes());
        entity.setMegjegyzes(values.getMegjegyzes());
        movements.flush();
        return mapper.penzmozgas(entity);
    }

    @Transactional
    public UtalasResponse atvezetes(Long owner, @NotNull @Valid AtvezetesRequest request) {
        if (request.forrasSzamlaId().equals(request.celSzamlaId())) {
            throw new HibasKeresException("Az átvezetés két különböző saját számla között történhet.");
        }
        var accounts = lookup.lockAccounts(owner, request.forrasSzamlaId(), request.celSzamlaId());
        var source = accounts.stream().filter(s -> s.getId().equals(request.forrasSzamlaId())).findFirst().orElseThrow();
        var target = accounts.stream().filter(s -> s.getId().equals(request.celSzamlaId())).findFirst().orElseThrow();
        initialState(request.allapot());
        accountAndTime(source, request.datum(), request.allapot());
        accountAndTime(target, request.datum(), request.allapot());
        if (source.getPenznem().equals(target.getPenznem()) && request.osszeg().compareTo(request.celOsszeg()) != 0) {
            throw new HibasKeresException("Azonos pénznem esetén a forrás- és célösszegnek egyeznie kell.");
        }
        var entity = new PenzmozgasEntity();
        entity.setSzamla(source);
        entity.setTipus(PenzmozgasTipus.ATVEZETES);
        entity.setOsszeg(request.osszeg());
        entity.setDatum(request.datum());
        entity.setAllapot(request.allapot());
        entity.setMegnevezes(request.megnevezes().strip());
        var transfer = new UtalasEntity();
        transfer.setPenzmozgas(entity);
        transfer.setCelSzamla(target);
        transfer.setCelOsszeg(request.celOsszeg());
        transfer.setKozlemeny(request.kozlemeny());
        checkBooking(entity);
        movements.saveAndFlush(entity);
        return mapper.utalas(transfers.saveAndFlush(transfer));
    }

    @Transactional
    public UtalasResponse kulsoUtalas(Long owner, @NotNull @Valid KulsoUtalasRequest request) {
        var account = lookup.lockAccounts(owner, request.penzmozgas().szamlaId()).getFirst();
        if (account.getTipus() != SzamlaTipus.BANKSZAMLA) {
            throw new HibasKeresException("Külső banki utalás csak bankszámlához rögzíthető.");
        }
        if (request.penzmozgas().kartyaId() != null) throw new HibasKeresException("A banki utalás nem kártyás vásárlás.");
        var entity = prepare(owner, account, request.penzmozgas());
        checkBooking(entity);
        movements.saveAndFlush(entity);
        var transfer = new UtalasEntity();
        transfer.setPenzmozgas(entity);
        transfer.setPartnerNev(request.partnerNev().strip());
        transfer.setPartnerIban(request.partnerIban().replace(" ", "").toUpperCase(Locale.ROOT));
        transfer.setKozlemeny(request.kozlemeny());
        transfer.setKulsoAzonosito(request.kulsoAzonosito());
        return mapper.utalas(transfers.saveAndFlush(transfer));
    }

    public Page<UtalasResponse> utalasok(Long owner, Pageable page) {
        lookup.user(owner);
        Specification<UtalasEntity> spec = (root, query, cb) ->
                cb.equal(root.get("penzmozgas").get("szamla").get("felhasznalo").get("id"), owner);
        return transfers.findAll(spec, Lapozas.korlatoz(page, "id", "letrehozva")).map(mapper::utalas);
    }

    public UtalasResponse utalas(Long owner, Long id) {
        lookup.user(owner);
        var entity = transfers.findById(id).filter(u -> u.getPenzmozgas().getSzamla().getFelhasznalo().getId().equals(owner))
                .orElseThrow(() -> new ErforrasNotFoundException("Nem található saját utalás: " + id));
        return mapper.utalas(entity);
    }

    @Transactional
    public PenzmozgasResponse konyvel(Long owner, Long id) {
        var entity = owned(owner, id, true);
        lookup.activeUser(owner);
        if (entity.getAllapot() == PenzmozgasAllapot.KONYVELT) return mapper.penzmozgas(entity);
        if (entity.getAllapot() != PenzmozgasAllapot.TERVEZETT) throw new UzletiSzabalyException("Sztornózott tétel nem könyvelhető.");
        var transfer = transfers.findByPenzmozgas_Id(id).orElse(null);
        lockMovementAccounts(owner, entity, transfer);
        accountAndTime(entity.getSzamla(), entity.getDatum(), PenzmozgasAllapot.KONYVELT);
        if (transfer != null && transfer.getCelSzamla() != null) {
            accountAndTime(transfer.getCelSzamla(), entity.getDatum(), PenzmozgasAllapot.KONYVELT);
        }
        if (entity.getTipus() != PenzmozgasTipus.ATVEZETES) {
            lookup.assignedCategory(owner, entity.getKategoria().getId(), entity.getTipus().name());
        }
        cardCheck(entity);
        // Az állapotot csak az egyenlegvizsgálat UTÁN módosítjuk, így a tétel még nem számít bele.
        if (entity.getTipus() != PenzmozgasTipus.BEVETEL) funds(entity.getSzamla(), entity.getOsszeg());
        entity.setAllapot(PenzmozgasAllapot.KONYVELT);
        movements.flush();
        return mapper.penzmozgas(entity);
    }

    @Transactional
    public PenzmozgasResponse sztorno(Long owner, Long id) {
        var entity = owned(owner, id, true);
        lookup.activeUser(owner);
        if (entity.getAllapot() == PenzmozgasAllapot.SZTORNOZOTT) return mapper.penzmozgas(entity);
        var transfer = transfers.findByPenzmozgas_Id(id).orElse(null);
        lockMovementAccounts(owner, entity, transfer);
        if (entity.getAllapot() == PenzmozgasAllapot.KONYVELT) {
            lookup.activeAccount(entity.getSzamla());
            if (entity.getTipus() == PenzmozgasTipus.BEVETEL) funds(entity.getSzamla(), entity.getOsszeg());
            if (transfer != null && transfer.getCelSzamla() != null) {
                lookup.activeAccount(transfer.getCelSzamla());
                funds(transfer.getCelSzamla(), transfer.getCelOsszeg());
            }
        }
        entity.setAllapot(PenzmozgasAllapot.SZTORNOZOTT);
        movements.flush();
        return mapper.penzmozgas(entity);
    }

    @Transactional
    public PenzmozgasResponse kategorizal(Long owner, Long id, Long categoryId) {
        lookup.activeUser(owner);
        var entity = owned(owner, id, true);
        if (entity.getTipus() == PenzmozgasTipus.ATVEZETES || entity.getAllapot() == PenzmozgasAllapot.SZTORNOZOTT) {
            throw new UzletiSzabalyException("Átvezetés vagy sztornózott tétel nem kategorizálható.");
        }
        entity.setKategoria(lookup.assignedCategory(owner, categoryId, entity.getTipus().name()));
        return mapper.penzmozgas(entity);
    }

    public List<KategoriaStatisztikaResponse> statisztika(Long owner, LocalDate tol, LocalDate ig) {
        lookup.user(owner);
        if (tol.isAfter(ig)) throw new HibasKeresException("A kezdő dátum nem lehet a záró dátum után.");
        return movements.statisztika(owner, tol.atStartOfDay(clock.getZone()).toInstant(),
                ig.plusDays(1).atStartOfDay(clock.getZone()).toInstant(),
                PenzmozgasAllapot.KONYVELT, PenzmozgasTipus.ATVEZETES).stream().map(row ->
                new KategoriaStatisztikaResponse(row.getKategoriaId(), row.getKategoriaNev(), row.getTipus(),
                        row.getPenznem(), row.getOsszeg(), row.getTetelszam())).toList();
    }

    private PenzmozgasEntity prepare(Long owner, SzamlaEntity account, PenzmozgasRequest request) {
        initialState(request.allapot());
        if (request.tipus() == PenzmozgasTipus.ATVEZETES) throw new HibasKeresException("Átvezetéshez az átvezetési végpont használható.");
        accountAndTime(account, request.datum(), request.allapot());
        var entity = new PenzmozgasEntity();
        entity.setSzamla(account);
        entity.setKategoria(lookup.assignedCategory(owner, request.kategoriaId(), request.tipus().name()));
        entity.setKartya(request.kartyaId() == null ? null : lookup.card(owner, request.kartyaId()));
        entity.setTipus(request.tipus());
        entity.setOsszeg(request.osszeg());
        entity.setDatum(request.datum());
        entity.setAllapot(request.allapot());
        entity.setMegnevezes(request.megnevezes().strip());
        entity.setMegjegyzes(request.megjegyzes());
        cardCheck(entity);
        return entity;
    }
    private void checkBooking(PenzmozgasEntity entity) {
        if (entity.getAllapot() == PenzmozgasAllapot.KONYVELT && entity.getTipus() != PenzmozgasTipus.BEVETEL) {
            funds(entity.getSzamla(), entity.getOsszeg());
        }
    }
    private void funds(SzamlaEntity account, BigDecimal amount) {
        if (balances.egyenleg(account).compareTo(amount) < 0) throw new UzletiSzabalyException("Nincs elegendő egyenleg a számlán: " + account.getId());
    }
    private void accountAndTime(SzamlaEntity account, Instant date, PenzmozgasAllapot state) {
        lookup.activeAccount(account);
        if (date.atZone(clock.getZone()).toLocalDate().isBefore(account.getNyitasDatum())) {
            throw new HibasKeresException("A pénzmozgás nem előzheti meg a számlanyitást.");
        }
        if (state == PenzmozgasAllapot.KONYVELT && date.isAfter(clock.instant())) {
            throw new HibasKeresException("Jövőbeli tétel csak tervezett állapotban rögzíthető.");
        }
    }
    private void cardCheck(PenzmozgasEntity entity) {
        var card = entity.getKartya();
        if (card == null) return;
        if (!card.getSzamla().getId().equals(entity.getSzamla().getId()) || entity.getTipus() != PenzmozgasTipus.KIADAS) {
            throw new HibasKeresException("A kártyának az érintett számlához kell tartoznia, és csak kiadáshoz használható.");
        }
        if (!card.isAktiv() || YearMonth.of(card.getLejaratiEv(), card.getLejaratiHonap())
                .isBefore(YearMonth.from(entity.getDatum().atZone(clock.getZone())))) {
            throw new UzletiSzabalyException("A kártya inaktív vagy a megadott időpontban lejárt.");
        }
    }
    private void initialState(PenzmozgasAllapot state) {
        if (state == PenzmozgasAllapot.SZTORNOZOTT) throw new HibasKeresException("Új tétel nem lehet sztornózott.");
    }
    private PenzmozgasEntity owned(Long owner, Long id, boolean lock) {
        var result = lock ? movements.lockById(id) : movements.findById(id);
        return result.filter(p -> p.getSzamla().getFelhasznalo().getId().equals(owner))
                .orElseThrow(() -> new ErforrasNotFoundException("Nem található saját pénzmozgás: " + id));
    }
    private void lockMovementAccounts(Long owner, PenzmozgasEntity entity, UtalasEntity transfer) {
        if (transfer != null && transfer.getCelSzamla() != null) {
            lookup.lockAccounts(owner, entity.getSzamla().getId(), transfer.getCelSzamla().getId());
        } else lookup.lockAccounts(owner, entity.getSzamla().getId());
    }
    private void period(Instant from, Instant to) {
        if (from != null && to != null && !from.isBefore(to)) throw new HibasKeresException("Az időszak vége legyen a kezdete után.");
    }
}
