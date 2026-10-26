package hu.unideb.inf.kiadaskezelo.service.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.MegtakaritasiTervEntity;
import hu.unideb.inf.kiadaskezelo.data.repository.MegtakaritasiTervRepository;
import hu.unideb.inf.kiadaskezelo.service.dto.domain.*;
import hu.unideb.inf.kiadaskezelo.service.exception.ErforrasNotFoundException;
import hu.unideb.inf.kiadaskezelo.service.mapper.PenzugyiMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.math.*;
import java.util.*;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MegtakaritasService {
    private static final MathContext MC = MathContext.DECIMAL128;
    private final MegtakaritasiTervRepository plans;
    private final DomainLookup lookup;
    private final PenzugyiMapper mapper;

    public Page<MegtakaritasResponse> list(Long owner, Pageable page) {
        lookup.user(owner);
        return plans.findAllByFelhasznalo_Id(owner, Lapozas.korlatoz(page, "id", "nev", "penznem", "futamidoHonap")).map(mapper::megtakaritas);
    }
    public MegtakaritasResponse get(Long owner, Long id) { lookup.user(owner); return mapper.megtakaritas(owned(owner, id)); }
    @Transactional
    public MegtakaritasResponse create(Long owner, @NotNull @Valid MegtakaritasRequest request) {
        var entity = new MegtakaritasiTervEntity();
        entity.setFelhasznalo(lookup.activeUser(owner));
        apply(entity, request);
        return mapper.megtakaritas(plans.saveAndFlush(entity));
    }
    @Transactional
    public MegtakaritasResponse update(Long owner, Long id, @NotNull @Valid MegtakaritasRequest request) {
        lookup.activeUser(owner);
        var entity = owned(owner, id);
        apply(entity, request);
        return mapper.megtakaritas(entity);
    }
    @Transactional
    public void delete(Long owner, Long id) { lookup.activeUser(owner); plans.delete(owned(owner, id)); }
    public KalkulacioResponse kalkulal(@NotNull @Valid MegtakaritasRequest request) {
        lookup.currency(request.penznem());
        return calculate(request);
    }
    public KalkulacioResponse kalkulalMentett(Long owner, Long id) {
        lookup.user(owner);
        var p = owned(owner, id);
        return calculate(new MegtakaritasRequest(p.getNev(), p.getPenznem(), p.getKezdoToke(),
                p.getHaviBefizetes(), p.getEvesKamatSzazalek(), p.getFutamidoHonap(), p.getCelosszeg()));
    }
    private KalkulacioResponse calculate(MegtakaritasRequest request) {
        // Nominális éves kamat / 12, havi tőkésítés; befizetés a hónap VÉGÉN.
        BigDecimal multiplier = BigDecimal.ONE.add(request.evesKamatSzazalek().divide(new BigDecimal("1200"), MC), MC);
        BigDecimal balance = request.kezdoToke();
        BigDecimal deposits = request.kezdoToke();
        List<HaviEgyenleg> months = new ArrayList<>();
        for (int month = 1; month <= request.futamidoHonap(); month++) {
            balance = balance.multiply(multiplier, MC).add(request.haviBefizetes(), MC);
            deposits = deposits.add(request.haviBefizetes());
            months.add(new HaviEgyenleg(month, money(deposits), money(balance)));
        }
        return new KalkulacioResponse(request.penznem(), money(deposits), money(balance), money(balance.subtract(deposits)),
                request.celosszeg() == null ? null : money(request.celosszeg().subtract(balance).max(BigDecimal.ZERO)), List.copyOf(months));
    }
    private BigDecimal money(BigDecimal amount) { return amount.setScale(4, RoundingMode.HALF_EVEN); }
    private MegtakaritasiTervEntity owned(Long owner, Long id) {
        return plans.findByIdAndFelhasznalo_Id(id, owner).orElseThrow(() -> new ErforrasNotFoundException("Nem található saját megtakarítási terv: " + id));
    }
    private void apply(MegtakaritasiTervEntity entity, MegtakaritasRequest request) {
        lookup.currency(request.penznem());
        entity.setNev(request.nev().strip());
        entity.setPenznem(request.penznem());
        entity.setKezdoToke(request.kezdoToke());
        entity.setHaviBefizetes(request.haviBefizetes());
        entity.setEvesKamatSzazalek(request.evesKamatSzazalek());
        entity.setFutamidoHonap(request.futamidoHonap());
        entity.setCelosszeg(request.celosszeg());
    }
}
