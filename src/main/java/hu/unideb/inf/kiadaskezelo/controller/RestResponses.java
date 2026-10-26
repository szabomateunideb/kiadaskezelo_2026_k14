package hu.unideb.inf.kiadaskezelo.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

final class RestResponses {
    private RestResponses() {}
    static <T> ResponseEntity<T> created(T value, Long id) {
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(id).toUri()).body(value);
    }
}
