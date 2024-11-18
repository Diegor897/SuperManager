package es.ucm.fdi.iw.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

import lombok.Data;

@NamedQueries({
    @NamedQuery(name="Equipo.todosEquipos",
    query="SELECT e FROM Equipo e"),
    @NamedQuery(name="Equipo.misEquipos",
    query="SELECT e FROM Equipo e "
            + "WHERE e.owner = :owner"),
    @NamedQuery(name="Equipo.bynombreequipo",
    query="SELECT e FROM Equipo e "
            + "WHERE e.teamname = :nombreequipo"),
    @NamedQuery(name="Equipo.miEquipoId",
    query="SELECT e FROM Equipo e "
            + "WHERE e.id = :idEquipo"),
    @NamedQuery(name="Equipo.miEquipo",
    query="SELECT e FROM Equipo e "
            + "WHERE e.id = :idEquipo AND e.owner = :owner"),
    @NamedQuery(name="Equipo.byliga",
    query="SELECT e FROM Equipo e "
            + "WHERE e.liga = :liga")
})
@Entity
@Data
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
    @SequenceGenerator(name = "gen", sequenceName = "gen")
	private long id;
    
    @Column(nullable = false, unique = true)
    private String teamname;

    @ManyToOne
    private User owner;

    @OneToOne
    private Liga liga;

    private int puntos;
    private double dinero;
    
    @ManyToMany
    @JoinColumn(name = "nombre_jugador")
    private List<JugadorACB> jugadores = new ArrayList<>(); // lista de los 10 jugadores del equipo del user
}
