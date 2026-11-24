package com.pokemon.battle;

import com.pokemon.battle.dao.*;
import com.pokemon.battle.database.ConnectionFactory;
import com.pokemon.battle.exception.*;
import com.pokemon.battle.model.*;
import com.pokemon.battle.service.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Real JDBC tests using H2 Oracle mode, not a substitute for the Oracle smoke test. */
class DatabaseIntegrationTest {
    ConnectionFactory connections;
    TeamService teams;
    BattleHistoryService history;
    JdbcTeamDao teamDao;
    JdbcBattleDao battleDao;
    static final LocalDate DAY=LocalDate.of(2026,11,22);

    @BeforeEach void setup() throws Exception {
        String url="jdbc:h2:mem:"+UUID.randomUUID()+";MODE=Oracle;DB_CLOSE_DELAY=-1";
        connections=() -> DriverManager.getConnection(url,"sa","");
        createSchema(connections);
        teamDao=new JdbcTeamDao(connections); teams=new TeamService(teamDao);
        battleDao=new JdbcBattleDao(connections); history=new BattleHistoryService(battleDao);
    }
    static void createSchema(ConnectionFactory factory) throws Exception {
        String ddl=Files.readString(Path.of("db/schema.sql")).replaceAll("(?m)--[^\\r\\n]*","")
                .replaceAll("VARCHAR2\\((\\d+) CHAR\\)","VARCHAR2($1)");
        try(Connection c=factory.open(); Statement s=c.createStatement()) {
            for(String sql:ddl.split(";"))if(!sql.isBlank())s.execute(sql);
        }
    }
    long count(String table) throws Exception {
        if(!Set.of("ps_team","ps_team_member","ps_battle","ps_battle_member").contains(table))throw new IllegalArgumentException();
        try(Connection c=connections.open();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM "+table)) { r.next();return r.getLong(1); }
    }
    BattleRecord battle(BattleOutcome outcome,LocalDateTime end) {
        return new BattleRecord(UUID.randomUUID().toString(),"Test party",outcome,3,end.minusMinutes(5),end,List.of(1,4),List.of(7,25));
    }

    @Test void createAndReadPreservesOrder() {
        SavedTeam saved=teams.create("First party",List.of(25,1,151));
        assertTrue(saved.id()>0);assertEquals(0,saved.version());
        assertEquals(List.of(saved),teams.search(""));
    }
    @Test void loadReadsCurrentSavedVersion() {
        SavedTeam old=teams.create("Original",List.of(1));SavedTeam updated=teams.update(old,"Fresh",List.of(25));
        assertEquals(updated,teams.load(old.id()));
    }
    @Test void loadingDeletedTeamReportsNotFound() {
        SavedTeam t=teams.create("Gone",List.of(1));teams.delete(t);
        assertThrows(NotFoundException.class,() -> teams.load(t.id()));
    }
    @Test void schemaVerificationUsesAllFourTables() throws Exception {
        try(Connection c=connections.open()) { com.pokemon.battle.database.SchemaInitializer.verify(c); }
    }
    @Test void allSixSlotsPersist() { teams.create("Six",List.of(1,2,3,4,5,6));assertEquals(6,teams.search("").getFirst().pokemonIds().size()); }
    @Test void emptyPartyRejected() { assertThrows(ValidationException.class,() -> teams.create("Empty",List.of())); }
    @Test void seventhMemberRejected() { assertThrows(ValidationException.class,() -> teams.create("Seven",List.of(1,2,3,4,5,6,7))); }
    @Test void duplicateMemberRejected() { assertThrows(ValidationException.class,() -> teams.create("Duplicate",List.of(1,1))); }
    @Test void invalidPokemonIdRejected() { assertThrows(ValidationException.class,() -> teams.create("Invalid",List.of(152))); }
    @Test void nullPokemonIdRejected() { assertThrows(ValidationException.class,() -> teams.create("Invalid",Arrays.asList(1,null))); }
    @Test void blankNameRejected() { assertThrows(ValidationException.class,() -> teams.create("  ",List.of(1))); }
    @Test void longNameRejected() { assertThrows(ValidationException.class,() -> teams.create("x".repeat(81),List.of(1))); }
    @Test void controlCharacterRejected() { assertThrows(ValidationException.class,() -> teams.create("My\nTeam",List.of(1))); }
    @Test void duplicateNamesIgnoreCaseAndOuterSpaces() {
        teams.create("  Champions  ",List.of(1));
        assertThrows(ConflictException.class,() -> teams.create("champions",List.of(2)));
        assertEquals(1,teams.search("").size());
    }
    @Test void quotedNameRoundTrips() { SavedTeam t=teams.create("Ash's team",List.of(25));assertEquals(List.of(t),teams.search("Ash's")); }
    @Test void injectionTextIsLiteral() { teams.create("Normal",List.of(1));assertTrue(teams.search("' OR '1'='1").isEmpty()); }
    @Test void wildcardCharactersAreLiteral() { teams.create("100%_ready",List.of(1));teams.create("Other",List.of(2));assertEquals(1,teams.search("%_").size()); }
    @Test void searchIsCaseInsensitive() { teams.create("Electric",List.of(25));assertEquals(1,teams.search("LECTR").size()); }
    @Test void updateReplacesNameAndMembersAtomically() {
        SavedTeam old=teams.create("Before",List.of(1,2));
        SavedTeam changed=teams.update(old,"After",List.of(151,25));
        assertEquals(old.id(),changed.id());assertEquals(1,changed.version());assertEquals(List.of(changed),teams.search(""));
    }
    @Test void staleUpdateRejected() {
        SavedTeam old=teams.create("Original",List.of(1));teams.update(old,"Updated",List.of(2));
        assertThrows(ConflictException.class,() -> teams.update(old,"Stale",List.of(3)));
        assertEquals("Updated",teams.search("").getFirst().name());
    }
    @Test void staleDeleteRejected() {
        SavedTeam old=teams.create("Original",List.of(1));teams.update(old,"Updated",List.of(2));
        assertThrows(ConflictException.class,() -> teams.delete(old));assertEquals(1,teams.search("").size());
    }
    @Test void deletingTeamCascadesMembers() throws Exception {
        SavedTeam t=teams.create("Remove",List.of(1,2));teams.delete(t);assertEquals(0,count("ps_team"));assertEquals(0,count("ps_team_member"));
    }
    @Test void repeatedDeleteReportsConflict() {
        SavedTeam t=teams.create("Remove",List.of(1));teams.delete(t);assertThrows(ConflictException.class,() -> teams.delete(t));
    }
    @Test void failedMemberInsertRollsBackNewHeader() throws Exception {
        assertThrows(AppException.class,() -> teamDao.create("Broken",List.of(1,1)));
        assertEquals(0,count("ps_team"));assertEquals(0,count("ps_team_member"));
    }
    @Test void failedReplacementRestoresOldNameVersionAndMembers() {
        SavedTeam old=teams.create("Original",List.of(1,2));
        assertThrows(AppException.class,() -> teamDao.update(old.id(),old.version(),"Broken",List.of(25,25)));
        assertEquals(List.of(old),teams.search(""));
    }
    @Test void failedRenameDoesNotChangeMembers() {
        SavedTeam a=teams.create("A",List.of(1));teams.create("B",List.of(2));
        assertThrows(ConflictException.class,() -> teams.update(a,"B",List.of(3)));assertEquals(a,teams.search("A").getFirst());
    }
    @Test void invalidInputNeverOpensDatabase() {
        AtomicInteger attempts=new AtomicInteger();
        TeamService offline=new TeamService(new JdbcTeamDao(() -> {attempts.incrementAndGet();throw new SQLException("Outage","08006");}));
        assertThrows(ValidationException.class,() -> offline.create("",List.of()));assertEquals(0,attempts.get());
    }
    @Test void outageBecomesActionableApplicationFailure() {
        TeamService offline=new TeamService(new JdbcTeamDao(() -> {throw new SQLException("Outage","08006");}));
        DataAccessException ex=assertThrows(DataAccessException.class,() -> offline.search(""));assertTrue(ex.getMessage().contains("Oracle"));
    }
    @Test void historySavesBothTeamSnapshots() {
        BattleRecord b=battle(BattleOutcome.WIN,DAY.atTime(12,30));history.save(b);
        assertEquals(List.of(b),history.report(DAY,DAY,null).battles());
    }
    @Test void retryDoesNotDuplicateBattle() throws Exception {
        BattleRecord b=battle(BattleOutcome.WIN,DAY.atTime(12,30));history.save(b);history.save(b);
        assertEquals(1,count("ps_battle"));assertEquals(4,count("ps_battle_member"));
    }
    @Test void reusedKeyCannotOverwriteDifferentResult() {
        BattleRecord a=battle(BattleOutcome.WIN,DAY.atTime(12,30));history.save(a);
        BattleRecord b=new BattleRecord(a.key(),a.teamLabel(),BattleOutcome.LOSS,a.playerTurns(),a.startedAt(),a.endedAt(),a.playerIds(),a.enemyIds());
        assertThrows(ConflictException.class,() -> history.save(b));assertEquals(a,history.report(DAY,DAY,null).battles().getFirst());
    }
    @Test void failedBattleMembersRollBackHeader() throws Exception {
        BattleRecord b=new BattleRecord(UUID.randomUUID().toString(),"Invalid",BattleOutcome.WIN,1,DAY.atTime(1,0),DAY.atTime(1,1),List.of(1,1),List.of(2));
        assertThrows(AppException.class,() -> battleDao.save(b));assertEquals(0,count("ps_battle"));assertEquals(0,count("ps_battle_member"));
    }
    @Test void winRateExcludesFled() {
        history.save(battle(BattleOutcome.WIN,DAY.atTime(10,0)));history.save(battle(BattleOutcome.LOSS,DAY.atTime(11,0)));history.save(battle(BattleOutcome.FLED,DAY.atTime(12,0)));
        var report=history.report(DAY,DAY,null);assertEquals(1,report.wins());assertEquals(1,report.losses());assertEquals(1,report.fled());assertEquals(50.0,report.winPercent());
    }
    @Test void reportFiltersOutcome() {
        history.save(battle(BattleOutcome.WIN,DAY.atTime(10,0)));history.save(battle(BattleOutcome.LOSS,DAY.atTime(11,0)));
        assertEquals(1,history.report(DAY,DAY,BattleOutcome.WIN).battles().size());
    }
    @Test void reportIncludesEntireEndDateButNotNextDay() {
        history.save(battle(BattleOutcome.WIN,DAY.atTime(23,59,59,999999000)));history.save(battle(BattleOutcome.LOSS,DAY.plusDays(1).atStartOfDay()));
        assertEquals(1,history.report(DAY,DAY,null).wins());assertEquals(0,history.report(DAY,DAY,null).losses());
    }
    @Test void invalidDateRangeRejected() { assertThrows(ValidationException.class,() -> history.report(DAY.plusDays(1),DAY,null)); }
    @Test void battleHistorySurvivesDeletingSavedTeam() {
        SavedTeam t=teams.create("Party",List.of(1));history.save(battle(BattleOutcome.WIN,DAY.atTime(10,0)));teams.delete(t);
        assertEquals(1,history.report(DAY,DAY,null).battles().size());
    }
    @Test void invalidBattleTimesRejected() {
        BattleRecord b=new BattleRecord(UUID.randomUUID().toString(),"Test",BattleOutcome.WIN,1,DAY.atTime(12,0),DAY.atTime(11,0),List.of(1),List.of(2));
        assertThrows(ValidationException.class,() -> history.save(b));
    }
    @Test void oraclePrecisionNormalizedForRetry() {
        BattleRecord b=battle(BattleOutcome.WIN,DAY.atTime(12,30,0,123456789));history.save(b);history.save(b);
        assertEquals(123456000,history.report(DAY,DAY,null).battles().getFirst().endedAt().getNano());
    }
    @Test void savedMembersCannotBeMutated() { SavedTeam t=teams.create("Read only",List.of(1));assertThrows(UnsupportedOperationException.class,() -> t.pokemonIds().add(2)); }
    @Test void persistenceAcrossSeparateJvm(@TempDir Path dir) throws Exception {
        String url="jdbc:h2:file:"+dir.resolve("persist").toAbsolutePath().toString().replace('\\','/')+";MODE=Oracle";
        ConnectionFactory fileConnections=() -> DriverManager.getConnection(url,"sa","");createSchema(fileConnections);
        SavedTeam t=new TeamService(new JdbcTeamDao(fileConnections)).create("Persistent party",List.of(151,25));
        String javaExecutable=Path.of(System.getProperty("java.home"),"bin","java").toString();
        String cp=System.getProperty("surefire.test.class.path",System.getProperty("java.class.path"));
        Process process=new ProcessBuilder(javaExecutable,"-cp",cp,PersistenceProbe.class.getName(),url,Long.toString(t.id())).redirectErrorStream(true).start();
        assertTrue(process.waitFor(20,java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(0,process.exitValue(),new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
    }
    public static class PersistenceProbe {
        public static void main(String[] args) {
            TeamService reopened=new TeamService(new JdbcTeamDao(() -> DriverManager.getConnection(args[0],"sa","")));
            SavedTeam t=reopened.search("").getFirst();
            if(t.id()!=Long.parseLong(args[1]) || !t.pokemonIds().equals(List.of(151,25)))throw new AssertionError("Persistence failed");
        }
    }
}
