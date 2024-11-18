package es.ucm.fdi.iw.model;

import javax.persistence.*;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Entity
@Data
@NamedQueries({
    @NamedQuery(name="Liga.allLigas",
            query="SELECT l FROM Liga l "),
    @NamedQuery(name="Liga.bynombreliga",
    query="SELECT l FROM Liga l "
            + "WHERE l.nombreLiga = :nombreliga"),
    @NamedQuery(name="Liga.byidliga",
    query="SELECT l FROM Liga l "
            + "WHERE l.id = :idLiga"),
    @NamedQuery(name="Liga.mensajes",
    query="SELECT l.received FROM Liga l "
            + "WHERE l.id = :idLiga")
})
@Table(name="Liga")
public class Liga {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
    @SequenceGenerator(name = "gen", sequenceName = "gen")
	private long id;
    
    @Column(nullable = false, unique = true)
    private String nombreLiga;

    private String contrasena;

    @ManyToOne
    private User creador;

    @OneToMany
    @JoinColumn(name="mensajes")
    private List<Message> received = new ArrayList<>();
    
}
