package es.ucm.fdi.iw.model;

import javax.persistence.*;

import lombok.Data;

@Entity
@Data
@NamedQueries({
    @NamedQuery(name="Jornada.getJornada",
            query="SELECT j FROM Jornada j ")
})
@Table(name="Jornada")
public class Jornada {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
    @SequenceGenerator(name = "gen", sequenceName = "gen")
	private long id;
    
    @Column(nullable = false, unique = true)
    private int jornada;
    
}
