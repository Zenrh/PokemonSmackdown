package com.pokemon.battle.model;

import java.util.List;

/** Immutable snapshot; version detects edits made from another window/process. */
public record SavedTeam(long id, String name, int version, List<Integer> pokemonIds) {
    public SavedTeam { pokemonIds = List.copyOf(pokemonIds); }
    @Override public String toString() { return name; }
}
