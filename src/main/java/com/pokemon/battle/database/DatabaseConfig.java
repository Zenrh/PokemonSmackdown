package com.pokemon.battle.database;

import com.pokemon.battle.exception.DataAccessException;
import com.pokemon.battle.exception.ValidationException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;

/** Credentials stay outside the JAR and source control. Never log this object. */
public final class DatabaseConfig {
    private final String url, user, password;

    public DatabaseConfig(String url, String user, String password) {
        if (url == null || !url.trim().startsWith("jdbc:oracle:thin:@"))
            throw new ValidationException("Enter an Oracle Thin URL, for example jdbc:oracle:thin:@//localhost:1521/XEPDB1.");
        if (user == null || user.isBlank()) throw new ValidationException("Enter your Oracle database username.");
        if (password == null || password.isEmpty()) throw new ValidationException("Enter your Oracle database password.");
        this.url = url.trim(); this.user = user.trim(); this.password = password;
    }

    public static Path path() { return Path.of(System.getProperty("pokemon.db.config", "config/db.properties")); }

    public static Properties readSettings() {
        Properties properties = new Properties();
        if (Files.exists(path())) {
            try (Reader reader = Files.newBufferedReader(path(), StandardCharsets.UTF_8)) { properties.load(reader); }
            catch (IOException ex) { throw new DataAccessException("Cannot read database settings. Check access to " + path(), ex); }
        }
        return properties;
    }

    public static DatabaseConfig load() {
        Properties p = readSettings();
        return new DatabaseConfig(value("PS_DB_URL", p, "db.url"), value("PS_DB_USER", p, "db.user"), value("PS_DB_PASSWORD", p, "db.password"));
    }

    private static String value(String env, Properties p, String key) {
        String override = System.getenv(env);
        return override == null ? p.getProperty(key, "") : override;
    }

    public void save() {
        Path temporary = null;
        try {
            Path target = path().toAbsolutePath();
            Files.createDirectories(target.getParent());
            temporary = Files.createTempFile(target.getParent(), "db-", ".tmp");
            Properties p = new Properties();
            p.setProperty("db.url", url); p.setProperty("db.user", user); p.setProperty("db.password", password);
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) { p.store(writer, "Local Oracle settings - do not share or commit"); }
            try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException ex) { throw new DataAccessException("Could not save the local database settings.", ex); }
        finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); }
            catch (IOException ex) { java.util.logging.Logger.getLogger(DatabaseConfig.class.getName()).warning("Could not remove temporary settings file."); }
        }
    }

    public String url() { return url; }
    public String user() { return user; }
    public Properties connectionProperties() {
        Properties p = new Properties(); p.setProperty("user", user); p.setProperty("password", password);
        p.setProperty("oracle.net.CONNECT_TIMEOUT", "5000");
        p.setProperty("oracle.jdbc.ReadTimeout", "10000");
        return p;
    }
}
