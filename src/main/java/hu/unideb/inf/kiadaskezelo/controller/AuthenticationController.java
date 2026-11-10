package hu.unideb.inf.kiadaskezelo.controller;

import hu.unideb.inf.kiadaskezelo.service.AuthenticationService;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloCreateRequest;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloLoginRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("auth")
public class AuthenticationController {

    private final AuthenticationService auth;

    public AuthenticationController(AuthenticationService auth) {
        this.auth = auth;
    }

    @PostMapping("/regisztracio")
    public void regisztracio(
            @Valid
            @RequestBody FelhasznaloCreateRequest dto) {
        auth.regisztracio(dto);
    }

    @PostMapping("/bejelentkezes")
    public String bejelentkezes(@RequestBody FelhasznaloLoginRequest dto){
        return auth.bejelentkezes(dto);
    }
}
