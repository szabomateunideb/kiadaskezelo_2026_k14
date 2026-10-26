package hu.unideb.inf.kiadaskezelo.service.exception;

public class FoglaltFelhasznaloAdatException extends RuntimeException {
    private final String field;

    public FoglaltFelhasznaloAdatException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
