package com.pokemon.battle.model;

import java.time.LocalDateTime;
import java.util.List;

/** Times are stored and reported in UTC. Team members are historical snapshots. */
public record BattleRecord(String key, String teamLabel, BattleOutcome outcome, int playerTurns,
                           LocalDateTime startedAt, LocalDateTime endedAt,
                           List<Integer> playerIds, List<Integer> enemyIds) {
    public BattleRecord {
        playerIds = List.copyOf(playerIds); enemyIds = List.copyOf(enemyIds);
        // Oracle TIMESTAMP defaults to microsecond precision; normalize once for safe retries.
        if (startedAt != null) startedAt = startedAt.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        if (endedAt != null) endedAt = endedAt.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
}
