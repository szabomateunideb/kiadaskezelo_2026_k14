package hu.unideb.inf.kiadaskezelo.service.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.kiadaskezelo.data.entity.JogEntity;
import hu.unideb.inf.kiadaskezelo.data.repository.FelhasznaloRepository;
import hu.unideb.inf.kiadaskezelo.data.repository.JogRepository;
import hu.unideb.inf.kiadaskezelo.service.AuthenticationService;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloCreateRequest;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloLoginRequest;
import hu.unideb.inf.kiadaskezelo.service.mapper.FelhasznaloMapper;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final FelhasznaloRepository frepo;
    private final JogRepository jrepo;
    private final FelhasznaloMapper fmapper;

    public AuthenticationServiceImpl(FelhasznaloRepository frepo, JogRepository jrepo, FelhasznaloMapper fmapper) {
        this.frepo = frepo;
        this.jrepo = jrepo;
        this.fmapper = fmapper;
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
        frepo.save(f);
    }

    @Override
    public void bejelentkezes(FelhasznaloLoginRequest dto) {

    }
}
