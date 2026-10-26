package hu.unideb.inf.kiadaskezelo.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.Instant;

@Getter
@MappedSuperclass
public abstract class AuditEntity {
    @Column(nullable = false, updatable = false)
    private Instant letrehozva;

    @Column(nullable = false)
    private Instant modositva;

    @PrePersist
    protected void letrehozasIdopontja() {
        Instant now = Instant.now();
        letrehozva = now;
        modositva = now;
    }

    @PreUpdate
    protected void modositasIdopontja() {
        modositva = Instant.now();
    }
}
