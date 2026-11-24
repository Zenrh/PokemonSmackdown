package com.pokemon.battle.service;

import com.pokemon.battle.dao.BattleDao;
import com.pokemon.battle.exception.ValidationException;
import com.pokemon.battle.model.*;
import com.pokemon.battle.report.BattleReport;
import java.time.LocalDate;
import java.util.*;

public final class BattleHistoryService {
    private final BattleDao battles;
    public BattleHistoryService(BattleDao battles) { this.battles = Objects.requireNonNull(battles); }
    public void save(BattleRecord b) {
        if (b == null) throw new ValidationException("No completed battle is available to save.");
        try { UUID.fromString(b.key()); } catch (IllegalArgumentException | NullPointerException ex) { throw new ValidationException("Invalid battle ID."); }
        TeamService.validName(b.teamLabel()); TeamService.validMembers(b.playerIds()); TeamService.validMembers(b.enemyIds());
        if (b.outcome() == null || b.playerTurns() < 0 || b.startedAt() == null || b.endedAt() == null || b.endedAt().isBefore(b.startedAt()))
            throw new ValidationException("Battle outcome, turn count, or timestamps are invalid.");
        battles.save(b);
    }
    public BattleReport report(LocalDate from, LocalDate to, BattleOutcome outcome) {
        if (from == null || to == null || from.isAfter(to) || to.getYear() > 9998 || from.getYear() < 1900)
            throw new ValidationException("Enter a valid date range from 1900 through 9998, with From no later than To.");
        List<BattleRecord> rows = battles.find(from, to, outcome);
        return new BattleReport(rows, count(rows, BattleOutcome.WIN), count(rows, BattleOutcome.LOSS), count(rows, BattleOutcome.FLED));
    }
    private long count(List<BattleRecord> rows, BattleOutcome outcome) { return rows.stream().filter(b -> b.outcome() == outcome).count(); }
}
