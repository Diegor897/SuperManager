package es.ucm.fdi.iw.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

/**
 * Puntuacion de un equipo de una liga en una determianda jornada.
 */
@Entity
@Data
@NamedQueries({
    @NamedQuery(name="PuntosEquipo.equipo",
    query="SELECT e FROM PuntosEquipo e "
            + "WHERE e.equipo = :equipo AND e.jornada = :jornada"),
    @NamedQuery(
    name = "PuntosEquipo.ultimasJornadas",
    query = "SELECT e.puntos FROM PuntosEquipo e " +
            "WHERE e.jornada < :jornada AND e.equipo = :equipo " +
            "ORDER BY ABS(e.jornada - :jornada)"),
    @NamedQuery(name="PuntosEquipo.deletePuntosEquipo",
    query="DELETE FROM PuntosEquipo")
})
@NoArgsConstructor
@Table(name="PuntosEquipo")
public class PuntosEquipo {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
    @SequenceGenerator(name = "gen", sequenceName = "gen")
	private long id;
    
    @OneToOne
    private Equipo equipo;

    private int jornada;

    @Column(nullable = false)
    private int puntos;

    @ManyToMany
    @JoinColumn(name = "nombre_jugador")
    private List<PuntosJugador> jugadores = new ArrayList<>(); // lista de los 10 jugadores del equipo del user
    
}
