package hu.unideb.inf.kiadaskezelo.data.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "FELHASZNALO")
public class FelhasznaloEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nev", nullable = false)
    private String nev;
    @Column(name = "szulDat", nullable = false)
    private LocalDate szuletesiDatum;
    @Column(name = "felhasznalonev", nullable = false, length = 60, unique = true)
    private String felhasznalonev;
    @Column(name = "jelszo", nullable = false)
    private String jelszo;
    @Column(name = "email",  nullable = false, unique = true)
    private String email;
    @Column(name = "nem")
    private String nem;

}
