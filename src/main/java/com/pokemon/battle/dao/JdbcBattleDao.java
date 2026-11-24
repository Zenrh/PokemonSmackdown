package com.pokemon.battle.dao;

import com.pokemon.battle.database.*;
import com.pokemon.battle.exception.ConflictException;
import com.pokemon.battle.model.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public final class JdbcBattleDao implements BattleDao {
    private final ConnectionFactory connections;
    public JdbcBattleDao(ConnectionFactory connections) { this.connections = connections; }

    @Override public void save(BattleRecord battle) {
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement s = c.prepareStatement("INSERT INTO ps_battle (battle_key, team_label, outcome, player_turns, started_at, ended_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                    s.setQueryTimeout(10); s.setString(1, battle.key()); s.setString(2, battle.teamLabel());
                    s.setString(3, battle.outcome().name()); s.setInt(4, battle.playerTurns());
                    s.setTimestamp(5, Timestamp.valueOf(battle.startedAt())); s.setTimestamp(6, Timestamp.valueOf(battle.endedAt()));
                    SqlErrors.requireOne(s.executeUpdate());
                }
                insertMembers(c, battle.key(), "P", battle.playerIds());
                insertMembers(c, battle.key(), "E", battle.enemyIds());
                c.commit();
            } catch (SQLException | RuntimeException ex) {
                SqlErrors.rollback(c, ex);
                // If the previous response was lost after commit, retrying this battle is safe.
                if (ex instanceof SQLException sql && (sql.getErrorCode() == 1 || "23505".equals(sql.getSQLState()))) {
                    BattleRecord stored = readByKey(c, battle.key());
                    if (battle.equals(stored)) return;
                    throw new ConflictException("A different battle result already uses this ID. Refresh the battle report.");
                }
                throw ex;
            }
        } catch (SQLException ex) { throw SqlErrors.translate(ex); }
    }

    private void insertMembers(Connection c, String key, String side, List<Integer> ids) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO ps_battle_member (battle_key, side_code, slot_no, pokemon_id) VALUES (?, ?, ?, ?)")) {
            s.setQueryTimeout(10);
            for (int i = 0; i < ids.size(); i++) {
                s.setString(1, key); s.setString(2, side); s.setInt(3, i + 1); s.setInt(4, ids.get(i));
                SqlErrors.requireOne(s.executeUpdate());
            }
        }
    }

    @Override public List<BattleRecord> find(LocalDate from, LocalDate to, BattleOutcome outcome) {
        String sql = "SELECT battle_key, team_label, outcome, player_turns, started_at, ended_at FROM ps_battle WHERE ended_at>=? AND ended_at<?"
                + (outcome == null ? "" : " AND outcome=?") + " ORDER BY ended_at DESC, battle_key";
        try (Connection c = connections.open(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setQueryTimeout(10); s.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            s.setTimestamp(2, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            if (outcome != null) s.setString(3, outcome.name());
            List<BattleRecord> rows = new ArrayList<>();
            // Battles are append-only, and each header and member snapshot is committed atomically.
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) rows.add(header(r, List.of(), List.of()));
            }
            List<BattleRecord> complete = new ArrayList<>();
            for (BattleRecord row : rows) complete.add(withMembers(c, row));
            return List.copyOf(complete);
        } catch (SQLException ex) { throw SqlErrors.translate(ex); }
    }

    private BattleRecord readByKey(Connection c, String key) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT battle_key, team_label, outcome, player_turns, started_at, ended_at FROM ps_battle WHERE battle_key=?")) {
            s.setQueryTimeout(10); s.setString(1, key);
            try (ResultSet r = s.executeQuery()) { return r.next() ? withMembers(c, header(r, List.of(), List.of())) : null; }
        }
    }

    private BattleRecord withMembers(Connection c, BattleRecord b) throws SQLException {
        List<Integer> player = new ArrayList<>(), enemy = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement("SELECT side_code, pokemon_id FROM ps_battle_member WHERE battle_key=? ORDER BY side_code, slot_no")) {
            s.setQueryTimeout(10); s.setString(1, b.key());
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) ("P".equals(r.getString(1)) ? player : enemy).add(r.getInt(2));
            }
        }
        return new BattleRecord(b.key(), b.teamLabel(), b.outcome(), b.playerTurns(), b.startedAt(), b.endedAt(), player, enemy);
    }

    private BattleRecord header(ResultSet r, List<Integer> p, List<Integer> e) throws SQLException {
        return new BattleRecord(r.getString(1), r.getString(2), BattleOutcome.valueOf(r.getString(3)), r.getInt(4),
                r.getTimestamp(5).toLocalDateTime(), r.getTimestamp(6).toLocalDateTime(), p, e);
    }
}
