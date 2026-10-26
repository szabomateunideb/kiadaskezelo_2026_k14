package hu.unideb.inf.kiadaskezelo;

import hu.unideb.inf.kiadaskezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.kiadaskezelo.data.entity.JogEntity;
import hu.unideb.inf.kiadaskezelo.data.repository.FelhasznaloRepository;
import hu.unideb.inf.kiadaskezelo.data.repository.JogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FelhasznaloApiTests {
    private static final String API = "/api/felhasznalo";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JogRepository jogRepository;
    @MockitoSpyBean FelhasznaloRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
        jogRepository.deleteAll();
    }

    @Test
    void createReturns201LocationAndPasswordFreeResponse() throws Exception {
        var response = mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest("anna", "anna@example.com", "Anna")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(API + "/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.felhasznalonev").value("anna"))
                .andExpect(jsonPath("$.jelszo").doesNotExist())
                .andReturn().getResponse();
        mvc.perform(get(response.getHeader("Location")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nev").value("Anna"))
                .andExpect(jsonPath("$.jelszo").doesNotExist());
    }

    @Test
    void invalidInputReturnsFieldErrorsAndDoesNotSave() throws Exception {
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest("anna", "hibas-email", " ")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:kiadaskezelo:problem:validacios-hiba"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value(API))
                .andExpect(jsonPath("$.errors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("nev")))
                .andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist());
        assertThat(repository.count()).isZero();
    }

    @Test
    void validatesDatesAndStringLengths() throws Exception {
        String body = createRequest("anna", "anna@example.com", "Anna")
                .replace("2000-01-01", LocalDate.now().plusDays(1).toString())
                .replace("\"nem\": null", "\"nem\": \"12345678901\"");
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("szuletesiDatum")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("nem")));
    }

    @Test
    void malformedJsonAndUnknownFieldsAre400() throws Exception {
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
        String body = createRequest("anna", "anna@example.com", "Anna")
                .replace("\"nev\":", "\"id\": 999, \"nev\":");
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
    }

    @Test
    void missingUserReturns404ForReadUpdateAndDelete() throws Exception {
        mvc.perform(get(API + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Nincs ilyen felhasználó"))
                .andExpect(jsonPath("$.instance").value(API + "/999999"))
                .andExpect(jsonPath("$.errors").isEmpty());
        mvc.perform(put(API + "/999999").contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest("anna", "anna@example.com", "Anna")))
                .andExpect(status().isNotFound());
        mvc.perform(delete(API + "/999999")).andExpect(status().isNotFound());
    }

    @Test
    void badIdentifiersAndUnsupportedMethodsUseProblemDetails() throws Exception {
        mvc.perform(get(API + "/nem-szam"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors").isArray());
        mvc.perform(get(API + "/0")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("id")));
        mvc.perform(get(API).param("nev", "x".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("nev")));
        mvc.perform(patch(API + "/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void duplicateEmailAndUsernameReturn409() throws Exception {
        createUser("anna", "anna@example.com", "Anna");
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest("masik", "anna@example.com", "Másik")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest("anna", "masik@example.com", "Másik")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].field").value("felhasznalonev"));
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void updateUsesIdAndPreservesPasswordAndRole() throws Exception {
        long id = createUser("anna", "anna@example.com", "Anna");
        JogEntity jog = new JogEntity();
        jog.setNev("FELHASZNALO");
        jog = jogRepository.saveAndFlush(jog);
        FelhasznaloEntity entity = repository.findById(id).orElseThrow();
        entity.setJog(jog);
        repository.saveAndFlush(entity);

        mvc.perform(put(API + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest("anna", "anna@example.com", "Új név")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nev").value("Új név"))
                .andExpect(jsonPath("$.jogNev").value("FELHASZNALO"))
                .andExpect(jsonPath("$.jelszo").doesNotExist());
        FelhasznaloEntity saved = repository.findById(id).orElseThrow();
        assertThat(saved.getNev()).isEqualTo("Új név");
        assertThat(saved.getJelszo()).isEqualTo("tesztjelszo");
        assertThat(saved.getJog().getId()).isEqualTo(jog.getId());
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void updateRejectsProtectedFieldsAndInvalidData() throws Exception {
        long id = createUser("anna", "anna@example.com", "Anna");
        String body = updateRequest("anna", "anna@example.com", "Új név")
                .replace("\"nev\":", "\"jelszo\": \"atirt\", \"nev\":");
        mvc.perform(put(API + "/" + id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        mvc.perform(put(API + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest("anna", "hibas", "Anna")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("email")));
        assertThat(repository.findById(id).orElseThrow().getNev()).isEqualTo("Anna");
    }

    @Test
    void lateDatabaseConflictRollsBackTheWholeUpdate() throws Exception {
        createUser("anna", "anna@example.com", "Anna");
        long id = createUser("bela", "bela@example.com", "Béla");
        // Párhuzamos kérés esetét modellezzük: az előzetes ellenőrzés még nem lát ütközést.
        doReturn(false).when(repository).existsByEmailAndIdNot("anna@example.com", id);
        mvc.perform(put(API + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest("bela", "anna@example.com", "Ez sem menthető")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:kiadaskezelo:problem:adatbazis-utkozes"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("INSERT INTO"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("UPDATE FELHASZNALO"))));
        FelhasznaloEntity saved = repository.findById(id).orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("bela@example.com");
        assertThat(saved.getNev()).isEqualTo("Béla");
    }

    @Test
    void concurrentRegistrationsCannotCreateDuplicateEmails() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = List.of("anna", "bela").stream().map(username -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Nem indult el a párhuzamos teszt.");
                }
                return mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON)
                                .content(createRequest(username, "kozos@example.com", username)))
                        .andReturn().getResponse().getStatus();
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(results.get(0).get(15, TimeUnit.SECONDS), results.get(1).get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void listIsPagedSortedAndSupportsNonUniqueNameSearch() throws Exception {
        createUser("cecil", "cecil@example.com", "Cecil");
        createUser("anna1", "anna1@example.com", "Anna");
        createUser("anna2", "anna2@example.com", "Anna");
        mvc.perform(get(API).param("page", "0").param("size", "2").param("sort", "nev,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].nev").value("Anna"))
                .andExpect(jsonPath("$.content[0].jelszo").doesNotExist())
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));
        mvc.perform(get(API).param("page", "1").param("size", "2").param("sort", "nev,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nev").value("Cecil"));
        mvc.perform(get(API).param("nev", "aNn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void noMatchesReturn200AndEmptyContent() throws Exception {
        mvc.perform(get(API).param("nev", "nincs ilyen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(0))
                .andExpect(jsonPath("$.page.size").value(20));
    }

    @Test
    void pageSizeIsLimitedAndInvalidSortReturns400() throws Exception {
        mvc.perform(get(API).param("size", "1000"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.size").value(100));
        mvc.perform(get(API).param("sort", "nincsIlyenMezo,asc"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(get(API).param("sort", "jelszo,asc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletionReturns204AndThen404() throws Exception {
        long id = createUser("anna", "anna@example.com", "Anna");
        mvc.perform(delete(API + "/" + id)).andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mvc.perform(delete(API + "/" + id)).andExpect(status().isNotFound());
        assertThat(repository.count()).isZero();
    }

    @Test
    void legacyListAndBulkDeleteRoutesRemainAvailable() throws Exception {
        createUser("anna", "anna@example.com", "Anna");
        mvc.perform(get(API + "/felhasznalok"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(delete(API + "/felhasznalok")).andExpect(status().isNoContent());
        assertThat(repository.count()).isZero();
    }

    private long createUser(String username, String email, String name) throws Exception {
        var response = mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(username, email, name)))
                .andExpect(status().isCreated()).andReturn().getResponse();
        return objectMapper.readTree(response.getContentAsString()).get("id").asLong();
    }

    private String createRequest(String username, String email, String name) {
        return """
                {"nev": "%s", "szuletesiDatum": "2000-01-01", "felhasznalonev": "%s",
                 "jelszo": "tesztjelszo", "email": "%s", "nem": null}
                """.formatted(name, username, email);
    }

    private String updateRequest(String username, String email, String name) {
        return """
                {"nev": "%s", "szuletesiDatum": "2000-01-01", "felhasznalonev": "%s",
                 "email": "%s", "nem": null}
                """.formatted(name, username, email);
    }
}
