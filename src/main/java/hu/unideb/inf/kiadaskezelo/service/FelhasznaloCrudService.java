package hu.unideb.inf.kiadaskezelo.service;

import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloCreateRequest;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloResponse;
import hu.unideb.inf.kiadaskezelo.service.dto.FelhasznaloUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FelhasznaloCrudService {

    FelhasznaloResponse save(FelhasznaloCreateRequest request);
    FelhasznaloResponse update(Long id, FelhasznaloUpdateRequest request);
    FelhasznaloResponse findById(Long id);
    Page<FelhasznaloResponse> findAll(String nev, Pageable pageable);
    void deleteAll();
    void deleteById(Long id);
}
