package hu.unideb.inf.kiadaskezelo.service.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.*;
import hu.unideb.inf.kiadaskezelo.data.repository.*;
import hu.unideb.inf.kiadaskezelo.service.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DomainLookup {
    private final FelhasznaloRepository users;
    private final SzamlaRepository accounts;
    private final KartyaRepository cards;
    private final KategoriaRepository categories;
    private final FelhKategoriaRepository assignments;
    private final jakarta.persistence.EntityManager entityManager;

    public FelhasznaloEntity user(Long id) {
        return users.findById(id).orElseThrow(() -> new FelhasznaloNotFoundException(id));
    }
    public FelhasznaloEntity activeUser(Long id) {
        var user = user(id);
        if (!user.isAktiv()) throw new UzletiSzabalyException("Az archivált felhasználó adatai nem módosíthatók.");
        return user;
    }
    public SzamlaEntity account(Long owner, Long id) {
        return accounts.findByIdAndFelhasznalo_Id(id, owner)
                .orElseThrow(() -> new ErforrasNotFoundException("Nem található saját számla: " + id));
    }
    public List<SzamlaEntity> lockAccounts(Long owner, Long... ids) {
        activeUser(owner);
        var distinct = Arrays.stream(ids).distinct().sorted().toList();
        var result = accounts.lockOwned(owner, distinct);
        if (result.size() != distinct.size()) throw new ErforrasNotFoundException("Nem található valamelyik saját számla.");
        result.forEach(entityManager::refresh);
        return result;
    }
    public void activeAccount(SzamlaEntity account) {
        if (!account.isAktiv()) throw new UzletiSzabalyException("Az archivált számlán nem könyvelhető pénzmozgás.");
    }
    public KartyaEntity card(Long owner, Long id) {
        return cards.findByIdAndSzamla_Felhasznalo_Id(id, owner)
                .orElseThrow(() -> new ErforrasNotFoundException("Nem található saját kártya: " + id));
    }
    public KategoriaEntity category(Long id) {
        return categories.findById(id).orElseThrow(() -> new ErforrasNotFoundException("Nem található kategória: " + id));
    }
    public KategoriaEntity assignedCategory(Long owner, Long id, String type) {
        if (id == null) throw new HibasKeresException("Kategória megadása kötelező.");
        var category = category(id);
        var link = assignments.findById(new FelhKategoriaId(owner, id)).filter(FelhKategoriaEntity::isAktiv)
                .orElseThrow(() -> new UzletiSzabalyException("A kategória nincs aktívan a felhasználóhoz rendelve."));
        if (!category.getTipus().name().equals(type)) {
            throw new HibasKeresException("A kategória típusa nem illeszkedik a pénzmozgáshoz.");
        }
        return link.getKategoria();
    }
    public void currency(String code) {
        try { Currency.getInstance(code); }
        catch (IllegalArgumentException exception) { throw new HibasKeresException("Ismeretlen pénznem: " + code); }
    }
}
