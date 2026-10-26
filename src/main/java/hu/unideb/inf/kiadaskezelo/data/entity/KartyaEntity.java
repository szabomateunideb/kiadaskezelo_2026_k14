package hu.unideb.inf.kiadaskezelo.data.entity;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.KartyaTarsasag;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "KARTYA", indexes = @Index(name = "idx_kartya_szamla", columnList = "szamla_id"),
        check = @CheckConstraint(name = "ck_kartya_lejarat", constraint = "lejarati_honap between 1 and 12"))
@Getter
@Setter
@NoArgsConstructor
public class KartyaEntity extends AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "szamla_id", nullable = false)
    private SzamlaEntity szamla;

    @Column(nullable = false, length = 100)
    private String elnevezes;

    @Column(name = "birtokos_neve", nullable = false, length = 100)
    private String birtokosNeve;

    @Enumerated(EnumType.STRING)
    @Column(name = "kartya_tarsasag", nullable = false, length = 20)
    private KartyaTarsasag kartyaTarsasag;

    @Pattern(regexp = "[0-9]{4}")
    @Column(name = "utolso_negy_szamjegy", nullable = false, length = 4)
    private String utolsoNegySzamjegy;

    @Min(1)
    @Max(12)
    @Column(name = "lejarati_honap", nullable = false)
    private int lejaratiHonap;

    @Min(2000)
    @Column(name = "lejarati_ev", nullable = false)
    private int lejaratiEv;

    @Column(nullable = false)
    private boolean aktiv = true;
}
