package hu.unideb.inf.kiadaskezelo.data.entity;

import hu.unideb.inf.kiadaskezelo.data.entity.enums.KategoriaTipus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "KATEGORIA", uniqueConstraints =
        @UniqueConstraint(name = "uk_kategoria_nev_tipus", columnNames = {"nev", "tipus"}))
@Getter
@Setter
@NoArgsConstructor
public class KategoriaEntity extends AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nev;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private KategoriaTipus tipus;

    @Column(length = 500)
    private String leiras;
}
