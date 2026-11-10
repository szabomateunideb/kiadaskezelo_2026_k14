package hu.unideb.inf.kiadaskezelo.service.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.kiadaskezelo.data.entity.JogEntity;
import hu.unideb.inf.kiadaskezelo.data.repository.FelhasznaloRepository;
import hu.unideb.inf.kiadaskezelo.data.repository.JogRepository;
import hu.unideb.inf.kiadaskezelo.service.AuthenticationService;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloCreateRequest;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloLoginRequest;
import hu.unideb.inf.kiadaskezelo.service.mapper.FelhasznaloMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final FelhasznaloRepository frepo;
    private final JogRepository jrepo;
    private final FelhasznaloMapper fmapper;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthenticationServiceImpl(FelhasznaloRepository frepo, JogRepository jrepo, FelhasznaloMapper fmapper, AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.frepo = frepo;
        this.jrepo = jrepo;
        this.fmapper = fmapper;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Override
    public void regisztracio(FelhasznaloCreateRequest dto) {
        FelhasznaloEntity f = fmapper.toEntity(dto);
        JogEntity j = jrepo.findByNev("FELHASZNALO").orElse(null);

        if( j != null ) {
            f.setJog(j);
        } else {
            j = new JogEntity();
            j.setNev("FELHASZNALO");
            j = jrepo.save(j);

            f.setJog(j);
        }
        f.setJelszo(passwordEncoder.encode(f.getJelszo()));
        frepo.save(f);
    }

    @Override
    public String bejelentkezes(FelhasznaloLoginRequest dto) {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        dto.felhasznalonev(),dto.jelszo()
                )
        );
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        var user = frepo.findByFelhasznalonev(dto.felhasznalonev());
        return tokenService.generateToken(user.get());
    }
}
