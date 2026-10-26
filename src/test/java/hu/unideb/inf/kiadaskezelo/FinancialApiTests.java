package hu.unideb.inf.kiadaskezelo;

import hu.unideb.inf.kiadaskezelo.data.entity.*;
import hu.unideb.inf.kiadaskezelo.data.entity.enums.*;
import hu.unideb.inf.kiadaskezelo.data.repository.*;
import hu.unideb.inf.kiadaskezelo.service.domain.*;
import hu.unideb.inf.kiadaskezelo.service.dto.domain.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FinancialApiTests {
    private static final Instant NOW = Instant.parse("2026-10-26T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026,10,26);
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired FelhasznaloRepository users;
    @Autowired SzamlaService accounts;
    @Autowired KategoriaService categories;
    @Autowired KartyaService cards;
    @Autowired PenzmozgasRepository movements;
    @Autowired UtalasRepository transfers;
    @Autowired IsmetlodesService recurring;
    @MockitoBean Clock clock;
    private Long owner, other, bank, cash, euro, otherBank, expense, housing, income, card;
    private String base;

    @BeforeEach
    void fixture() {
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneId.of("Europe/Budapest"));
        for (String table : List.of("UTALAS","PENZMOZGAS","ISMETLODO_PENZMOZGAS","KARTYA","MEGTAKARITASI_TERV",
                "FELH_KATEGORIA","SZAMLA","FELHASZNALO","KATEGORIA","JOGOSULTSAG")) jdbc.update("delete from " + table);
        owner = user("anna"); other = user("bela");
        expense = categories.create(new KategoriaRequest("Élelmiszer",KategoriaTipus.KIADAS,null)).id();
        housing = categories.create(new KategoriaRequest("Lakhatás",KategoriaTipus.KIADAS,null)).id();
        income = categories.create(new KategoriaRequest("Munkabér",KategoriaTipus.BEVETEL,null)).id();
        for (Long user : List.of(owner,other)) for (Long category : List.of(expense,housing,income)) {
            categories.assign(user,category,new FelhKategoriaRequest(null,true));
        }
        bank = account(owner,"Bank",SzamlaTipus.BANKSZAMLA,"HUF","1000");
        cash = account(owner,"Készpénz",SzamlaTipus.KESZPENZ,"HUF","0");
        euro = account(owner,"Euró",SzamlaTipus.BANKSZAMLA,"EUR","100");
        otherBank = account(other,"Másik bank",SzamlaTipus.BANKSZAMLA,"HUF","1000");
        card = cards.create(owner,cardRequest(bank,"Fő kártya",2030)).id();
        base = "/api/felhasznalok/" + owner;
    }

    @Test void incomeExpenseAndPlannedBookingChangeOnlyBookedBalances() throws Exception {
        created("/penzmozgasok",money(bank,income,PenzmozgasTipus.BEVETEL,"500",PenzmozgasAllapot.KONYVELT));
        created("/penzmozgasok",money(bank,expense,PenzmozgasTipus.KIADAS,"200",PenzmozgasAllapot.KONYVELT));
        Long planned = created("/penzmozgasok",money(bank,expense,PenzmozgasTipus.KIADAS,"100",PenzmozgasAllapot.TERVEZETT));
        balance(bank,"1300");
        perform(post(base+"/penzmozgasok/"+planned+"/konyveles"),200);
        perform(post(base+"/penzmozgasok/"+planned+"/konyveles"),200);
        balance(bank,"1200");
        perform(post(base+"/penzmozgasok/"+planned+"/sztorno"),200);
        perform(post(base+"/penzmozgasok/"+planned+"/sztorno"),200);
        balance(bank,"1300");
        perform(post(base+"/penzmozgasok/"+planned+"/konyveles"),409);
    }
    @Test void insufficientFundsRollBackCreationAndBooking() throws Exception {
        send(post(base+"/penzmozgasok"),money(bank,expense,PenzmozgasTipus.KIADAS,"1001",PenzmozgasAllapot.KONYVELT),409);
        assertThat(movements.count()).isZero(); balance(bank,"1000");
        Long id = created("/penzmozgasok",money(bank,expense,PenzmozgasTipus.KIADAS,"1001",PenzmozgasAllapot.TERVEZETT));
        perform(post(base+"/penzmozgasok/"+id+"/konyveles"),409);
        perform(get(base+"/penzmozgasok/"+id),200).andExpect(jsonPath("$.allapot").value("TERVEZETT"));
    }
    @Test void internalTransferAndReversalPreserveTotalBalance() throws Exception {
        var result = send(post(base+"/atvezetesek"),transfer(bank,cash,"250","250"),201)
                .andExpect(header().exists("Location")).andReturn();
        var value = json.readTree(result.getResponse().getContentAsString());
        Long id = value.get("penzmozgas").get("id").asLong();
        perform(get(result.getResponse().getHeader("Location")),200);
        balance(bank,"750"); balance(cash,"250");
        perform(post(base+"/penzmozgasok/"+id+"/sztorno"),200);
        balance(bank,"1000"); balance(cash,"0");
    }
    @Test void transferReversalCannotSpendAlreadyUsedDestinationFunds() throws Exception {
        var value = sendJson(post(base+"/atvezetesek"),transfer(bank,cash,"250","250"),201);
        created("/penzmozgasok",money(cash,expense,PenzmozgasTipus.KIADAS,"200",PenzmozgasAllapot.KONYVELT));
        perform(post(base+"/penzmozgasok/"+value.get("penzmozgas").get("id").asLong()+"/sztorno"),409);
        balance(bank,"750"); balance(cash,"50");
    }
    @Test void currencyTransferUsesExplicitTargetAmountAndValidatesBothAccounts() throws Exception {
        send(post(base+"/atvezetesek"),transfer(bank,euro,"400","1"),201);
        balance(bank,"600"); balance(euro,"101");
        send(post(base+"/atvezetesek"),transfer(bank,cash,"1","2"),400);
        send(post(base+"/atvezetesek"),transfer(bank,bank,"1","1"),400);
        send(post(base+"/atvezetesek"),transfer(bank,otherBank,"1","1"),404);
        send(post(base+"/atvezetesek"),transfer(bank,cash,"601","601"),409);
        assertThat(transfers.count()).isEqualTo(1);
    }
    @Test void externalTransferRecordsPartnerAndIsARegularCategorizedExpense() throws Exception {
        var request = new KulsoUtalasRequest(money(bank,housing,PenzmozgasTipus.KIADAS,"100",PenzmozgasAllapot.KONYVELT),
                "Minta szolgáltató","HU00000000000000000000000000","Számla","demo-1");
        var id = created("/utalasok",request);
        perform(get(base+"/utalasok/"+id),200).andExpect(jsonPath("$.partnerNev").value("Minta szolgáltató"));
        balance(bank,"900");
        send(post(base+"/utalasok"),new KulsoUtalasRequest(money(cash,housing,PenzmozgasTipus.KIADAS,"1",PenzmozgasAllapot.TERVEZETT),
                "Partner","HU00",null,null),400);
    }
    @Test void ownershipAppliesToEveryResourceRoute() throws Exception {
        Long movement = created("/penzmozgasok",money(bank,expense,PenzmozgasTipus.KIADAS,"1",PenzmozgasAllapot.TERVEZETT));
        String foreign = "/api/felhasznalok/"+other;
        perform(get(foreign+"/penzmozgasok/"+movement),404);
        perform(post(foreign+"/penzmozgasok/"+movement+"/konyveles"),404);
        perform(get(foreign+"/szamlak/"+bank),404);
        perform(get(foreign+"/kartyak/"+card),404);
        send(post(base+"/penzmozgasok"),money(otherBank,expense,PenzmozgasTipus.KIADAS,"1",PenzmozgasAllapot.KONYVELT),404);
    }
    @Test void accountMetadataCanChangeButCurrencyAndOpeningBalanceCannotBeOverwritten() throws Exception {
        send(put(base+"/szamlak/"+bank),new SzamlaUpdateRequest("Új név","Bank",null),200);
        perform(get(base+"/szamlak/"+bank),200).andExpect(jsonPath("$.nev").value("Új név"));
        mvc.perform(put(base+"/szamlak/"+bank).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nev\":\"Új név\",\"nyitoEgyenleg\":9999}" )).andExpect(status().isBadRequest());
        balance(bank,"1000");
        send(post(base+"/szamlak"),new SzamlaCreateRequest("Hibás",SzamlaTipus.KESZPENZ,"HUF",BigDecimal.ZERO,
                TODAY.minusDays(1),"Bank",null),400);
        send(post(base+"/szamlak"),new SzamlaCreateRequest("Hibás",SzamlaTipus.BANKSZAMLA,"ZZZ",BigDecimal.ZERO,
                TODAY.minusDays(1),null,null),400);
    }
    @Test void accountArchiveRequiresZeroAndBlocksPostingUntilReactivated() throws Exception {
        perform(delete(base+"/szamlak/"+bank),409);
        perform(delete(base+"/szamlak/"+cash),204);
        send(post(base+"/penzmozgasok"),money(cash,income,PenzmozgasTipus.BEVETEL,"1",PenzmozgasAllapot.KONYVELT),409);
        send(patch(base+"/szamlak/"+cash+"/aktiv"),new AktivRequest(true),204);
        created("/penzmozgasok",money(cash,income,PenzmozgasTipus.BEVETEL,"1",PenzmozgasAllapot.KONYVELT));
    }
    @Test void cardValidationAndArchiveProtectPayments() throws Exception {
        send(post(base+"/kartyak"),cardRequest(cash,"KP kártya",2030),400);
        send(post(base+"/kartyak"),cardRequest(bank,"Lejárt",2020),400);
        send(put(base+"/kartyak/"+card),cardRequest(bank,"Átnevezett",2031),200);
        send(put(base+"/kartyak/"+card),cardRequest(euro,"Áthelyezés",2031),400);
        perform(delete(base+"/kartyak/"+card),204);
        var payment = new PenzmozgasRequest(bank,expense,card,PenzmozgasTipus.KIADAS,BigDecimal.ONE,NOW.minusSeconds(60),
                PenzmozgasAllapot.KONYVELT,"Kártyás",null);
        send(post(base+"/penzmozgasok"),payment,409);
        send(patch(base+"/kartyak/"+card+"/aktiv"),new AktivRequest(true),204);
        send(post(base+"/penzmozgasok"),payment,201);
    }
    @Test void categoryAssignmentTypeAndUsageAreChecked() throws Exception {
        var unassigned = categories.create(new KategoriaRequest("Új",KategoriaTipus.KIADAS,null)).id();
        send(post(base+"/penzmozgasok"),money(bank,unassigned,PenzmozgasTipus.KIADAS,"1",PenzmozgasAllapot.TERVEZETT),409);
        send(put(base+"/kategoriak/"+unassigned),new FelhKategoriaRequest("Saját",true),200);
        created("/penzmozgasok",money(bank,unassigned,PenzmozgasTipus.KIADAS,"1",PenzmozgasAllapot.TERVEZETT));
        send(put("/api/kategoriak/"+unassigned),new KategoriaRequest("Új",KategoriaTipus.BEVETEL,null),409);
        perform(delete("/api/kategoriak/"+unassigned),409);
        send(post(base+"/penzmozgasok"),money(bank,income,PenzmozgasTipus.KIADAS,"1",PenzmozgasAllapot.KONYVELT),400);
    }
    @Test void bookedEntriesAreImmutableButCanBeRecategorized() throws Exception {
        Long id = created("/penzmozgasok",money(bank,expense,PenzmozgasTipus.KIADAS,"10",PenzmozgasAllapot.KONYVELT));
        send(put(base+"/penzmozgasok/"+id),money(bank,expense,PenzmozgasTipus.KIADAS,"20",PenzmozgasAllapot.TERVEZETT),409);
        send(patch(base+"/penzmozgasok/"+id+"/kategoria"),new KategorizalasRequest(housing),200)
                .andExpect(jsonPath("$.kategoriaId").value(housing));
        send(patch(base+"/penzmozgasok/"+id+"/kategoria"),new KategorizalasRequest(income),400);
        balance(bank,"990");
    }
    @Test void plannedEntriesCanBeEditedWithoutChangingTheirIdentity() throws Exception {
        Long id = created("/penzmozgasok",money(bank,expense,PenzmozgasTipus.KIADAS,"10",PenzmozgasAllapot.TERVEZETT));
        send(put(base+"/penzmozgasok/"+id),money(bank,housing,PenzmozgasTipus.KIADAS,"20",PenzmozgasAllapot.TERVEZETT),200)
                .andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.osszeg").value(20));
        balance(bank,"1000");
    }
    @Test void moneyInputAndDateErrorsReturnProblemDetails() throws Exception {
        send(post(base+"/penzmozgasok"),money(bank,expense,PenzmozgasTipus.KIADAS,"-1",PenzmozgasAllapot.KONYVELT),400)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        send(post(base+"/penzmozgasok"),new PenzmozgasRequest(bank,expense,null,PenzmozgasTipus.KIADAS,BigDecimal.ONE,
                NOW.plusSeconds(86400),PenzmozgasAllapot.KONYVELT,"Jövő",null),400);
        send(post(base+"/penzmozgasok"),new PenzmozgasRequest(bank,expense,null,PenzmozgasTipus.KIADAS,BigDecimal.ONE,
                Instant.parse("2010-01-01T00:00:00Z"),PenzmozgasAllapot.TERVEZETT,"Túl korai",null),400);
        send(post(base+"/penzmozgasok"),money(bank,expense,PenzmozgasTipus.KIADAS,"1",PenzmozgasAllapot.SZTORNOZOTT),400);
        send(post(base+"/penzmozgasok"),money(bank,null,PenzmozgasTipus.KIADAS,"1",PenzmozgasAllapot.TERVEZETT),400);
    }
    @Test void listingFiltersPagesAndRejectsUnknownSorts() throws Exception {
        for (int i=0;i<3;i++) created("/penzmozgasok",money(bank,expense,PenzmozgasTipus.KIADAS,"10",PenzmozgasAllapot.TERVEZETT));
        perform(get(base+"/penzmozgasok").param("szamlaId",bank.toString()).param("kategoriaId",expense.toString())
                .param("size","2").param("allapot","TERVEZETT"),200)
                .andExpect(jsonPath("$.content.length()").value(2)).andExpect(jsonPath("$.page.totalElements").value(3));
        perform(get(base+"/penzmozgasok").param("sort","jelszo,asc"),400);
        perform(get(base+"/szamlak").param("size","1000"),200).andExpect(jsonPath("$.page.size").value(100));
    }
    @Test void statisticsSeparateCurrenciesAndExcludeTransfersPlansAndCancelledItems() throws Exception {
        created("/penzmozgasok",money(bank,expense,PenzmozgasTipus.KIADAS,"100",PenzmozgasAllapot.KONYVELT));
        created("/penzmozgasok",money(euro,expense,PenzmozgasTipus.KIADAS,"5",PenzmozgasAllapot.KONYVELT));
        created("/penzmozgasok",money(bank,expense,PenzmozgasTipus.KIADAS,"200",PenzmozgasAllapot.TERVEZETT));
        send(post(base+"/atvezetesek"),transfer(bank,cash,"50","50"),201);
        var result = perform(get(base+"/statisztika").param("tol",TODAY.toString()).param("ig",TODAY.toString()),200)
                .andReturn().getResponse();
        var rows = json.readTree(result.getContentAsString());
        assertThat(rows.size()).isEqualTo(2);
        assertThat(rows.get(0).get("penznem").asText()).isEqualTo("EUR");
        assertThat(rows.get(0).get("osszeg").decimalValue()).isEqualByComparingTo("5");
        assertThat(rows.get(1).get("osszeg").decimalValue()).isEqualByComparingTo("100");
    }
    @Test void recurringGenerationIsIdempotentAndCreatesPlansNotExpenses() throws Exception {
        var request = new IsmetlesRequest(bank,expense,"Napi",new BigDecimal("10"),IsmetlodesGyakorisag.NAP,1,TODAY.minusDays(3),TODAY);
        Long id = created("/ismetlesek",request);
        perform(post(base+"/ismetlesek/"+id+"/feldolgozas"),200).andExpect(jsonPath("$.length()").value(4));
        perform(post(base+"/ismetlesek/"+id+"/feldolgozas"),200).andExpect(jsonPath("$.length()").value(0));
        perform(get(base+"/ismetlesek/"+id),200).andExpect(jsonPath("$.aktiv").value(false));
        assertThat(movements.count()).isEqualTo(4); balance(bank,"1000");
    }
    @Test void usedScheduleCannotChangeAnchorAndActiveCategoryCannotBeDisabled() throws Exception {
        var request = new IsmetlesRequest(bank,expense,"Havi",BigDecimal.TEN,IsmetlodesGyakorisag.HONAP,1,TODAY.minusMonths(1),null);
        Long id = created("/ismetlesek",request);
        perform(post(base+"/ismetlesek/"+id+"/feldolgozas"),200);
        send(put(base+"/ismetlesek/"+id),new IsmetlesRequest(bank,expense,"Havi",BigDecimal.TEN,
                IsmetlodesGyakorisag.HONAP,1,TODAY,null),409);
        send(put(base+"/kategoriak/"+expense),new FelhKategoriaRequest(null,false),409);
        perform(delete(base+"/ismetlesek/"+id),204);
        send(put(base+"/kategoriak/"+expense),new FelhKategoriaRequest(null,false),200);
    }
    @Test void zeroBalanceAccountWithActiveScheduleCannotBeArchived() throws Exception {
        created("/ismetlesek",new IsmetlesRequest(cash,expense,"Napi",BigDecimal.ONE,IsmetlodesGyakorisag.NAP,1,TODAY,null));
        perform(delete(base+"/szamlak/"+cash),409);
    }
    @Test void concurrentRecurringProcessingDoesNotDuplicateOccurrences() throws Exception {
        Long id = created("/ismetlesek",new IsmetlesRequest(bank,expense,"Napi",BigDecimal.TEN,
                IsmetlodesGyakorisag.NAP,1,TODAY.minusDays(3),null));
        String path = base+"/ismetlesek/"+id+"/feldolgozas";
        assertThat(concurrent(() -> mvc.perform(post(path)).andReturn().getResponse().getStatus(),
                () -> mvc.perform(post(path)).andReturn().getResponse().getStatus())).containsOnly(200);
        assertThat(movements.count()).isEqualTo(4);
        balance(bank,"1000");
    }
    @Test void savingsCalculatorSupportsZeroInterestAndMonthlyCompounding() throws Exception {
        var zero = new MegtakaritasRequest("Terv","HUF",new BigDecimal("1000"),new BigDecimal("100"),BigDecimal.ZERO,12,null);
        send(post("/api/megtakaritas-kalkulator"),zero,200)
                .andExpect(jsonPath("$.varhatoVegosszeg").value(2200)).andExpect(jsonPath("$.kamatOsszesen").value(0));
        var compound = new MegtakaritasRequest("Terv","HUF",new BigDecimal("1000"),new BigDecimal("100"),new BigDecimal("12"),2,new BigDecimal("1500"));
        send(post("/api/megtakaritas-kalkulator"),compound,200)
                .andExpect(jsonPath("$.varhatoVegosszeg").value(1221.1))
                .andExpect(jsonPath("$.kamatOsszesen").value(21.1)).andExpect(jsonPath("$.celhozHianyzoOsszeg").value(278.9));
    }
    @Test void savedPlansHaveCrudOwnershipAndCalculation() throws Exception {
        var request = new MegtakaritasRequest("Laptop","EUR",BigDecimal.TEN,BigDecimal.ONE,BigDecimal.ZERO,12,null);
        Long id = created("/megtakaritasok",request);
        perform(get(base+"/megtakaritasok/"+id+"/kalkulacio"),200).andExpect(jsonPath("$.varhatoVegosszeg").value(22));
        send(put(base+"/megtakaritasok/"+id),request,200);
        perform(get("/api/felhasznalok/"+other+"/megtakaritasok/"+id),404);
        perform(delete(base+"/megtakaritasok/"+id),204);
        perform(get(base+"/megtakaritasok/"+id),404);
    }
    @Test void globalCatalogsHaveCrudAndDuplicateProtection() throws Exception {
        var role = sendJson(post("/api/jogosultsagok"),new JogRequest("TESZT","Leírás"),201);
        long id = role.get("id").asLong();
        perform(get("/api/jogosultsagok/"+id),200);
        send(put("/api/jogosultsagok/"+id),new JogRequest("TESZT2",null),200);
        send(post("/api/jogosultsagok"),new JogRequest("TESZT2",null),409);
        perform(delete("/api/jogosultsagok/"+id),204);
        var category = sendJson(post("/api/kategoriak"),new KategoriaRequest("Szabad",KategoriaTipus.KIADAS,null),201);
        long cid = category.get("id").asLong();
        send(put("/api/kategoriak/"+cid),new KategoriaRequest("Szabad2",KategoriaTipus.BEVETEL,null),200);
        perform(delete("/api/kategoriak/"+cid),204);
    }
    @Test void inactiveUserCannotPostFinancialChanges() throws Exception {
        send(patch("/api/felhasznalo/"+owner+"/aktiv"),new AktivRequest(false),204);
        send(post(base+"/penzmozgasok"),money(bank,expense,PenzmozgasTipus.KIADAS,"1",PenzmozgasAllapot.KONYVELT),409);
        perform(get(base+"/szamlak"),200);
    }
    @Test void concurrentExpensesCannotOverspendTheSameBalance() throws Exception {
        var statuses = concurrent(() -> sendStatus(post(base+"/penzmozgasok"),money(bank,expense,PenzmozgasTipus.KIADAS,"700",PenzmozgasAllapot.KONYVELT)),
                () -> sendStatus(post(base+"/penzmozgasok"),money(bank,expense,PenzmozgasTipus.KIADAS,"700",PenzmozgasAllapot.KONYVELT)));
        assertThat(statuses).containsExactlyInAnyOrder(201,409);
        balance(bank,"300"); assertThat(movements.count()).isEqualTo(1);
    }
    @Test void concurrentBookingOfOnePlanChargesOnlyOnce() throws Exception {
        Long id = created("/penzmozgasok",money(bank,expense,PenzmozgasTipus.KIADAS,"100",PenzmozgasAllapot.TERVEZETT));
        String path = base+"/penzmozgasok/"+id+"/konyveles";
        assertThat(concurrent(() -> mvc.perform(post(path)).andReturn().getResponse().getStatus(),
                () -> mvc.perform(post(path)).andReturn().getResponse().getStatus())).containsOnly(200);
        balance(bank,"900");
    }
    @Test void oppositeTransfersUseStableLockOrder() throws Exception {
        created("/penzmozgasok",money(cash,income,PenzmozgasTipus.BEVETEL,"1000",PenzmozgasAllapot.KONYVELT));
        assertThat(concurrent(() -> sendStatus(post(base+"/atvezetesek"),transfer(bank,cash,"100","100")),
                () -> sendStatus(post(base+"/atvezetesek"),transfer(cash,bank,"100","100")))).containsOnly(201);
        balance(bank,"1000"); balance(cash,"1000");
    }
    @Test void openApiDescribesTheNewFunctionalEndpoints() throws Exception {
        perform(get("/v3/api-docs"),200).andExpect(jsonPath("$.paths['/api/megtakaritas-kalkulator']").exists())
                .andExpect(jsonPath("$.paths['/api/felhasznalok/{felhasznaloId}/atvezetesek']").exists());
    }

    private Long user(String name) {
        var u = new FelhasznaloEntity(); u.setNev(name); u.setFelhasznalonev(name); u.setEmail(name+"@example.test");
        u.setJelszo("demo"); u.setSzuletesiDatum(LocalDate.of(2000,1,1)); return users.saveAndFlush(u).getId();
    }
    private Long account(Long user,String name,SzamlaTipus type,String currency,String opening) {
        return accounts.create(user,new SzamlaCreateRequest(name,type,currency,new BigDecimal(opening),LocalDate.of(2020,1,1),null,null)).id();
    }
    private KartyaRequest cardRequest(Long account,String name,int year) { return new KartyaRequest(account,name,"Anna",KartyaTarsasag.VISA,"1234",12,year); }
    private PenzmozgasRequest money(Long account,Long category,PenzmozgasTipus type,String amount,PenzmozgasAllapot state) {
        return new PenzmozgasRequest(account,category,null,type,new BigDecimal(amount),NOW.minusSeconds(60),state,"Teszt",null);
    }
    private AtvezetesRequest transfer(Long source,Long target,String amount,String targetAmount) {
        return new AtvezetesRequest(source,target,new BigDecimal(amount),new BigDecimal(targetAmount),NOW.minusSeconds(60),PenzmozgasAllapot.KONYVELT,"Átvezetés",null);
    }
    private void balance(Long account,String expected) { assertThat(accounts.get(owner,account).egyenleg()).isEqualByComparingTo(expected); }
    private Long created(String path,Object request) throws Exception { return sendJson(post(base+path),request,201).get("id").asLong(); }
    private ResultActions send(MockHttpServletRequestBuilder request,Object body,int status) throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().is(status));
    }
    private int sendStatus(MockHttpServletRequestBuilder request,Object body) throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andReturn().getResponse().getStatus();
    }
    private JsonNode sendJson(MockHttpServletRequestBuilder request,Object body,int status) throws Exception {
        return json.readTree(send(request,body,status).andReturn().getResponse().getContentAsString());
    }
    private ResultActions perform(MockHttpServletRequestBuilder request,int status) throws Exception { return mvc.perform(request).andExpect(status().is(status)); }
    private List<Integer> concurrent(Callable<Integer> first,Callable<Integer> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = List.of(first,second).stream().map(task -> executor.submit(() -> {
                ready.countDown(); if (!start.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("Tesztindítási timeout");
                return task.call();
            })).toList();
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue(); start.countDown();
            return List.of(futures.get(0).get(20,TimeUnit.SECONDS),futures.get(1).get(20,TimeUnit.SECONDS));
        }
    }
}
