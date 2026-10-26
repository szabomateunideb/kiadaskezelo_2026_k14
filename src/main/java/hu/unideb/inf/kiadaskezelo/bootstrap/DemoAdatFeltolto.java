package hu.unideb.inf.kiadaskezelo.bootstrap;

import hu.unideb.inf.kiadaskezelo.data.entity.*;
import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import hu.unideb.inf.kiadaskezelo.data.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true")
public class DemoAdatFeltolto implements CommandLineRunner {
    private static final ZoneId IDOZONA = ZoneId.of("Europe/Budapest");
    // Előre elkészített, bemutató célú bcrypt hash; a feltöltőnek nem kell valódi jelszó.
    private static final String DEMO_JELSZO_HASH =
            "{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";

    private final JogRepository jogRepository;
    private final FelhasznaloRepository felhasznaloRepository;
    private final KategoriaRepository kategoriaRepository;
    private final FelhKategoriaRepository felhKategoriaRepository;
    private final SzamlaRepository szamlaRepository;
    private final KartyaRepository kartyaRepository;
    private final PenzmozgasRepository penzmozgasRepository;
    private final UtalasRepository utalasRepository;
    private final IsmetlodoPenzmozgasRepository ismetlesRepository;
    private final MegtakaritasiTervRepository megtakaritasRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // Egyetlen tranzakció: hibánál nem maradhat félkész mintaadatbázis.
        // Meglévő felhasználói adatbázisba nem keverünk bemutató adatokat.
        if (felhasznaloRepository.count() > 0) {
            log.info("A mintaadat-feltöltés kimarad: az adatbázis már tartalmaz felhasználót.");
            return;
        }

        LocalDate ma = LocalDate.now(IDOZONA);
        JogEntity adminJog = jog("ADMIN");
        JogEntity felhasznaloJog = jog("FELHASZNALO");
        felhasznalo("demo_admin", "Minta Admin", adminJog);
        List<FelhasznaloEntity> felhasznalok = List.of(
                felhasznalo("demo_anna", "Minta Anna", felhasznaloJog),
                felhasznalo("demo_bela", "Minta Béla", felhasznaloJog),
                felhasznalo("demo_csilla", "Minta Csilla", felhasznaloJog));

        Map<String, KategoriaEntity> kategoriak = kategoriak();
        for (int i = 0; i < felhasznalok.size(); i++) {
            FelhasznaloEntity felhasznalo = felhasznalok.get(i);
            kategoriak.values().forEach(kategoria ->
                    felhKategoriaRepository.save(new FelhKategoriaEntity(felhasznalo, kategoria)));

            SzamlaEntity kp = szamla(felhasznalo, "Készpénz", SzamlaTipus.KESZPENZ,
                    "HUF", "20000", null, null, ma);
            SzamlaEntity bank = szamla(felhasznalo, "Forint bankszámla", SzamlaTipus.BANKSZAMLA,
                    "HUF", "150000", "Minta Bank", "HU00%024d".formatted(i * 2 + 1), ma);
            SzamlaEntity euro = szamla(felhasznalo, "Euró bankszámla", SzamlaTipus.BANKSZAMLA,
                    "EUR", "100", "Minta Bank", "HU00%024d".formatted(i * 2 + 2), ma);
            KartyaEntity kartya = kartya(bank, felhasznalo.getNev(), "Fő kártya", "100" + i, ma);
            if (i == 0) {
                kartya(bank, felhasznalo.getNev(), "Virtuális kártya", "9999", ma);
            }

            mozgas(bank, kategoriak.get("munkaber"), null, PenzmozgasTipus.BEVETEL, "300000",
                    "Munkabér", ma.minusDays(14), PenzmozgasAllapot.KONYVELT);
            mozgas(bank, kategoriak.get("elelmiszer"), kartya, PenzmozgasTipus.KIADAS, "12000",
                    "Kártyás bevásárlás", ma.minusDays(7), PenzmozgasAllapot.KONYVELT);
            mozgas(kp, kategoriak.get("elelmiszer"), null, PenzmozgasTipus.KIADAS, "3500",
                    "Piaci bevásárlás", ma.minusDays(5), PenzmozgasAllapot.KONYVELT);
            mozgas(bank, kategoriak.get("szorakozas"), null, PenzmozgasTipus.KIADAS, "8000",
                    "Tervezett mozi", ma.plusDays(3), PenzmozgasAllapot.TERVEZETT);
            mozgas(kp, kategoriak.get("kozlekedes"), null, PenzmozgasTipus.KIADAS, "900",
                    "Sztornózott menetjegy", ma.minusDays(2), PenzmozgasAllapot.SZTORNOZOTT);

            KategoriaEntity ismetlesKategoria = kategoriak.get(i == 0 ? "lakhatas" : i == 1 ? "szorakozas" : "osztondij");
            ismetlodes(bank, ismetlesKategoria, i == 0 ? "Albérlet" : i == 1 ? "Streaming előfizetés" : "Ösztöndíj",
                    i == 0 ? "120000" : i == 1 ? "4500" : "75000", 5 + i * 5, ma);

            PenzmozgasEntity keszpenzfelvetel = mozgas(bank, null, kartya, PenzmozgasTipus.ATVEZETES,
                    "15000", "Készpénzfelvétel", ma.minusDays(6), PenzmozgasAllapot.KONYVELT);
            belsoAtvezetes(keszpenzfelvetel, kp, "15000");
            PenzmozgasEntity devizavaltas = mozgas(bank, null, null, PenzmozgasTipus.ATVEZETES,
                    "40000", "Forint–euró átváltás, mintaárfolyammal", ma.minusDays(4), PenzmozgasAllapot.KONYVELT);
            belsoAtvezetes(devizavaltas, euro, "100");

            PenzmozgasEntity rezsi = mozgas(bank, kategoriak.get("lakhatas"), null, PenzmozgasTipus.KIADAS,
                    "6000", "Külső utalás: közüzemi számla", ma.minusDays(3), PenzmozgasAllapot.KONYVELT);
            UtalasEntity utalas = new UtalasEntity();
            utalas.setPenzmozgas(rezsi);
            utalas.setPartnerNev("Minta Közmű Szolgáltató");
            utalas.setPartnerIban("HU00000000000000000000000999");
            utalas.setKozlemeny("DEMO közüzemi díj");
            utalas.setKulsoAzonosito("DEMO-UTALAS-" + i);
            utalasRepository.save(utalas);
        }

        megtakaritas(felhasznalok.get(0), "Vésztartalék", "HUF", "100000", "30000", "4.5", 24, "1000000");
        megtakaritas(felhasznalok.get(1), "Új laptop", "EUR", "200", "50", "2", 18, "1000");
        penzmozgasRepository.flush();
        log.info("Mintaadatok létrehozva: 4 felhasználó, 8 kategória, 9 számla, 4 kártya, "
                + "30 pénzmozgás, 9 utalás/átvezetés, 3 ismétlődés és 2 megtakarítási terv.");
    }

    private JogEntity jog(String nev) {
        return jogRepository.findByNev(nev).orElseGet(() -> {
            JogEntity entity = new JogEntity();
            entity.setNev(nev);
            entity.setLeiras("Bemutató jogosultság: " + nev);
            return jogRepository.save(entity);
        });
    }

    private FelhasznaloEntity felhasznalo(String felhasznalonev, String nev, JogEntity jog) {
        FelhasznaloEntity entity = new FelhasznaloEntity();
        entity.setFelhasznalonev(felhasznalonev);
        entity.setNev(nev);
        entity.setEmail(felhasznalonev + "@example.test");
        entity.setJelszo(DEMO_JELSZO_HASH);
        entity.setSzuletesiDatum(LocalDate.of(2001, 1, 1));
        entity.setJog(jog);
        entity.setEmailEllenorizve(true);
        return felhasznaloRepository.save(entity);
    }

    private Map<String, KategoriaEntity> kategoriak() {
        Map<String, KategoriaEntity> result = new LinkedHashMap<>();
        result.put("elelmiszer", kategoria("Élelmiszer", KategoriaTipus.KIADAS));
        result.put("lakhatas", kategoria("Lakhatás", KategoriaTipus.KIADAS));
        result.put("kozlekedes", kategoria("Közlekedés", KategoriaTipus.KIADAS));
        result.put("szorakozas", kategoria("Szórakozás", KategoriaTipus.KIADAS));
        result.put("banki_dij", kategoria("Banki díj", KategoriaTipus.KIADAS));
        result.put("munkaber", kategoria("Munkabér", KategoriaTipus.BEVETEL));
        result.put("osztondij", kategoria("Ösztöndíj", KategoriaTipus.BEVETEL));
        result.put("ajandek", kategoria("Ajándék", KategoriaTipus.BEVETEL));
        return result;
    }

    private KategoriaEntity kategoria(String nev, KategoriaTipus tipus) {
        return kategoriaRepository.findByNevAndTipus(nev, tipus).orElseGet(() -> {
            KategoriaEntity entity = new KategoriaEntity();
            entity.setNev(nev);
            entity.setTipus(tipus);
            return kategoriaRepository.save(entity);
        });
    }

    private SzamlaEntity szamla(FelhasznaloEntity tulajdonos, String nev, SzamlaTipus tipus, String penznem,
            String nyitoEgyenleg, String bankNev, String iban, LocalDate ma) {
        SzamlaEntity entity = new SzamlaEntity();
        entity.setFelhasznalo(tulajdonos);
        entity.setNev(nev);
        entity.setTipus(tipus);
        entity.setPenznem(penznem);
        entity.setNyitoEgyenleg(new BigDecimal(nyitoEgyenleg));
        entity.setNyitasDatum(ma.minusYears(1));
        entity.setBankNev(bankNev);
        entity.setIban(iban);
        return szamlaRepository.save(entity);
    }

    private KartyaEntity kartya(SzamlaEntity szamla, String birtokos, String nev, String utolsoNegy, LocalDate ma) {
        KartyaEntity entity = new KartyaEntity();
        entity.setSzamla(szamla);
        entity.setElnevezes(nev);
        entity.setBirtokosNeve(birtokos);
        entity.setKartyaTarsasag(KartyaTarsasag.MASTERCARD);
        entity.setUtolsoNegySzamjegy(utolsoNegy);
        entity.setLejaratiHonap(12);
        entity.setLejaratiEv(ma.getYear() + 3);
        return kartyaRepository.save(entity);
    }

    private PenzmozgasEntity mozgas(SzamlaEntity szamla, KategoriaEntity kategoria, KartyaEntity kartya,
            PenzmozgasTipus tipus, String osszeg, String nev, LocalDate datum, PenzmozgasAllapot allapot) {
        PenzmozgasEntity entity = new PenzmozgasEntity();
        entity.setSzamla(szamla);
        entity.setKategoria(kategoria);
        entity.setKartya(kartya);
        entity.setTipus(tipus);
        entity.setOsszeg(new BigDecimal(osszeg));
        entity.setMegnevezes(nev);
        entity.setDatum(datum.atTime(LocalTime.NOON).atZone(IDOZONA).toInstant());
        entity.setAllapot(allapot);
        return penzmozgasRepository.save(entity);
    }

    private void belsoAtvezetes(PenzmozgasEntity mozgas, SzamlaEntity cel, String celOsszeg) {
        UtalasEntity entity = new UtalasEntity();
        entity.setPenzmozgas(mozgas);
        entity.setCelSzamla(cel);
        entity.setCelOsszeg(new BigDecimal(celOsszeg));
        entity.setKozlemeny(mozgas.getMegnevezes());
        utalasRepository.save(entity);
    }

    private void ismetlodes(SzamlaEntity szamla, KategoriaEntity kategoria, String nev, String osszeg,
            int honapNapja, LocalDate ma) {
        LocalDate kovetkezo = ma.withDayOfMonth(honapNapja);
        if (!kovetkezo.isAfter(ma)) {
            kovetkezo = kovetkezo.plusMonths(1);
        }
        IsmetlodoPenzmozgasEntity entity = new IsmetlodoPenzmozgasEntity();
        entity.setSzamla(szamla);
        entity.setKategoria(kategoria);
        entity.setMegnevezes(nev);
        entity.setTipus(kategoria.getTipus());
        entity.setOsszeg(new BigDecimal(osszeg));
        entity.setGyakorisag(IsmetlodesGyakorisag.HONAP);
        entity.setKezdoDatum(kovetkezo.minusMonths(2));
        entity.setKovetkezoDatum(kovetkezo);
        entity = ismetlesRepository.save(entity);

        for (int honap = 2; honap >= 1; honap--) {
            LocalDate esedekesseg = kovetkezo.minusMonths(honap);
            // Minden kötelező adatot mentés ELŐTT állítunk be: IDENTITY esetén a save azonnal INSERT lehet.
            PenzmozgasEntity tetel = new PenzmozgasEntity();
            tetel.setSzamla(szamla);
            tetel.setKategoria(kategoria);
            tetel.setIsmetles(entity);
            tetel.setEsedekesseg(esedekesseg);
            tetel.setTipus(PenzmozgasTipus.valueOf(kategoria.getTipus().name()));
            tetel.setOsszeg(new BigDecimal(osszeg));
            tetel.setMegnevezes(nev);
            tetel.setDatum(esedekesseg.atTime(LocalTime.NOON).atZone(IDOZONA).toInstant());
            tetel.setAllapot(PenzmozgasAllapot.KONYVELT);
            penzmozgasRepository.save(tetel);
        }
    }

    private void megtakaritas(FelhasznaloEntity tulajdonos, String nev, String penznem, String kezdoToke,
            String haviBefizetes, String kamat, int honap, String cel) {
        MegtakaritasiTervEntity entity = new MegtakaritasiTervEntity();
        entity.setFelhasznalo(tulajdonos);
        entity.setNev(nev);
        entity.setPenznem(penznem);
        entity.setKezdoToke(new BigDecimal(kezdoToke));
        entity.setHaviBefizetes(new BigDecimal(haviBefizetes));
        entity.setEvesKamatSzazalek(new BigDecimal(kamat));
        entity.setFutamidoHonap(honap);
        entity.setCelosszeg(new BigDecimal(cel));
        megtakaritasRepository.save(entity);
    }
}
