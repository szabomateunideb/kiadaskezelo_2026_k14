package hu.unideb.inf.kiadaskezelo.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "UTALAS", indexes = @Index(name = "idx_utalas_celszamla", columnList = "cel_szamla_id"),
        check = @CheckConstraint(name = "ck_utalas_celosszeg", constraint =
                "(cel_szamla_id is null and cel_osszeg is null) or "
                + "(cel_szamla_id is not null and cel_osszeg is not null and cel_osszeg > 0)"))
@Getter
@Setter
@NoArgsConstructor
public class UtalasEntity extends AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "penzmozg_id", nullable = false, unique = true)
    private PenzmozgasEntity penzmozgas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cel_szamla_id")
    private SzamlaEntity celSzamla;

    @Positive
    @Digits(integer = 15, fraction = 4)
    @Column(name = "cel_osszeg", precision = 19, scale = 4)
    private BigDecimal celOsszeg;

    @Column(name = "partner_nev", length = 150)
    private String partnerNev;

    @Column(name = "partner_iban", length = 34)
    private String partnerIban;

    @Column(length = 500)
    private String kozlemeny;

    @Column(name = "kulso_azonosito", length = 100)
    private String kulsoAzonosito;
}
