package com.pokemon.battle.service;

import com.pokemon.battle.dao.TeamDao;
import com.pokemon.battle.exception.ValidationException;
import com.pokemon.battle.model.SavedTeam;
import java.util.*;

public final class TeamService {
    private final TeamDao teams;
    public TeamService(TeamDao teams) { this.teams = Objects.requireNonNull(teams); }

    public static String validName(String value) {
        if (value == null || value.isBlank()) throw new ValidationException("Enter a team name.");
        String name = value.strip();
        if (name.length() > 80) throw new ValidationException("Team names can contain up to 80 characters.");
        if (name.codePoints().anyMatch(Character::isISOControl)) throw new ValidationException("A team name cannot contain line breaks or control characters.");
        return name;
    }

    public static List<Integer> validMembers(List<Integer> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > 6) throw new ValidationException("Choose between 1 and 6 Pokemon.");
        Set<Integer> unique = new HashSet<>();
        for (Integer id : ids) {
            if (id == null || id < 1 || id > 151) throw new ValidationException("Pokemon IDs must be between 1 and 151.");
            if (!unique.add(id)) throw new ValidationException("A team cannot contain the same Pokemon twice.");
        }
        return List.copyOf(ids);
    }

    public List<SavedTeam> search(String filter) {
        String term = filter == null ? "" : filter.strip();
        if (term.length() > 80) throw new ValidationException("Search can contain up to 80 characters.");
        return teams.findAll(term);
    }
    public SavedTeam create(String name, List<Integer> ids) { return teams.create(validName(name), validMembers(ids)); }
    public SavedTeam load(long id) {
        if(id<=0)throw new ValidationException("Select a saved team first.");
        SavedTeam team=teams.findById(id);validMembers(team.pokemonIds());return team;
    }
    public SavedTeam update(SavedTeam original, String name, List<Integer> ids) {
        validateExisting(original);
        return teams.update(original.id(), original.version(), validName(name), validMembers(ids));
    }
    public void delete(SavedTeam original) { validateExisting(original); teams.delete(original.id(), original.version()); }
    private void validateExisting(SavedTeam team) {
        if (team == null || team.id() <= 0 || team.version() < 0) throw new ValidationException("Select a saved team first.");
    }
}
