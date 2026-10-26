package hu.unideb.inf.kiadaskezelo.controller.error;

import hu.unideb.inf.kiadaskezelo.service.exception.FelhasznaloNotFoundException;
import hu.unideb.inf.kiadaskezelo.service.exception.FoglaltFelhasznaloAdatException;
import hu.unideb.inf.kiadaskezelo.service.exception.HibasKeresException;
import hu.unideb.inf.kiadaskezelo.service.exception.ErforrasNotFoundException;
import hu.unideb.inf.kiadaskezelo.service.exception.UzletiSzabalyException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(java.time.DateTimeException.class)
    public ProblemDetail handleInvalidDate(java.time.DateTimeException exception) {
        return problem(HttpStatus.BAD_REQUEST, "hibas-datum", "Érvénytelen dátum",
                "A dátum kívül esik a támogatott tartományon.");
    }

    @ExceptionHandler(ErforrasNotFoundException.class)
    public ProblemDetail handleMissingResource(ErforrasNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "nem-talalhato", "Nem található", exception.getMessage());
    }

    @ExceptionHandler(UzletiSzabalyException.class)
    public ProblemDetail handleBusinessConflict(UzletiSzabalyException exception) {
        return problem(HttpStatus.CONFLICT, "uzleti-utkozes", "A művelet nem hajtható végre", exception.getMessage());
    }

    @ExceptionHandler({PessimisticLockingFailureException.class, ObjectOptimisticLockingFailureException.class})
    public ProblemDetail handleConcurrentChange(Exception exception) {
        return problem(HttpStatus.CONFLICT, "parhuzamos-modositas", "Párhuzamos módosítás",
                "Az adatot másik művelet módosítja. Próbálja újra a kérést.");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleServiceValidation(ConstraintViolationException exception) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "validacios-hiba", "Érvénytelen kérés",
                "Egy vagy több paraméter értéke érvénytelen.");
        detail.setProperty("errors", exception.getConstraintViolations().stream().map(v ->
                new FieldViolation(v.getPropertyPath().toString(), v.getMessage())).toList());
        return detail;
    }

    public record FieldViolation(String field, String message) {
    }

    @ExceptionHandler(FelhasznaloNotFoundException.class)
    public ProblemDetail handleNotFound(FelhasznaloNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "felhasznalo-nem-talalhato",
                "Nincs ilyen felhasználó", exception.getMessage());
    }

    @ExceptionHandler(FoglaltFelhasznaloAdatException.class)
    public ProblemDetail handleDuplicate(FoglaltFelhasznaloAdatException exception) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "foglalt-adat",
                "Foglalt felhasználói adat", exception.getMessage());
        problem.setProperty("errors", List.of(new FieldViolation(exception.getField(), exception.getMessage())));
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDatabaseConflict(DataIntegrityViolationException exception) {
        // A párhuzamos kérések az előzetes ellenőrzésen egyszerre is átjuthatnak.
        // Az adatbázis-kényszer hibájából nem küldünk SQL-t vagy belső részleteket a kliensnek.
        return problem(HttpStatus.CONFLICT, "adatbazis-utkozes", "Adatütközés",
                "Az adatok ütköznek egy egyediségi vagy hivatkozási adatbázis-korlátozással.");
    }

    @ExceptionHandler(HibasKeresException.class)
    public ProblemDetail handleBadRequest(HibasKeresException exception) {
        return problem(HttpStatus.BAD_REQUEST, "hibas-keres", "Hibás kérés", exception.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validacios-hiba", "Érvénytelen kérés",
                "Egy vagy több mező értéke érvénytelen.");
        problem.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::message))
                .toList());
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (exception.isForReturnValue()) {
            return handleExceptionInternal(exception, handleUnexpected(exception), headers, status, request);
        }
        // Például a @Positive path paraméter és az @Valid kérés együttes validációja.
        List<FieldViolation> errors = new ArrayList<>();
        exception.getParameterValidationResults().forEach(result -> {
            if (result instanceof ParameterErrors parameterErrors) {
                parameterErrors.getFieldErrors().forEach(error ->
                        errors.add(new FieldViolation(error.getField(), error.getDefaultMessage())));
            } else {
                String name = result.getMethodParameter().getParameterName();
                result.getResolvableErrors().forEach(error -> errors.add(new FieldViolation(
                        name == null ? "parameter" : name, error.getDefaultMessage())));
            }
        });
        errors.sort(Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::message));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validacios-hiba", "Érvénytelen kérés",
                "Egy vagy több mező vagy paraméter értéke érvénytelen.");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "hibas-json", "Hibás kérés",
                "A kérés törzse hiányzik, hibás JSON-t, ismeretlen mezőt vagy érvénytelen adattípust tartalmaz.");
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // A Spring beépített hibái (például 405, 415, hibás path paraméter) is ezt a formát használják.
        ProblemDetail problem = body instanceof ProblemDetail detail ? detail
                : createProblemDetail(exception, status, "A kérés feldolgozása sikertelen.", null, null, request);
        if (problem.getProperties() == null || !problem.getProperties().containsKey("errors")) {
            problem.setProperty("errors", List.of());
        }
        return super.handleExceptionInternal(exception, problem, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        log.error("Nem várt hiba a kérés feldolgozása közben", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "belso-hiba", "Belső szerverhiba",
                "A kérés feldolgozása váratlan hiba miatt sikertelen.");
    }

    private ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:kiadaskezelo:problem:" + type));
        problem.setTitle(title);
        problem.setProperty("errors", List.of());
        return problem;
    }
}
