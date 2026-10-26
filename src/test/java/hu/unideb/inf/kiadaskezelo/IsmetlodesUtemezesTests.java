package hu.unideb.inf.kiadaskezelo;
import hu.unideb.inf.kiadaskezelo.service.domain.IsmetlodesUtemezes;
import hu.unideb.inf.kiadaskezelo.data.entity.enums.IsmetlodesGyakorisag;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class IsmetlodesUtemezesTests {
    @Test void monthEndReturnsToOriginalDayAfterFebruary() {
        var anchor = LocalDate.of(2026,1,31);
        var feb = IsmetlodesUtemezes.kovetkezo(anchor,anchor,IsmetlodesGyakorisag.HONAP,1);
        assertThat(feb).isEqualTo(LocalDate.of(2026,2,28));
        assertThat(IsmetlodesUtemezes.kovetkezo(anchor,feb,IsmetlodesGyakorisag.HONAP,1)).isEqualTo(LocalDate.of(2026,3,31));
    }
    @Test void annualLeapDayReturnsInNextLeapYear() {
        var anchor = LocalDate.of(2024,2,29);
        assertThat(IsmetlodesUtemezes.kovetkezo(anchor,LocalDate.of(2027,2,28),IsmetlodesGyakorisag.EV,1))
                .isEqualTo(LocalDate.of(2028,2,29));
    }
    @Test void weeklyIntervalUsesOriginalAnchor() {
        var anchor = LocalDate.of(2026,1,1);
        assertThat(IsmetlodesUtemezes.kovetkezo(anchor,anchor.plusWeeks(2),IsmetlodesGyakorisag.HET,2)).isEqualTo(anchor.plusWeeks(4));
    }
}
