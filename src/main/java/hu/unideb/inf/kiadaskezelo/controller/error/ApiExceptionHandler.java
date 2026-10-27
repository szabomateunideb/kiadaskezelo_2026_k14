package hu.unideb.inf.kiadaskezelo.controller.error;

import hu.unideb.inf.kiadaskezelo.service.exception.FelhasznaloNotFoundException;
import hu.unideb.inf.kiadaskezelo.service.exception.HibasKeresException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler
        extends ResponseEntityExceptionHandler {

    public record FieldViolation(String field, String message){
    }

    @ExceptionHandler(FelhasznaloNotFoundException.class)
    public ProblemDetail handleNotFound(FelhasznaloNotFoundException exception){
        return problem(HttpStatus.NOT_FOUND
                , "felhasznalo-nem-talalhato"
                , "Nincs ilyen felhasználó"
                , exception.getMessage());
    }

    @ExceptionHandler(HibasKeresException.class)
    public ProblemDetail handleBadRequest(HibasKeresException exception){
        return problem(HttpStatus.BAD_REQUEST,
                "hibas-keres",
                "Hibás kérés",
                exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception){
        log.error("Nem várt hiba a kérés feldolgozása közben"
                , exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR
                ,"belso-hiba",
                "Belső szerverhiba",
                "A kérés feldolgozása váratlan hiba miatt sikertelen");
    }

    private ProblemDetail problem(HttpStatus status, String type, String title, String detail){
        ProblemDetail problem = ProblemDetail
                .forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("errors", List.of());
        return problem;
    }
}
