package br.com.henry.selective.gupy.ame.starwars;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SwapiPlanet extends Planet {

    private int films;

}
