package hu.unideb.inf.kiadaskezelo.service.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(prefix="app.recurring", name="enabled", havingValue="true")
public class IsmetlodesFeldolgozo {
    private final IsmetlodesService service;
    @Scheduled(fixedDelayString="${app.recurring.delay-ms:60000}", initialDelayString="${app.recurring.initial-delay-ms:30000}")
    public void run() {
        for (var schedule : service.esedekesek()) {
            try {
                // Külön service-hívás, külön tranzakció: egy hibás sorozat nem állítja le a többit.
                service.feldolgoz(service.tulajdonos(schedule.id()), schedule.id());
            } catch (RuntimeException exception) {
                log.warn("Az ismétlődés feldolgozása sikertelen, id={}: {}", schedule.id(), exception.getMessage());
            }
        }
    }
}
