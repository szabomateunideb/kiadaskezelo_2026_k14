package hu.unideb.inf.kiadaskezelo;

import hu.unideb.inf.kiadaskezelo.bootstrap.DemoAdatFeltolto;
import hu.unideb.inf.kiadaskezelo.data.entity.*;
import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import hu.unideb.inf.kiadaskezelo.data.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "app.demo-data.enabled=true")
@ActiveProfiles("test")
@Transactional
class DomainPersistenceTests {
    @Autowired DemoAdatFeltolto feltolto;
    @Autowired FelhasznaloRepository felhasznalok;
    @Autowired JogRepository jogok;
    @Autowired KategoriaRepository kategoriak;
    @Autowired FelhKategoriaRepository kapcsolatok;
    @Autowired SzamlaRepository szamlak;
    @Autowired KartyaRepository kartyak;
    @Autowired PenzmozgasRepository mozgasok;
    @Autowired UtalasRepository utalasok;
    @Autowired IsmetlodoPenzmozgasRepository ismetlesek;
    @Autowired MegtakaritasiTervRepository tervek;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @Test
    void startupSeedsEveryTableAndRepeatedRunDoesNotDuplicateRows() {
        List<Long> expected = List.of(4L, 2L, 8L, 24L, 9L, 4L, 30L, 9L, 3L, 2L);
        assertThat(counts()).isEqualTo(expected);
        feltolto.run();
        entityManager.flush();
        assertThat(counts()).isEqualTo(expected);
    }

    @Test
    void compositeCategoryKeyMapsBothSidesAndKeepsOptionalCustomName() {
        var anna = anna();
        var elelmiszer = kategoriak.findByNevAndTipus("Élelmiszer", KategoriaTipus.KIADAS).orElseThrow();
        FelhKategoriaId id = new FelhKategoriaId(anna.getId(), elelmiszer.getId());
        var kapcsolat = kapcsolatok.findById(id).orElseThrow();
        kapcsolat.setSajatElnevezes("Bevásárlás");
        entityManager.flush();
        entityManager.clear();
        var reloaded = kapcsolatok.findById(id).orElseThrow();
        assertThat(reloaded.getFelhasznalo().getFelhasznalonev()).isEqualTo("demo_anna");
        assertThat(reloaded.getKategoria().getNev()).isEqualTo("Élelmiszer");
        assertThat(reloaded.getSajatElnevezes()).isEqualTo("Bevásárlás");
        assertThat(reloaded.getLetrehozva()).isNotNull();
        assertThat(reloaded.getModositva()).isAfterOrEqualTo(reloaded.getLetrehozva());
        assertThat(kapcsolatok.findAllByFelhasznalo_IdAndAktivTrue(anna.getId())).hasSize(8);
    }

    @Test
    void twoCardsCanShareAnAccountAndAccountLookupIsScopedToOwner() {
        var anna = anna();
        var bela = felhasznalok.findByFelhasznalonev("demo_bela").orElseThrow();
        var cards = kartyak.findAllBySzamla_Felhasznalo_IdAndAktivTrue(anna.getId());
        assertThat(cards).hasSize(2);
        Long accountId = cards.getFirst().getSzamla().getId();
        assertThat(cards).allSatisfy(card -> {
            assertThat(card.getSzamla().getId()).isEqualTo(accountId);
            assertThat(card.getUtolsoNegySzamjegy()).hasSize(4);
        });
        assertThat(szamlak.findByIdAndFelhasznalo_Id(accountId, anna.getId())).isPresent();
        assertThat(szamlak.findByIdAndFelhasznalo_Id(accountId, bela.getId())).isEmpty();
        assertThat(szamlak.findAllByFelhasznalo_IdAndAktivTrueOrderByNevAsc(anna.getId())).hasSize(3);
    }

    @Test
    void seededMovementsRespectAccountCardCategoryAndTransferRelationships() {
        var movements = mozgasok.findAll();
        assertThat(movements).allSatisfy(movement -> {
            assertThat(movement.getOsszeg()).isPositive();
            assertThat(movement.getLetrehozva()).isNotNull();
            if (movement.getKartya() != null) {
                assertThat(movement.getKartya().getSzamla().getId()).isEqualTo(movement.getSzamla().getId());
            }
            if (movement.getTipus() == PenzmozgasTipus.ATVEZETES) {
                assertThat(movement.getKategoria()).isNull();
                var transfer = utalasok.findByPenzmozgas_Id(movement.getId()).orElseThrow();
                assertThat(transfer.getCelSzamla()).isNotNull();
                assertThat(transfer.getCelSzamla().getId()).isNotEqualTo(movement.getSzamla().getId());
                assertThat(transfer.getCelSzamla().getFelhasznalo().getId())
                        .isEqualTo(movement.getSzamla().getFelhasznalo().getId());
                if (movement.getSzamla().getPenznem().equals(transfer.getCelSzamla().getPenznem())) {
                    assertThat(transfer.getCelOsszeg()).isEqualByComparingTo(movement.getOsszeg());
                }
            } else {
                assertThat(movement.getKategoria().getTipus().name()).isEqualTo(movement.getTipus().name());
                assertThat(kapcsolatok.existsByFelhasznalo_IdAndKategoria_Id(
                        movement.getSzamla().getFelhasznalo().getId(), movement.getKategoria().getId())).isTrue();
            }
        });
        assertThat(movements).filteredOn(m -> m.getAllapot() == PenzmozgasAllapot.TERVEZETT).hasSize(3);
        assertThat(movements).filteredOn(m -> m.getAllapot() == PenzmozgasAllapot.SZTORNOZOTT).hasSize(3);
        assertThat(utalasok.findAll()).filteredOn(u -> u.getCelSzamla() == null).hasSize(3);
    }

    @Test
    void pagedAndTimeBoundedMovementQueriesWork() {
        Long annaId = anna().getId();
        var page = mozgasok.findAllBySzamla_Felhasznalo_Id(annaId,
                PageRequest.of(0, 3, Sort.by("datum").descending().and(Sort.by("id"))));
        assertThat(page.getTotalElements()).isEqualTo(10);
        assertThat(page.getContent()).hasSize(3).allSatisfy(m ->
                assertThat(m.getSzamla().getFelhasznalo().getId()).isEqualTo(annaId));
        var food = mozgasok.findAll().stream().filter(m ->
                m.getSzamla().getFelhasznalo().getId().equals(annaId)
                && "Kártyás bevásárlás".equals(m.getMegnevezes())).findFirst().orElseThrow();
        var included = mozgasok.findInPeriod(food.getSzamla().getId(), PenzmozgasAllapot.KONYVELT,
                food.getDatum(), food.getDatum().plusSeconds(1));
        assertThat(included).extracting(PenzmozgasEntity::getId).contains(food.getId());
        var excludedAtUpperBound = mozgasok.findInPeriod(food.getSzamla().getId(), PenzmozgasAllapot.KONYVELT,
                food.getDatum().minusSeconds(1), food.getDatum());
        assertThat(excludedAtUpperBound).isEmpty();
    }

    @Test
    void recurringSchedulesAndSavedPlansCanBeQueriedByOwner() {
        var ma = LocalDate.now(ZoneId.of("Europe/Budapest"));
        assertThat(ismetlesek.findAllByAktivTrueAndKovetkezoDatumLessThanEqual(ma)).isEmpty();
        assertThat(ismetlesek.findAllByAktivTrueAndKovetkezoDatumLessThanEqual(ma.plusYears(1))).hasSize(3);
        assertThat(ismetlesek.findAllBySzamla_Felhasznalo_Id(anna().getId())).hasSize(1);
        var schedule = ismetlesek.findAllBySzamla_Felhasznalo_Id(anna().getId()).getFirst();
        assertThat(mozgasok.existsByIsmetles_IdAndEsedekesseg(schedule.getId(), schedule.getKezdoDatum())).isTrue();
        assertThat(tervek.findAllByFelhasznalo_Id(anna().getId())).singleElement().satisfies(plan -> {
            assertThat(plan.getNev()).isEqualTo("Vésztartalék");
            assertThat(plan.getEvesKamatSzazalek()).isEqualByComparingTo("4.5");
            assertThat(plan.getFutamidoHonap()).isEqualTo(24);
        });
    }

    @Test
    void databaseRejectsDuplicateCategoryAssignment() {
        var link = kapcsolatok.findAll().getFirst();
        assertThatThrownBy(() -> jdbc.update("""
                insert into FELH_KATEGORIA (felh_id, kat_id, aktiv, letrehozva, modositva)
                select felh_id, kat_id, aktiv, letrehozva, modositva
                from FELH_KATEGORIA where felh_id = ? and kat_id = ?
                """, link.getId().getFelhId(), link.getId().getKatId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsDuplicateOccurrenceOfRecurringMovement() {
        var existing = mozgasok.findAll().stream().filter(m -> m.getIsmetles() != null).findFirst().orElseThrow();
        PenzmozgasEntity duplicate = new PenzmozgasEntity();
        duplicate.setSzamla(existing.getSzamla());
        duplicate.setKategoria(existing.getKategoria());
        duplicate.setIsmetles(existing.getIsmetles());
        duplicate.setEsedekesseg(existing.getEsedekesseg());
        duplicate.setTipus(existing.getTipus());
        duplicate.setOsszeg(existing.getOsszeg());
        duplicate.setDatum(existing.getDatum());
        duplicate.setAllapot(existing.getAllapot());
        duplicate.setMegnevezes("Duplikált ismétlődés");
        assertThatThrownBy(() -> mozgasok.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("UK_PENZMOZGAS_ISMETLES_ESEDEKESSEG");
    }

    @Test
    void databaseEnforcesOneTransferPerMovement() {
        var existing = utalasok.findAll().getFirst();
        UtalasEntity duplicate = new UtalasEntity();
        duplicate.setPenzmozgas(existing.getPenzmozgas());
        duplicate.setCelSzamla(existing.getCelSzamla());
        duplicate.setCelOsszeg(existing.getCelOsszeg());
        assertThatThrownBy(() -> utalasok.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseCheckRejectsNegativeAmountEvenWithoutBeanValidation() {
        Long id = mozgasok.findAll().getFirst().getId();
        assertThatThrownBy(() -> jdbc.update("update PENZMOZGAS set osszeg = -1 where id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("CK_PENZMOZGAS_ERTEKEK");
    }

    @Test
    void internalTransferCannotLoseItsTargetAmount() {
        var transfer = utalasok.findAll().stream().filter(u -> u.getCelSzamla() != null).findFirst().orElseThrow();
        assertThatThrownBy(() -> jdbc.update("update UTALAS set cel_osszeg = null where id = ?", transfer.getId()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("CK_UTALAS_CELOSSZEG");
    }

    @Test
    void foreignKeysProtectFinancialHistoryFromDeletingItsOwner() {
        var owner = anna();
        assertThatThrownBy(() -> {
            felhasznalok.delete(owner);
            felhasznalok.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private FelhasznaloEntity anna() {
        return felhasznalok.findByFelhasznalonev("demo_anna").orElseThrow();
    }

    private List<Long> counts() {
        return List.of(felhasznalok.count(), jogok.count(), kategoriak.count(), kapcsolatok.count(),
                szamlak.count(), kartyak.count(), mozgasok.count(), utalasok.count(), ismetlesek.count(), tervek.count());
    }
}
