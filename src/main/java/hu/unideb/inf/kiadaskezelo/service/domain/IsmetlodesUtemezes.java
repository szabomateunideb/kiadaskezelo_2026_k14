package hu.unideb.inf.kiadaskezelo.service.domain;
import hu.unideb.inf.kiadaskezelo.data.entity.enums.IsmetlodesGyakorisag;
import java.time.*;
import java.time.temporal.ChronoUnit;

public final class IsmetlodesUtemezes {
    private IsmetlodesUtemezes() {}
    public static LocalDate kovetkezo(LocalDate anchor, LocalDate current, IsmetlodesGyakorisag frequency, int interval) {
        // Eredeti kezdőnap: január 31. -> február vége -> március 31.; február 29. is helyreáll.
        return switch (frequency) {
            case NAP -> anchor.plusDays((ChronoUnit.DAYS.between(anchor, current) / interval + 1) * interval);
            case HET -> anchor.plusWeeks((ChronoUnit.DAYS.between(anchor, current) / (7L * interval) + 1) * interval);
            case HONAP -> anchor.plusMonths((ChronoUnit.MONTHS.between(YearMonth.from(anchor), YearMonth.from(current)) / interval + 1) * interval);
            case EV -> anchor.plusYears(((current.getYear() - anchor.getYear()) / interval + 1L) * interval);
        };
    }
}
