package hu.unideb.inf.kiadaskezelo.data.entity;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.SzamlaTipus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "SZAMLA", indexes = @Index(name = "idx_szamla_felh", columnList = "felh_id"))
@Getter
@Setter
@NoArgsConstructor
public class SzamlaEntity extends AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "felh_id", nullable = false)
    private FelhasznaloEntity felhasznalo;

    @Column(nullable = false, length = 100)
    private String nev;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SzamlaTipus tipus;

    @Pattern(regexp = "[A-Z]{3}")
    @Column(nullable = false, length = 3)
    private String penznem = "HUF";

    @Digits(integer = 15, fraction = 4)
    @Column(name = "nyito_egyenleg", nullable = false, precision = 19, scale = 4)
    private BigDecimal nyitoEgyenleg = BigDecimal.ZERO;

    @Column(name = "nyitas_datum", nullable = false)
    private LocalDate nyitasDatum;

    @Column(name = "bank_nev", length = 100)
    private String bankNev;

    @Column(length = 34)
    private String iban;

    @Column(nullable = false)
    private boolean aktiv = true;
}
