package hu.unideb.inf.kiadaskezelo.service.impl;

import hu.unideb.inf.kiadaskezelo.data.repository.FelhasznaloRepository;
import hu.unideb.inf.kiadaskezelo.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final FelhasznaloRepository repo;

    public UserServiceImpl(FelhasznaloRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetailsService getUserDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                return repo.findByFelhasznalonev(username)
                        .orElseThrow(
                                () -> new UsernameNotFoundException(username)
                        );
            }
        };
    }
}
