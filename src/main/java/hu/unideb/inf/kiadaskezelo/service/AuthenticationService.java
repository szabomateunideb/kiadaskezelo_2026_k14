package hu.unideb.inf.kiadaskezelo.service;

import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloCreateRequest;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloLoginRequest;

public interface AuthenticationService {

    public void regisztracio(FelhasznaloCreateRequest dto);
    public void bejelentkezes(FelhasznaloLoginRequest dto);
}
