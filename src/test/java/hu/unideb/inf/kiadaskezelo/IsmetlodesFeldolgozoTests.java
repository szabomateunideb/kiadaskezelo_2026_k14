package hu.unideb.inf.kiadaskezelo;

import hu.unideb.inf.kiadaskezelo.service.domain.*;
import hu.unideb.inf.kiadaskezelo.service.dto.domain.IsmetlesResponse;
import hu.unideb.inf.kiadaskezelo.service.exception.UzletiSzabalyException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.Mockito.*;

class IsmetlodesFeldolgozoTests {
    @Test void failureOfOneScheduleDoesNotStopTheNext() {
        var service = mock(IsmetlodesService.class);
        var first = mock(IsmetlesResponse.class);
        var second = mock(IsmetlesResponse.class);
        when(first.id()).thenReturn(1L);
        when(second.id()).thenReturn(2L);
        when(service.esedekesek()).thenReturn(List.of(first,second));
        when(service.tulajdonos(anyLong())).thenReturn(10L);
        when(service.feldolgoz(10L,1L)).thenThrow(new UzletiSzabalyException("Inaktív számla"));
        when(service.feldolgoz(10L,2L)).thenReturn(List.of());
        new IsmetlodesFeldolgozo(service).run();
        verify(service).feldolgoz(10L,1L);
        verify(service).feldolgoz(10L,2L);
    }
}
