package com.pokemon.battle.service;

import com.pokemon.battle.dao.*;
import com.pokemon.battle.database.*;

public final class AppServices {
    private AppServices() {}
    private static final ConnectionFactory CONNECTIONS = new OracleConnectionFactory();
    public static final DatabaseService DATABASE = new DatabaseService();
    public static final TeamService TEAMS = new TeamService(new JdbcTeamDao(CONNECTIONS));
    public static final BattleHistoryService HISTORY = new BattleHistoryService(new JdbcBattleDao(CONNECTIONS));
}
