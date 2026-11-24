package com.pokemon.battle.database;

import com.pokemon.battle.exception.DataAccessException;
import java.sql.*;

public final class OracleConnectionFactory implements ConnectionFactory {
    private final DatabaseConfig settings;
    public OracleConnectionFactory() { this.settings = null; }
    public OracleConnectionFactory(DatabaseConfig settings) { this.settings = settings; }

    @Override public Connection open() throws SQLException {
        try { Class.forName("oracle.jdbc.OracleDriver"); }
        catch (ClassNotFoundException ex) { throw new DataAccessException("Oracle JDBC driver is missing. Reload Maven dependencies and build the project.", ex); }
        DatabaseConfig config = settings == null ? DatabaseConfig.load() : settings;
        return DriverManager.getConnection(config.url(), config.connectionProperties());
    }
}
