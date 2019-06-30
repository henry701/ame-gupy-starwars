package br.com.henry.selective.gupy.ame.starwars.model;

import lombok.Data;

import javax.persistence.*;

@MappedSuperclass
@Data
public class Planet {

    @Column(name = "id", insertable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(name = "climate")
    private String climate;

    @Column(name = "terrain")
    private String terrain;

}
