package com.pokemon.battle.dao;

import com.pokemon.battle.model.SavedTeam;
import java.util.List;

public interface TeamDao {
    List<SavedTeam> findAll(String nameFilter);
    SavedTeam findById(long id);
    SavedTeam create(String name, List<Integer> ids);
    SavedTeam update(long id, int version, String name, List<Integer> ids);
    void delete(long id, int version);
}
