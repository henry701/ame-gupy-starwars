package br.com.henry.selective.gupy.ame.starwars.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "Planets")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SwapiPlanet extends Planet {

    @Column(name = "films")
    private Integer films;

}
