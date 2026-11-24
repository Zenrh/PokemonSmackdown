package com.pokemon.battle.report;

import com.pokemon.battle.model.*;
import java.util.List;

public record BattleReport(List<BattleRecord> battles, long wins, long losses, long fled) {
    public BattleReport { battles = List.copyOf(battles); }
    public double winPercent() { return wins + losses == 0 ? 0 : wins * 100.0 / (wins + losses); }
}
