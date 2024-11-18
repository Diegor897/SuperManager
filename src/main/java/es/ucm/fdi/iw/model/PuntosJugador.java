package es.ucm.fdi.iw.model;

import javax.persistence.*;

import lombok.Data;

@Entity
@Data
@NamedQueries({
    @NamedQuery(name="PuntosJugador.jugador",
    query="SELECT j FROM PuntosJugador j "
        + "WHERE j.nombre = :nombre AND j.jornada = :jornada"),
    @NamedQuery(
    name = "PuntosJugador.ultimosPartidos",
    query = "SELECT j.valoracion FROM PuntosJugador j " +
            "WHERE j.jornada < :jornada AND j.nombre = :nombre " +
            "ORDER BY ABS(j.jornada - :jornada)"
    )
})
@Table(name="PuntosJugador")
public class PuntosJugador {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
    @SequenceGenerator(name = "gen", sequenceName = "gen")
	private long id;
    
    // @Column(nullable = false, unique = true)
    private int jornada;

    // @OneToOne
    // @JoinColumn(name = "nombre", nullable = false, unique = true, referencedColumnName = "nombre") -- Antes el tipo era JugadorACB
    private String nombre;

    private String posicion;

    private int valoracion;
    private double valor_mercado;
    private int puntos;

}
