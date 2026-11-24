
package com.pokemon.battle.engine;

import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Move;
import com.pokemon.battle.model.DamageCalculator;
import com.pokemon.battle.model.TypeChart;
import com.pokemon.battle.team.PokemonTeam;
import java.util.Random;

public class BattleEngine {

    private PokemonTeam playerTeam;
    private PokemonTeam enemyTeam;
    private Random random = new Random();

    public BattleEngine(PokemonTeam playerTeam,
                        PokemonTeam enemyTeam) {
        if(playerTeam==null || enemyTeam==null || !playerTeam.hasAvailablePokemon() || !enemyTeam.hasAvailablePokemon())
            throw new com.pokemon.battle.exception.ValidationException("Both teams need at least one healthy Pokemon.");
        this.playerTeam = playerTeam;
        this.enemyTeam = enemyTeam;
    }

    public String playerAttack(int moveIndex) {
        return attack(playerTeam, enemyTeam, moveIndex);
    }

    public String enemyAttack(int moveIndex) {
        return attack(enemyTeam, playerTeam, moveIndex);
    }

    private String attack(PokemonTeam attackerTeam,
                          PokemonTeam defenderTeam,
                          int moveIndex) {

        Pokemon attacker = attackerTeam.getActivePokemon();
        Pokemon defender = defenderTeam.getActivePokemon();

        if (attacker == null || defender == null) {
            return "No active Pokemon!";
        }

        if (attacker.isFainted()) {
            return attacker.getName() + " has fainted!";
        }

        if (defender.isFainted()) {
            return defender.getName() + " has fainted!";
        }

        boolean fallback = moveIndex == -1 && attacker.getMoves().stream().noneMatch(m -> m.getCurrentPP() > 0);
        if (!fallback && (moveIndex < 0 ||
            moveIndex >= attacker.getMoves().size())) {
            return "Invalid move!";
        }

        Move move = fallback ? new Move("Struggle", "Normal", 50, 100, 1) : attacker.getMoves().get(moveIndex);

        if (!move.useMove()) {
            return "No PP remaining for " + move.getName() + "!";
        }

        String message = attacker.getName()
                + " used " + move.getName() + "!";

        if (random.nextInt(100) >= move.getAccuracy()) {
            return message + "\nBut the attack missed!";
        }

        double effectiveness = TypeChart.getEffectiveness(
                move.getType(), defender.getType()
        );

        int damage = DamageCalculator.calculateDamage(
                attacker, defender, move
        );

        if (effectiveness == 0) {
            damage = 0;
        }

        defender.takeDamage(damage);

        message += "\nDealt " + damage + " damage!";

        message += "\n" +
                DamageCalculator.getEffectivenessMessage(
                        defender, move
                );

        if (defender.isFainted()) {
            message += "\n" + defender.getName() + " fainted!";
        }

        return message;
    }

    public boolean switchPlayerPokemon(int index) {
        return playerTeam.switchPokemon(index);
    }

    public boolean switchEnemyPokemon(int index) {
        return enemyTeam.switchPokemon(index);
    }

    public Pokemon getPlayerPokemon() {
        return playerTeam.getActivePokemon();
    }

    public Pokemon getEnemyPokemon() {
        return enemyTeam.getActivePokemon();
    }

    public PokemonTeam getPlayerTeam() {
        return playerTeam;
    }

    public PokemonTeam getEnemyTeam() {
        return enemyTeam;
    }

    public boolean isBattleOver() {
        return !playerTeam.hasAvailablePokemon()
                || !enemyTeam.hasAvailablePokemon();
    }
}
