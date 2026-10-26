package hu.unideb.inf.kiadaskezelo.data.entity;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.IsmetlodesGyakorisag;
import hu.unideb.inf.kiadaskezelo.data.entity.enums.KategoriaTipus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ISMETLODO_PENZMOZGAS", indexes =
        @Index(name = "idx_ismetles_esedekes", columnList = "aktiv,kovetkezo_datum"),
        check = @CheckConstraint(name = "ck_ismetles_ertekek",
                constraint = "osszeg > 0 and intervallum > 0 and (vegdatum is null or vegdatum >= kezdo_datum)"))
@Getter
@Setter
@NoArgsConstructor
public class IsmetlodoPenzmozgasEntity extends AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "szamla_id", nullable = false)
    private SzamlaEntity szamla;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kat_id", nullable = false)
    private KategoriaEntity kategoria;

    @Column(nullable = false, length = 150)
    private String megnevezes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private KategoriaTipus tipus;

    @Positive
    @Digits(integer = 15, fraction = 4)
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal osszeg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private IsmetlodesGyakorisag gyakorisag;

    @Positive
    @Column(nullable = false)
    private int intervallum = 1;

    @Column(name = "kezdo_datum", nullable = false)
    private LocalDate kezdoDatum;

    @Column(name = "kovetkezo_datum", nullable = false)
    private LocalDate kovetkezoDatum;

    private LocalDate vegdatum;

    @Column(nullable = false)
    private boolean aktiv = true;
}
