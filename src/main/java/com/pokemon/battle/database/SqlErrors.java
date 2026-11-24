package com.pokemon.battle.database;

import com.pokemon.battle.exception.*;
import java.sql.SQLException;
import java.util.logging.*;

public final class SqlErrors {
    private SqlErrors() {}
    public static AppException translate(SQLException ex) {
        Logger.getLogger(SqlErrors.class.getName()).log(Level.WARNING, "Database operation failed. SQL state {0}; code {1}", new Object[]{ex.getSQLState(), ex.getErrorCode()});
        if (ex.getErrorCode() == 1 || "23505".equals(ex.getSQLState())) return new ConflictException("A team with that name already exists. Choose another name or refresh the saved teams.");
        if (ex.getErrorCode() == 942) return new DataAccessException("Game tables were not found. Run db/schema.sql in SQL Developer using this same database user.", ex);
        if (ex.getErrorCode() == 1017 || "28000".equals(ex.getSQLState())) return new DataAccessException("Oracle rejected the username or password. Check Database settings.", ex);
        if(ex.getErrorCode()==1031)return new DataAccessException("This Oracle user needs permission to create the game tables. Ask your database administrator to run db/schema.sql or grant the required permissions.",ex);
        return new DataAccessException("Could not confirm the database operation. Check the Oracle service, connection settings, and schema, then refresh before retrying.", ex);
    }
    public static void requireOne(int affected) {
        if (affected != 1) throw new ConflictException("This record changed or was removed. Refresh before trying again.");
    }
    public static void rollback(java.sql.Connection c, Throwable original) {
        try { c.rollback(); } catch (SQLException rollbackFailure) { original.addSuppressed(rollbackFailure); }
    }
}
