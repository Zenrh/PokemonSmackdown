package com.pokemon.battle;

import com.pokemon.battle.dao.*;
import com.pokemon.battle.database.*;
import com.pokemon.battle.exception.*;
import com.pokemon.battle.model.*;
import com.pokemon.battle.service.*;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in only: uses the user's configured Oracle schema and removes only its own test records. */
@EnabledIfSystemProperty(named="oracle.integration", matches="true")
class OracleSmokeTest {
    @Test void oracleCrudRollbackHistoryAndRestart() throws Exception {
        ConnectionFactory connections=new OracleConnectionFactory();
        TeamService teams=new TeamService(new JdbcTeamDao(connections));
        BattleHistoryService history=new BattleHistoryService(new JdbcBattleDao(connections));
        String label="__oracle_test_"+UUID.randomUUID().toString().substring(0,12);
        SavedTeam saved=null;
        String battleKey=UUID.randomUUID().toString();
        boolean schemaReady=false;
        try {
            try(Connection c=connections.open()) { SchemaInitializer.verify(c); }
            schemaReady=true;
            saved=teams.create(label+"'s party",List.of(151,25));
            assertEquals(saved,teams.load(saved.id()));
            SavedTeam original=saved;
            assertThrows(ConflictException.class,() -> teams.create(original.name().toUpperCase(Locale.ROOT),List.of(1)));
            assertTrue(teams.search("' OR '1'='1").isEmpty());
            saved=teams.update(saved,label+" updated",List.of(1,4,7));
            assertThrows(ConflictException.class,() -> teams.update(original,"Stale",List.of(2)));
            SavedTeam expected=saved;
            assertThrows(AppException.class,() -> new JdbcTeamDao(connections).update(expected.id(),expected.version(),label+" broken",List.of(25,25)));
            assertEquals(expected,teams.load(saved.id()));
            LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
            BattleRecord record=new BattleRecord(battleKey,label,BattleOutcome.WIN,3,now.minusMinutes(1),now,List.of(1,4,7),List.of(25));
            history.save(record);history.save(record);
            var rows=history.report(now.toLocalDate(),now.toLocalDate(),BattleOutcome.WIN).battles();
            assertEquals(1,rows.stream().filter(b -> b.key().equals(battleKey)).count());
            String cp=System.getProperty("surefire.test.class.path",System.getProperty("java.class.path"));
            Process process=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),
                    "-Dpokemon.db.config="+DatabaseConfig.path().toAbsolutePath(),"-cp",cp,OracleProbe.class.getName(),Long.toString(saved.id())).redirectErrorStream(true).start();
            assertTrue(process.waitFor(20,TimeUnit.SECONDS));
            assertEquals(0,process.exitValue(),new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
            teams.delete(saved);assertThrows(NotFoundException.class,() -> teams.load(expected.id()));saved=null;
        } finally {
            if(schemaReady) {
                try(Connection c=connections.open();PreparedStatement s=c.prepareStatement("DELETE FROM ps_battle WHERE battle_key=?")) {
                    s.setString(1,battleKey);s.executeUpdate();
                }
                if(saved!=null) { SavedTeam current=teams.load(saved.id());teams.delete(current); }
            }
        }
    }
    public static class OracleProbe {
        public static void main(String[] args) {
            SavedTeam team=new TeamService(new JdbcTeamDao(new OracleConnectionFactory())).load(Long.parseLong(args[0]));
            if(!team.pokemonIds().equals(List.of(1,4,7)))throw new AssertionError("Oracle restart persistence failed");
            System.out.println("Oracle restart persistence verified.");
        }
    }
}
