package br.com.henry.selective.gupy.ame.starwars;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SwapiPlanet extends Planet {

    private List<String> films;

}
