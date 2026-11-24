package com.pokemon.battle;

import com.pokemon.battle.database.DatabaseConfig;
import com.pokemon.battle.exception.ValidationException;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {
    @Test void acceptsOracleSid() { assertEquals("jdbc:oracle:thin:@localhost:1521:xe",new DatabaseConfig("jdbc:oracle:thin:@localhost:1521:xe","app","test-password").url()); }
    @Test void acceptsOracleServiceName() { assertEquals("app",new DatabaseConfig("jdbc:oracle:thin:@//localhost:1521/XEPDB1","app","test-password").user()); }
    @Test void embeddedCredentialsInUrlAreRejected() { assertThrows(ValidationException.class,() -> new DatabaseConfig("jdbc:oracle:thin:app/secret@localhost:1521:xe","app","test-password")); }
    @Test void missingUsernameRejected() { assertThrows(ValidationException.class,() -> new DatabaseConfig("jdbc:oracle:thin:@localhost:1521:xe"," ","test-password")); }
    @Test void missingPasswordRejected() { assertThrows(ValidationException.class,() -> new DatabaseConfig("jdbc:oracle:thin:@localhost:1521:xe","app","")); }
    @Test void settingsRoundTripLocally(@TempDir Path temp) throws Exception {
        String previous=System.getProperty("pokemon.db.config");
        try {
            System.setProperty("pokemon.db.config",temp.resolve("settings/db.properties").toString());
            new DatabaseConfig("jdbc:oracle:thin:@localhost:1521:xe","app","synthetic-test-password").save();
            var settings=DatabaseConfig.readSettings();assertEquals("app",settings.getProperty("db.user"));
            assertEquals("synthetic-test-password",settings.getProperty("db.password"));
            try(var files=Files.list(temp.resolve("settings"))) {
                assertTrue(files.allMatch(p -> p.getFileName().toString().equals("db.properties")));
            }
        } finally {
            if(previous==null)System.clearProperty("pokemon.db.config");else System.setProperty("pokemon.db.config",previous);
        }
    }
}
