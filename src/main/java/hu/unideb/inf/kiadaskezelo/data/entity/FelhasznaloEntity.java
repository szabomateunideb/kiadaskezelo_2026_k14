package hu.unideb.inf.kiadaskezelo.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "FELHASZNALO", uniqueConstraints = {
        @UniqueConstraint(name = "uk_felhasznalo_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_felhasznalo_felhasznalonev", columnNames = "felhasznalonev")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FelhasznaloEntity
        extends AuditEntity
        implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nev", nullable = false, length = 100)
    private String nev;
    @Column(name = "szulDat", nullable = false)
    private LocalDate szuletesiDatum;
    @Column(name = "felhasznalonev", nullable = false, length = 60)
    private String felhasznalonev;
    @Column(name = "jelszo", nullable = false, length = 255)
    private String jelszo;
    @Column(name = "email", nullable = false, length = 254)
    private String email;
    @Column(name = "nem", length = 10)
    private String nem;

    @Column(nullable = false)
    private boolean aktiv = true;

    @Column(name = "email_ellenorizve", nullable = false)
    private boolean emailEllenorizve;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jogosultsag_id")
    private JogEntity jog;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(jog);
    }

    @Override
    public @Nullable String getPassword() {
        return jelszo;
    }

    @Override
    public String getUsername() {
        return felhasznalonev;
    }
}
