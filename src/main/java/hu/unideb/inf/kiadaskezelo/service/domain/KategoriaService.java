package hu.unideb.inf.kiadaskezelo.service.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.*;
import hu.unideb.inf.kiadaskezelo.data.entity.enums.KategoriaTipus;
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
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KategoriaService {
    private final KategoriaRepository categories;
    private final FelhKategoriaRepository assignments;
    private final PenzmozgasRepository movements;
    private final IsmetlodoPenzmozgasRepository schedules;
    private final DomainLookup lookup;
    private final PenzugyiMapper mapper;

    public Page<KategoriaResponse> list(KategoriaTipus type, Pageable page) {
        var paging = Lapozas.korlatoz(page, "id", "nev", "tipus");
        return (type == null ? categories.findAll(paging) : categories.findAllByTipus(type, paging)).map(mapper::kategoria);
    }
    public KategoriaResponse get(Long id) { return mapper.kategoria(lookup.category(id)); }
    @Transactional
    public KategoriaResponse create(@NotNull @Valid KategoriaRequest request) {
        var entity = new KategoriaEntity();
        apply(entity, request);
        return mapper.kategoria(categories.saveAndFlush(entity));
    }
    @Transactional
    public KategoriaResponse update(Long id, @NotNull @Valid KategoriaRequest request) {
        var entity = lookup.category(id);
        if (entity.getTipus() != request.tipus() && used(id)) {
            throw new UzletiSzabalyException("Használt kategória típusa nem változtatható meg.");
        }
        apply(entity, request);
        categories.flush();
        return mapper.kategoria(entity);
    }
    @Transactional
    public void delete(Long id) {
        var entity = lookup.category(id);
        if (used(id) || assignments.existsByKategoria_Id(id)) {
            throw new UzletiSzabalyException("Használt kategória nem törölhető; a saját hozzárendelés kikapcsolható.");
        }
        categories.delete(entity);
        categories.flush();
    }
    public List<FelhKategoriaResponse> assigned(Long owner) {
        lookup.user(owner);
        return assignments.findAllByFelhasznalo_Id(owner).stream().map(mapper::felhKategoria).toList();
    }
    @Transactional
    public FelhKategoriaResponse assign(Long owner, Long categoryId, @NotNull @Valid FelhKategoriaRequest request) {
        var user = lookup.activeUser(owner);
        var category = lookup.category(categoryId);
        if (!request.aktiv() && schedules.existsBySzamla_Felhasznalo_IdAndKategoria_IdAndAktivTrue(owner, categoryId)) {
            throw new UzletiSzabalyException("Az aktív ismétlődés kategóriája nem kapcsolható ki.");
        }
        var id = new FelhKategoriaId(owner, categoryId);
        var entity = assignments.findById(id).orElseGet(() -> new FelhKategoriaEntity(user, category));
        entity.setSajatElnevezes(request.sajatElnevezes());
        entity.setAktiv(request.aktiv());
        return mapper.felhKategoria(assignments.saveAndFlush(entity));
    }
    private boolean used(Long id) { return movements.existsByKategoria_Id(id) || schedules.existsByKategoria_Id(id); }
    private void apply(KategoriaEntity entity, KategoriaRequest request) {
        String name = request.nev().strip();
        boolean duplicate = entity.getId() == null
                ? categories.findByNevAndTipus(name, request.tipus()).isPresent()
                : categories.existsByNevAndTipusAndIdNot(name, request.tipus(), entity.getId());
        if (duplicate) throw new UzletiSzabalyException("Ilyen nevű és típusú kategória már létezik.");
        entity.setNev(name);
        entity.setTipus(request.tipus());
        entity.setLeiras(request.leiras());
    }
}
