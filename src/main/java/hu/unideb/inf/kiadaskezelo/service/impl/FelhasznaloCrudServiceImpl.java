package hu.unideb.inf.kiadaskezelo.service.impl;

import hu.unideb.inf.kiadaskezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.kiadaskezelo.data.repository.FelhasznaloRepository;
import hu.unideb.inf.kiadaskezelo.service.exception.FelhasznaloNotFoundException;
import hu.unideb.inf.kiadaskezelo.service.exception.FoglaltFelhasznaloAdatException;
import hu.unideb.inf.kiadaskezelo.service.exception.HibasKeresException;
import hu.unideb.inf.kiadaskezelo.service.FelhasznaloCrudService;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloCreateRequest;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloResponse;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloUpdateRequest;
import hu.unideb.inf.kiadaskezelo.service.mapper.FelhasznaloMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FelhasznaloCrudServiceImpl implements FelhasznaloCrudService {

    private static final Set<String> RENDEZHETO_MEZOK = Set.of(
            "id", "nev", "szuletesiDatum", "felhasznalonev", "email", "nem");

    private final FelhasznaloRepository frepo;
    private final FelhasznaloMapper fmapper;

    @Override
    @Transactional
    public FelhasznaloResponse save(FelhasznaloCreateRequest request) {
        ellenorizEgyediseget(request.email(), request.felhasznalonev(), null);
        FelhasznaloEntity entity = fmapper.toEntity(request);
        // Az adatbázis-kényszer a párhuzamos kérések ellen is véd; a flush itt érvényesíti.
        return fmapper.toResponse(frepo.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public FelhasznaloResponse update(Long id, FelhasznaloUpdateRequest request) {
        FelhasznaloEntity entity = keresIdAlapjan(id);
        ellenorizEgyediseget(request.email(), request.felhasznalonev(), id);
        fmapper.updateEntity(request, entity);
        // Az entitás managed: a JPA dirty checking menti, nincs szükség új save() hívásra.
        frepo.flush();
        return fmapper.toResponse(entity);
    }

    @Override
    public FelhasznaloResponse findById(Long id) {
        return fmapper.toResponse(keresIdAlapjan(id));
    }

    @Override
    public Page<FelhasznaloResponse> findAll(String nev, Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!RENDEZHETO_MEZOK.contains(order.getProperty())) {
                throw new HibasKeresException("Ismeretlen vagy nem rendezhető mező: " + order.getProperty());
            }
        }
        // Azonos értékeknél is determinisztikus lapozás; service-hívásból sem kérhető 100-nál több.
        Sort sort = pageable.getSort();
        if (sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by("id"));
        }
        Pageable korlatozott = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100), sort);
        Page<FelhasznaloEntity> entities = nev == null || nev.isBlank()
                ? frepo.findAll(korlatozott)
                : frepo.findAllByNevContainingIgnoreCase(nev.strip(), korlatozott);
        // A LAZY kapcsolatok beolvasása és a DTO-képzés még a tranzakción belül történik.
        return entities.map(fmapper::toResponse);
    }

    @Override
    @Transactional
    public void deleteAll() {
        frepo.deleteAll();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        frepo.delete(keresIdAlapjan(id));
        frepo.flush();
    }

    private FelhasznaloEntity keresIdAlapjan(Long id) {
        return frepo.findById(id).orElseThrow(() -> new FelhasznaloNotFoundException(id));
    }

    @Override
    @Transactional
    public void aktiv(Long id, boolean aktiv) {
        keresIdAlapjan(id).setAktiv(aktiv);
    }

    private void ellenorizEgyediseget(String email, String felhasznalonev, Long id) {
        boolean emailFoglalt = id == null
                ? frepo.existsByEmail(email) : frepo.existsByEmailAndIdNot(email, id);
        if (emailFoglalt) {
            throw new FoglaltFelhasznaloAdatException("email", "Az e-mail-cím már foglalt.");
        }
        boolean nevFoglalt = id == null
                ? frepo.existsByFelhasznalonev(felhasznalonev)
                : frepo.existsByFelhasznalonevAndIdNot(felhasznalonev, id);
        if (nevFoglalt) {
            throw new FoglaltFelhasznaloAdatException("felhasznalonev", "A felhasználónév már foglalt.");
        }
    }
}
