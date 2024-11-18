package es.ucm.fdi.iw.model;

import javax.persistence.*;

import lombok.Data;

@Entity
@Data
@NamedQueries({
    @NamedQuery(name="JugadorACB.jugadores",
    query="SELECT j FROM JugadorACB j"),
    @NamedQuery(name="JugadorACB.jugadorId",
    query="SELECT j FROM JugadorACB j "
        + "WHERE j.id = :idJugador"),
    @NamedQuery(name="JugadorACB.jugador",
    query="SELECT j FROM JugadorACB j "
        + "WHERE j.nombre = :nombre"),
    @NamedQuery(name="JugadorACB.bases",
    query="SELECT j FROM JugadorACB j "
        + "WHERE j.posicion = 'Base'"),
    @NamedQuery(name="JugadorACB.aleros",
    query="SELECT j FROM JugadorACB j "
        + "WHERE j.posicion = 'Alero'"),
    @NamedQuery(name="JugadorACB.pivots",
    query="SELECT j FROM JugadorACB j "
        + "WHERE j.posicion = 'Pivot'"),
    @NamedQuery(name="JugadorACB.nombreSimilar",
    query="SELECT j FROM JugadorACB j "
        + "WHERE LOWER(j.nombre) LIKE LOWER(:nombreSimilar)"),
    @NamedQuery(name="JugadorACB.nombreSimilarBases",
    query="SELECT j FROM JugadorACB j "
        + "WHERE j.posicion = 'Base' AND LOWER(j.nombre) LIKE LOWER(:nombreSimilar)"),
    @NamedQuery(name="JugadorACB.nombreSimilarAleros",
    query="SELECT j FROM JugadorACB j "
        + "WHERE j.posicion = 'Alero' AND LOWER(j.nombre) LIKE LOWER(:nombreSimilar)"),
    @NamedQuery(name="JugadorACB.nombreSimilarPivots",
    query="SELECT j FROM JugadorACB j "
        + "WHERE j.posicion = 'Pivot' AND LOWER(j.nombre) LIKE LOWER(:nombreSimilar)")
})
@Table(name="JugadorACB")
public class JugadorACB {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
    @SequenceGenerator(name = "gen", sequenceName = "gen")
	private long id;
    
    @Column(nullable = false, unique = true)
    private String nombre;

    private String pais;

    private String equipo;

    @Column(nullable = false)
    private String posicion;

    private int partidosJugados;
    private int puntosTotales;
    private int valoracionTotal;
    private double valoracionMedia;
    private double valorMercado;

}