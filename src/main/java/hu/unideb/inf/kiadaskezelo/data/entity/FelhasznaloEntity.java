package hu.unideb.inf.kiadaskezelo.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "FELHASZNALO", uniqueConstraints = {
        @UniqueConstraint(name = "uk_felhasznalo_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_felhasznalo_felhasznalonev", columnNames = "felhasznalonev")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FelhasznaloEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nev", nullable = false, length = 100)
    private String nev;
    @Column(name = "szulDat", nullable = false)
    private LocalDate szuletesiDatum;
    @Column(name = "felhasznalonev", nullable = false, length = 60)
    private String felhasznalonev;
    @Column(name = "jelszo", nullable = false, length = 100)
    private String jelszo;
    @Column(name = "email", nullable = false, length = 254)
    private String email;
    @Column(name = "nem", length = 10)
    private String nem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jogosultsag_id")
    private JogEntity jog;

}
