
package com.pokemon.battle.team;

import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Move;

public class TeamFactory {

    public static PokemonTeam createPlayerTeam() {

        PokemonTeam team = new PokemonTeam();

        Pokemon charizard = new Pokemon(
                "Charizard", "Fire", 150, 110, 90, 100
        );
        charizard.addMove(new Move("Flamethrower", "Fire", 90, 100, 15));
        charizard.addMove(new Move("Fire Blast", "Fire", 110, 85, 5));
        charizard.addMove(new Move("Slash", "Normal", 70, 100, 20));
        charizard.addMove(new Move("Quick Attack", "Normal", 40, 100, 30));

        Pokemon blastoise = new Pokemon(
                "Blastoise", "Water", 160, 95, 120, 78
        );
        blastoise.addMove(new Move("Water Pulse", "Water", 60, 100, 20));
        blastoise.addMove(new Move("Hydro Pump", "Water", 110, 80, 5));
        blastoise.addMove(new Move("Tackle", "Normal", 40, 100, 35));
        blastoise.addMove(new Move("Ice Beam", "Ice", 90, 100, 10));

        Pokemon venusaur = new Pokemon(
                "Venusaur", "Grass", 155, 100, 100, 80
        );
        venusaur.addMove(new Move("Razor Leaf", "Grass", 55, 95, 25));
        venusaur.addMove(new Move("Solar Beam", "Grass", 120, 100, 10));
        venusaur.addMove(new Move("Vine Whip", "Grass", 45, 100, 25));
        venusaur.addMove(new Move("Tackle", "Normal", 40, 100, 35));

        team.addPokemon(charizard);
        team.addPokemon(blastoise);
        team.addPokemon(venusaur);

        return team;
    }

    public static PokemonTeam createEnemyTeam() {

        PokemonTeam team = new PokemonTeam();

        Pokemon pikachu = new Pokemon(
                "Pikachu", "Electric", 110, 90, 60, 110
        );
        pikachu.addMove(new Move("Thunderbolt", "Electric", 90, 100, 15));
        pikachu.addMove(new Move("Thunder", "Electric", 110, 70, 10));
        pikachu.addMove(new Move("Quick Attack", "Normal", 40, 100, 30));
        pikachu.addMove(new Move("Iron Tail", "Steel", 100, 75, 15));

        Pokemon arcanine = new Pokemon(
                "Arcanine", "Fire", 150, 115, 85, 95
        );
        arcanine.addMove(new Move("Flamethrower", "Fire", 90, 100, 15));
        arcanine.addMove(new Move("Fire Fang", "Fire", 65, 95, 15));
        arcanine.addMove(new Move("Bite", "Dark", 60, 100, 25));
        arcanine.addMove(new Move("Quick Attack", "Normal", 40, 100, 30));

        Pokemon gyarados = new Pokemon(
                "Gyarados", "Water", 170, 125, 90, 81
        );
        gyarados.addMove(new Move("Waterfall", "Water", 80, 100, 15));
        gyarados.addMove(new Move("Hydro Pump", "Water", 110, 80, 5));
        gyarados.addMove(new Move("Bite", "Dark", 60, 100, 25));
        gyarados.addMove(new Move("Tackle", "Normal", 40, 100, 35));

        team.addPokemon(pikachu);
        team.addPokemon(arcanine);
        team.addPokemon(gyarados);

        return team;
    }
}
