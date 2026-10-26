package hu.unideb.inf.kiadaskezelo.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "MEGTAKARITASI_TERV", indexes =
        @Index(name = "idx_megtakaritas_felh", columnList = "felh_id"),
        check = @CheckConstraint(name = "ck_megtakaritas_ertekek", constraint =
                "kezdo_toke >= 0 and havi_befizetes >= 0 and eves_kamat_szazalek >= 0 "
                + "and futamido_honap > 0 and (celosszeg is null or celosszeg > 0)"))
@Getter
@Setter
@NoArgsConstructor
public class MegtakaritasiTervEntity extends AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "felh_id", nullable = false)
    private FelhasznaloEntity felhasznalo;

    @Column(nullable = false, length = 100)
    private String nev;

    @Pattern(regexp = "[A-Z]{3}")
    @Column(nullable = false, length = 3)
    private String penznem = "HUF";

    @PositiveOrZero
    @Digits(integer = 15, fraction = 4)
    @Column(name = "kezdo_toke", nullable = false, precision = 19, scale = 4)
    private BigDecimal kezdoToke = BigDecimal.ZERO;

    @PositiveOrZero
    @Digits(integer = 15, fraction = 4)
    @Column(name = "havi_befizetes", nullable = false, precision = 19, scale = 4)
    private BigDecimal haviBefizetes = BigDecimal.ZERO;

    @DecimalMin("0.0")
    @Digits(integer = 5, fraction = 4)
    @Column(name = "eves_kamat_szazalek", nullable = false, precision = 9, scale = 4)
    private BigDecimal evesKamatSzazalek = BigDecimal.ZERO;

    @Positive
    @Column(name = "futamido_honap", nullable = false)
    private int futamidoHonap;

    @Positive
    @Digits(integer = 15, fraction = 4)
    @Column(precision = 19, scale = 4)
    private BigDecimal celosszeg;
}
