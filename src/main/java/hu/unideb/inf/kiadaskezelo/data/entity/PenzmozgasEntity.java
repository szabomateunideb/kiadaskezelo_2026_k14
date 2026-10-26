package hu.unideb.inf.kiadaskezelo.data.entity;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.PenzmozgasAllapot;
import hu.unideb.inf.kiadaskezelo.data.entity.enums.PenzmozgasTipus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "PENZMOZGAS",
        uniqueConstraints = @UniqueConstraint(name = "uk_penzmozgas_ismetles_esedekesseg",
                columnNames = {"ismetles_id", "esedekesseg"}),
        indexes = @Index(name = "idx_penzmozgas_szamla_datum", columnList = "szamla_id,datum"),
        check = @CheckConstraint(name = "ck_penzmozgas_ertekek",
                constraint = "osszeg > 0 and (ismetles_id is null or esedekesseg is not null)"))
@Getter
@Setter
@NoArgsConstructor
public class PenzmozgasEntity extends AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "szamla_id", nullable = false)
    private SzamlaEntity szamla;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kat_id")
    private KategoriaEntity kategoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kartya_id")
    private KartyaEntity kartya;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ismetles_id")
    private IsmetlodoPenzmozgasEntity ismetles;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private PenzmozgasTipus tipus;

    @Positive
    @Digits(integer = 15, fraction = 4)
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal osszeg;

    @Column(nullable = false)
    private Instant datum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private PenzmozgasAllapot allapot = PenzmozgasAllapot.TERVEZETT;

    @Column(nullable = false, length = 150)
    private String megnevezes;

    @Column(length = 2000)
    private String megjegyzes;

    private LocalDate esedekesseg;
}
