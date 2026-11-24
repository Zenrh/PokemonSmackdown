package com.pokemon.battle.database;

import com.pokemon.battle.exception.DataAccessException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Map;

/** Explicit one-time setup. Oracle DDL commits independently; no existing data is dropped. */
public final class SchemaInitializer {
    private SchemaInitializer() {}
    public static void initialize(ConnectionFactory connections) {
        String ddl;
        try(InputStream in=SchemaInitializer.class.getResourceAsStream("/db/schema.sql")) {
            if(in==null)throw new DataAccessException("The schema resource is missing. Clean and build the project first.");
            ddl=new String(in.readAllBytes(),StandardCharsets.UTF_8).replaceAll("(?m)--[^\\r\\n]*","");
        } catch(IOException ex) { throw new DataAccessException("Could not read the bundled database schema.",ex); }
        try(Connection c=connections.open();Statement s=c.createStatement()) {
            s.setQueryTimeout(15);
            for(String sql:ddl.split(";")) {
                if(sql.isBlank())continue;
                try { s.execute(sql); }
                catch(SQLException ex) {
                    // ORA-00955 means an object already exists. Validate required columns below.
                    if(ex.getErrorCode()!=955)throw ex;
                }
            }
            verify(c);
        } catch(SQLException ex) { throw SqlErrors.translate(ex); }
    }
    public static void verify(Connection c) throws SQLException {
        Map<String,String> columns=Map.of(
                "ps_team","id,name,name_key,row_version",
                "ps_team_member","team_id,slot_no,pokemon_id",
                "ps_battle","battle_key,team_label,outcome,player_turns,started_at,ended_at",
                "ps_battle_member","battle_key,side_code,slot_no,pokemon_id");
        // All identifiers come from this fixed map, never a user input.
        for(var table:columns.entrySet())try(PreparedStatement s=c.prepareStatement("SELECT "+table.getValue()+" FROM "+table.getKey()+" WHERE 1=0")) {
            s.setQueryTimeout(5);
            try(ResultSet ignored=s.executeQuery()) { /* Verifies the expected game schema is accessible. */ }
        }
    }
}
