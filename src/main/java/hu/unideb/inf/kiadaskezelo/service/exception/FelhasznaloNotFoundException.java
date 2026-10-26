package hu.unideb.inf.kiadaskezelo.service.exception;

public class FelhasznaloNotFoundException extends RuntimeException {
    public FelhasznaloNotFoundException(Long id) {
        super("Nem található felhasználó ezzel az azonosítóval: " + id);
    }
}
