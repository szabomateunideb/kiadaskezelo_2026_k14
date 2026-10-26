package hu.unideb.inf.kiadaskezelo;

import hu.unideb.inf.kiadaskezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.kiadaskezelo.data.entity.JogEntity;
import hu.unideb.inf.kiadaskezelo.data.repository.FelhasznaloRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.properties.hibernate.generate_statistics=true", "spring.jpa.show-sql=false"})
@ActiveProfiles("test")
class FelhasznaloFetchTests {
    @Autowired EntityManager entityManager;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired FelhasznaloRepository repository;

    private Statistics statistics;

    @BeforeEach
    void seedDistinctRoles() {
        for (int i = 0; i < 3; i++) {
            JogEntity role = new JogEntity();
            role.setNev("jog-" + i);
            entityManager.persist(role);
            FelhasznaloEntity user = new FelhasznaloEntity();
            user.setNev("Teszt " + i);
            user.setSzuletesiDatum(LocalDate.of(2000, 1, 1));
            user.setFelhasznalonev("teszt-" + i);
            user.setEmail("teszt-" + i + "@example.com");
            user.setJelszo("teszt");
            user.setJog(role);
            entityManager.persist(user);
        }
        entityManager.flush();
        entityManager.clear();
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    void lazyLoadingWithoutFetchPlanDemonstratesNPlusOne() {
        var users = entityManager.createQuery("select f from FelhasznaloEntity f", FelhasznaloEntity.class)
                .getResultList();
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        assertThat(Hibernate.isInitialized(users.getFirst().getJog())).isFalse();
        users.forEach(user -> assertThat(user.getJog().getNev()).startsWith("jog-"));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
    }

    @Test
    void entityGraphLoadsRolesInPageQueryWithoutExtraSelects() {
        var page = repository.findAll(PageRequest.of(0, 2));
        assertThat(page.getTotalElements()).isEqualTo(3);
        // Egy adatlekérdezés és egy COUNT; a jogosultságokhoz nincs további SELECT.
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        entityManager.clear();
        page.forEach(user -> {
            assertThat(Hibernate.isInitialized(user.getJog())).isTrue();
            assertThat(user.getJog().getNev()).startsWith("jog-");
        });
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    void filteredPageAlsoUsesEntityGraph() {
        var page = repository.findAllByNevContainingIgnoreCase("TESZT", PageRequest.of(0, 2));
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        entityManager.clear();
        page.forEach(user -> assertThat(user.getJog().getNev()).startsWith("jog-"));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }
}
