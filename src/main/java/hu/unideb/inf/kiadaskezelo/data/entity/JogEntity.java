package hu.unideb.inf.kiadaskezelo.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "JOGOSULTSAG")
public class JogEntity
        extends AuditEntity
        implements GrantedAuthority {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 100,unique = true,nullable = false)
    private String nev;
    @Column
    private String leiras;

    @Override
    public @Nullable String getAuthority() {
        return nev;
    }
}
