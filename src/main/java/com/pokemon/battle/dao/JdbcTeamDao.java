package com.pokemon.battle.dao;

import com.pokemon.battle.database.*;
import com.pokemon.battle.exception.*;
import com.pokemon.battle.model.SavedTeam;
import java.sql.*;
import java.util.*;

public final class JdbcTeamDao implements TeamDao {
    private final ConnectionFactory connections;
    public JdbcTeamDao(ConnectionFactory connections) { this.connections = connections; }

    @Override public SavedTeam findById(long id) {
        String sql="SELECT t.name,t.row_version,m.pokemon_id FROM ps_team t JOIN ps_team_member m ON m.team_id=t.id WHERE t.id=? ORDER BY m.slot_no";
        try(Connection c=connections.open();PreparedStatement s=c.prepareStatement(sql)) {
            s.setQueryTimeout(10);s.setLong(1,id);
            try(ResultSet r=s.executeQuery()) {
                if(!r.next())throw new NotFoundException("This saved team no longer exists. Refresh the list.");
                String name=r.getString(1);int version=r.getInt(2);List<Integer> ids=new ArrayList<>();
                do { ids.add(r.getInt(3)); } while(r.next());
                return new SavedTeam(id,name,version,ids);
            }
        } catch(SQLException ex) { throw SqlErrors.translate(ex); }
    }

    @Override public List<SavedTeam> findAll(String filter) {
        // INSTR treats quotes, percent signs and underscores as literal search characters.
        String sql = "SELECT t.id, t.name, t.row_version, m.pokemon_id FROM ps_team t "
                + "JOIN ps_team_member m ON m.team_id=t.id WHERE INSTR(t.name_key, ?) > 0 "
                + "ORDER BY t.name_key, t.id, m.slot_no";
        // Oracle treats empty strings as NULL; a single space-free search is handled without a WHERE clause.
        boolean all = filter.isEmpty();
        if (all) sql = sql.replace(" WHERE INSTR(t.name_key, ?) > 0", "");
        try (Connection c = connections.open(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setQueryTimeout(10);
            if (!all) s.setString(1, filter.toLowerCase(Locale.ROOT));
            Map<Long, SavedTeam> headers = new LinkedHashMap<>();
            Map<Long, List<Integer>> members = new LinkedHashMap<>();
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    long id = r.getLong(1);
                    headers.putIfAbsent(id, new SavedTeam(id, r.getString(2), r.getInt(3), List.of()));
                    members.computeIfAbsent(id, unused -> new ArrayList<>()).add(r.getInt(4));
                }
            }
            return headers.values().stream().map(t -> new SavedTeam(t.id(), t.name(), t.version(), members.get(t.id()))).toList();
        } catch (SQLException ex) { throw SqlErrors.translate(ex); }
    }

    @Override public SavedTeam create(String name, List<Integer> ids) {
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement s = c.prepareStatement("INSERT INTO ps_team (name, name_key) VALUES (?, ?)", new String[]{"ID"})) {
                    s.setQueryTimeout(10); s.setString(1, name); s.setString(2, name.toLowerCase(Locale.ROOT));
                    SqlErrors.requireOne(s.executeUpdate());
                    try (ResultSet keys = s.getGeneratedKeys()) {
                        if (!keys.next()) throw new DataAccessException("Oracle did not return the new team ID.");
                        id = keys.getLong(1);
                    }
                }
                insertMembers(c, id, ids);
                c.commit();
                return new SavedTeam(id, name, 0, ids);
            } catch (SQLException | RuntimeException ex) { SqlErrors.rollback(c, ex); throw ex; }
        } catch (SQLException ex) { throw SqlErrors.translate(ex); }
    }

    @Override public SavedTeam update(long id, int version, String name, List<Integer> ids) {
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement s = c.prepareStatement("UPDATE ps_team SET name=?, name_key=?, row_version=row_version+1 WHERE id=? AND row_version=?")) {
                    s.setQueryTimeout(10); s.setString(1, name); s.setString(2, name.toLowerCase(Locale.ROOT)); s.setLong(3, id); s.setInt(4, version);
                    SqlErrors.requireOne(s.executeUpdate());
                }
                try (PreparedStatement s = c.prepareStatement("DELETE FROM ps_team_member WHERE team_id=?")) {
                    s.setQueryTimeout(10); s.setLong(1, id); s.executeUpdate();
                }
                insertMembers(c, id, ids);
                c.commit();
                return new SavedTeam(id, name, version + 1, ids);
            } catch (SQLException | RuntimeException ex) { SqlErrors.rollback(c, ex); throw ex; }
        } catch (SQLException ex) { throw SqlErrors.translate(ex); }
    }

    @Override public void delete(long id, int version) {
        try (Connection c = connections.open(); PreparedStatement s = c.prepareStatement("DELETE FROM ps_team WHERE id=? AND row_version=?")) {
            s.setQueryTimeout(10); s.setLong(1, id); s.setInt(2, version);
            SqlErrors.requireOne(s.executeUpdate()); // FK cascade removes the members in the same statement.
        } catch (SQLException ex) { throw SqlErrors.translate(ex); }
    }

    private void insertMembers(Connection c, long id, List<Integer> ids) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("INSERT INTO ps_team_member (team_id, slot_no, pokemon_id) VALUES (?, ?, ?)")) {
            s.setQueryTimeout(10);
            for (int i = 0; i < ids.size(); i++) {
                s.setLong(1, id); s.setInt(2, i + 1); s.setInt(3, ids.get(i));
                SqlErrors.requireOne(s.executeUpdate());
            }
        }
    }
}
