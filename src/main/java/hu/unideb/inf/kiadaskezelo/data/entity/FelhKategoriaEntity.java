package hu.unideb.inf.kiadaskezelo.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "FELH_KATEGORIA")
@Getter
@Setter
@NoArgsConstructor
public class FelhKategoriaEntity extends AuditEntity {
    @EmbeddedId
    private FelhKategoriaId id;

    @MapsId("felhId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "felh_id", nullable = false)
    private FelhasznaloEntity felhasznalo;

    @MapsId("katId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kat_id", nullable = false)
    private KategoriaEntity kategoria;

    @Column(name = "sajat_elnevezes", length = 100)
    private String sajatElnevezes;

    @Column(nullable = false)
    private boolean aktiv = true;

    public FelhKategoriaEntity(FelhasznaloEntity felhasznalo, KategoriaEntity kategoria) {
        this.felhasznalo = felhasznalo;
        this.kategoria = kategoria;
        this.id = new FelhKategoriaId(felhasznalo.getId(), kategoria.getId());
    }
}
