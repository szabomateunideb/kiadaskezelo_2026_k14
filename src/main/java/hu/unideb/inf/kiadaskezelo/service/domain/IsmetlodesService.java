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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.time.*;
import java.util.*;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IsmetlodesService {
    private final IsmetlodoPenzmozgasRepository schedules;
    private final PenzmozgasRepository movements;
    private final DomainLookup lookup;
    private final PenzugyiMapper mapper;
    private final Clock clock;

    public Page<IsmetlesResponse> list(Long owner, Pageable page) {
        lookup.user(owner);
        return schedules.findAllBySzamla_Felhasznalo_Id(owner, Lapozas.korlatoz(page, "id", "megnevezes", "kovetkezoDatum"))
                .map(mapper::ismetles);
    }
    public IsmetlesResponse get(Long owner, Long id) {
        lookup.user(owner);
        return mapper.ismetles(owned(owner, id, false));
    }
    @Transactional
    public IsmetlesResponse create(Long owner, @NotNull @Valid IsmetlesRequest request) {
        var account = lookup.lockAccounts(owner, request.szamlaId()).getFirst();
        validate(request, account);
        var category = lookup.category(request.kategoriaId());
        category = lookup.assignedCategory(owner, request.kategoriaId(), category.getTipus().name());
        var entity = new IsmetlodoPenzmozgasEntity();
        entity.setSzamla(account);
        entity.setKovetkezoDatum(request.kezdoDatum());
        apply(entity, request, category);
        return mapper.ismetles(schedules.saveAndFlush(entity));
    }
    @Transactional
    public IsmetlesResponse update(Long owner, Long id, @NotNull @Valid IsmetlesRequest request) {
        var entity = owned(owner, id, true);
        if (!entity.getSzamla().getId().equals(request.szamlaId())) throw new HibasKeresException("Az ismétlődés számlája nem írható át.");
        var account = lookup.lockAccounts(owner, request.szamlaId()).getFirst();
        validate(request, account);
        boolean used = movements.existsByIsmetles_Id(id);
        if (used && (!entity.getKezdoDatum().equals(request.kezdoDatum())
                || entity.getGyakorisag() != request.gyakorisag() || entity.getIntervallum() != request.intervallum())) {
            throw new UzletiSzabalyException("Használt ütemezés kezdete és ritmusa nem változtatható meg; új sorozat indítható.");
        }
        var category = lookup.category(request.kategoriaId());
        category = lookup.assignedCategory(owner, category.getId(), category.getTipus().name());
        if (!used) entity.setKovetkezoDatum(request.kezdoDatum());
        apply(entity, request, category);
        if (entity.getVegdatum() != null && entity.getKovetkezoDatum().isAfter(entity.getVegdatum())) entity.setAktiv(false);
        return mapper.ismetles(entity);
    }
    @Transactional
    public void aktiv(Long owner, Long id, boolean active) {
        var entity = owned(owner, id, true);
        lookup.activeUser(owner);
        if (active) {
            var account = lookup.lockAccounts(owner, entity.getSzamla().getId()).getFirst();
            lookup.activeAccount(account);
            lookup.assignedCategory(owner, entity.getKategoria().getId(), entity.getTipus().name());
            if (entity.getVegdatum() != null && entity.getKovetkezoDatum().isAfter(entity.getVegdatum())) {
                throw new UzletiSzabalyException("A sorozat véget ért; új sorozat indítható.");
            }
        }
        entity.setAktiv(active);
    }
    @Transactional
    public List<PenzmozgasResponse> feldolgoz(Long owner, Long id) {
        var entity = owned(owner, id, true);
        lookup.activeUser(owner);
        List<PenzmozgasResponse> result = new ArrayList<>();
        LocalDate today = LocalDate.now(clock);
        if (!entity.isAktiv() || entity.getKovetkezoDatum().isAfter(today)) return result;
        var account = lookup.lockAccounts(owner, entity.getSzamla().getId()).getFirst();
        lookup.activeAccount(account);
        lookup.assignedCategory(owner, entity.getKategoria().getId(), entity.getTipus().name());
        int visited = 0;
        while (entity.isAktiv() && !entity.getKovetkezoDatum().isAfter(today) && visited++ < 120) {
            LocalDate due = entity.getKovetkezoDatum();
            if (entity.getVegdatum() != null && due.isAfter(entity.getVegdatum())) {
                entity.setAktiv(false);
                break;
            }
            if (!movements.existsByIsmetles_IdAndEsedekesseg(id, due)) {
                var movement = new PenzmozgasEntity();
                movement.setSzamla(account);
                movement.setKategoria(entity.getKategoria());
                movement.setIsmetles(entity);
                movement.setEsedekesseg(due);
                movement.setDatum(due.atStartOfDay(clock.getZone()).toInstant());
                movement.setMegnevezes(entity.getMegnevezes());
                movement.setOsszeg(entity.getOsszeg());
                movement.setTipus(PenzmozgasTipus.valueOf(entity.getTipus().name()));
                movement.setAllapot(PenzmozgasAllapot.TERVEZETT);
                result.add(mapper.penzmozgas(movements.save(movement)));
            }
            entity.setKovetkezoDatum(IsmetlodesUtemezes.kovetkezo(entity.getKezdoDatum(), due,
                    entity.getGyakorisag(), entity.getIntervallum()));
            if (entity.getVegdatum() != null && entity.getKovetkezoDatum().isAfter(entity.getVegdatum())) entity.setAktiv(false);
        }
        movements.flush();
        return result;
    }
    public List<IsmetlesResponse> esedekesek() {
        return schedules.feldolgozhatoEsedekesek(LocalDate.now(clock), PageRequest.of(0,100)).stream()
                .map(mapper::ismetles).toList();
    }
    public Long tulajdonos(Long id) {
        return schedules.findById(id).orElseThrow(() -> new ErforrasNotFoundException("Nincs ilyen ismétlődés."))
                .getSzamla().getFelhasznalo().getId();
    }
    private IsmetlodoPenzmozgasEntity owned(Long owner, Long id, boolean lock) {
        var result = lock ? schedules.lockById(id) : schedules.findById(id);
        return result.filter(i -> i.getSzamla().getFelhasznalo().getId().equals(owner))
                .orElseThrow(() -> new ErforrasNotFoundException("Nem található saját ismétlődés: " + id));
    }
    private void validate(IsmetlesRequest request, SzamlaEntity account) {
        lookup.activeAccount(account);
        if (request.kezdoDatum().isBefore(account.getNyitasDatum())) throw new HibasKeresException("Az ismétlődés nem előzheti meg a számlanyitást.");
        if (request.vegdatum() != null && request.vegdatum().isBefore(request.kezdoDatum())) throw new HibasKeresException("A sorozat vége nem előzheti meg a kezdetét.");
        if (request.kezdoDatum().isAfter(LocalDate.now(clock).plusYears(100))) throw new HibasKeresException("A kezdőnap legfeljebb 100 évvel lehet későbbi.");
    }
    private void apply(IsmetlodoPenzmozgasEntity entity, IsmetlesRequest request, KategoriaEntity category) {
        entity.setKategoria(category);
        entity.setTipus(category.getTipus());
        entity.setMegnevezes(request.megnevezes().strip());
        entity.setOsszeg(request.osszeg());
        entity.setGyakorisag(request.gyakorisag());
        entity.setIntervallum(request.intervallum());
        entity.setKezdoDatum(request.kezdoDatum());
        entity.setVegdatum(request.vegdatum());
    }
}
