
package com.pokemon.battle.team;

import com.pokemon.battle.model.Pokemon;
import java.util.ArrayList;
import java.util.List;
import com.pokemon.battle.exception.ValidationException;

public class PokemonTeam {

    private List<Pokemon> team;
    private int activeIndex;

    public PokemonTeam() {
        team = new ArrayList<>();
        activeIndex = 0;
    }

    public void addPokemon(Pokemon pokemon) {
        if(pokemon==null)throw new ValidationException("A Pokemon is required.");
        if(team.size()>=6)throw new ValidationException("A team can have at most six Pokemon.");
        if(team.stream().anyMatch(p -> p.getName().equalsIgnoreCase(pokemon.getName())))
            throw new ValidationException("A team cannot contain the same Pokemon twice.");
        team.add(pokemon);
    }

    public Pokemon getActivePokemon() {
        if (team.isEmpty()) {
            return null;
        }

        return team.get(activeIndex);
    }

    public boolean switchPokemon(int index) {
        if (index < 0 || index >= team.size()) {
            return false;
        }

        if (team.get(index).isFainted()) {
            return false;
        }

        activeIndex = index;
        return true;
    }

    public Pokemon getPokemon(int index) {
        if (index < 0 || index >= team.size()) {
            return null;
        }

        return team.get(index);
    }

    public int getTeamSize() {
        return team.size();
    }

    public boolean hasAvailablePokemon() {
        for (Pokemon pokemon : team) {
            if (!pokemon.isFainted()) {
                return true;
            }
        }

        return false;
    }

    public List<Pokemon> getTeam() {
        return List.copyOf(team);
    }
}
