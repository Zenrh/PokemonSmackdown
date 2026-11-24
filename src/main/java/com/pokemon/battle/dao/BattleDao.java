package com.pokemon.battle.dao;

import com.pokemon.battle.model.*;
import java.time.LocalDate;
import java.util.List;

public interface BattleDao {
    /** A retry with the same key and identical result does not create a duplicate. */
    void save(BattleRecord battle);
    List<BattleRecord> find(LocalDate from, LocalDate to, BattleOutcome outcome);
}
