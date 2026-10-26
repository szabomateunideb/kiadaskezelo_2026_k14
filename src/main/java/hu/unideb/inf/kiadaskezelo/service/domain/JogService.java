package hu.unideb.inf.kiadaskezelo.service.domain;

import hu.unideb.inf.kiadaskezelo.data.entity.JogEntity;
import hu.unideb.inf.kiadaskezelo.data.repository.JogRepository;
import hu.unideb.inf.kiadaskezelo.service.dto.domain.*;
import hu.unideb.inf.kiadaskezelo.service.exception.*;
import hu.unideb.inf.kiadaskezelo.service.mapper.PenzugyiMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JogService {
    private final JogRepository roles;
    private final PenzugyiMapper mapper;
    public Page<JogResponse> list(Pageable page) {
        return roles.findAll(Lapozas.korlatoz(page, "id", "nev")).map(mapper::jog);
    }
    public JogResponse get(Long id) { return mapper.jog(entity(id)); }
    @Transactional
    public JogResponse save(Long id, @NotNull @Valid JogRequest request) {
        var entity = id == null ? new JogEntity() : entity(id);
        roles.findByNev(request.nev().strip()).filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new UzletiSzabalyException("A jogosultság neve már foglalt."); });
        entity.setNev(request.nev().strip());
        entity.setLeiras(request.leiras());
        return mapper.jog(roles.saveAndFlush(entity));
    }
    @Transactional
    public void delete(Long id) {
        roles.delete(entity(id));
        roles.flush();
    }
    private JogEntity entity(Long id) {
        return roles.findById(id).orElseThrow(() -> new ErforrasNotFoundException("Nem található jogosultság: " + id));
    }
}
