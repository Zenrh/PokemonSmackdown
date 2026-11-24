package com.pokemon.battle.service;

import com.pokemon.battle.database.*;
import com.pokemon.battle.exception.DataAccessException;
import java.sql.*;

/** Database setup and health checks are kept outside Swing listeners. */
public final class DatabaseService {
    public void testConnection(DatabaseConfig config) {
        try(Connection c=new OracleConnectionFactory(config).open();PreparedStatement s=c.prepareStatement("SELECT 1 FROM dual")) {
            s.setQueryTimeout(5);
            try(ResultSet r=s.executeQuery()) {
                if(!r.next() || r.getInt(1)!=1)throw new DataAccessException("Oracle did not return a valid connection test.");
            }
        } catch(SQLException ex) { throw SqlErrors.translate(ex); }
    }
    public void initialize(DatabaseConfig config) { SchemaInitializer.initialize(new OracleConnectionFactory(config)); }
}
