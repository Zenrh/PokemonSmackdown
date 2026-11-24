package com.pokemon.battle;

import com.pokemon.battle.engine.BattleEngine;
import com.pokemon.battle.exception.ValidationException;
import com.pokemon.battle.model.*;
import com.pokemon.battle.team.PokemonTeam;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModelReliabilityTest {
    Pokemon pokemon(String name) { Pokemon p=new Pokemon(name,"Normal",100,80,80,80);p.addMove(new Move("Tackle","Normal",40,100,1));return p; }
    @Test void negativeDamageRejected() { assertThrows(ValidationException.class,() -> pokemon("P").takeDamage(-1)); }
    @Test void negativeHealingRejected() { assertThrows(ValidationException.class,() -> pokemon("P").heal(-1)); }
    @Test void hpClampsToZero() { Pokemon p=pokemon("P");p.takeDamage(Integer.MAX_VALUE);assertEquals(0,p.getCurrentHP());assertTrue(p.isFainted()); }
    @Test void healingCannotOverflow() { Pokemon p=pokemon("P");p.takeDamage(1);p.heal(Integer.MAX_VALUE);assertEquals(100,p.getCurrentHP()); }
    @Test void zeroDefenseRejected() { assertThrows(ValidationException.class,() -> new Pokemon("P","Normal",100,80,0,80)); }
    @Test void invalidAccuracyRejected() { assertThrows(ValidationException.class,() -> new Move("X","Normal",40,101,10)); }
    @Test void ppStopsAtZero() { Move m=new Move("X","Normal",40,100,1);assertTrue(m.useMove());assertFalse(m.useMove());assertEquals(0,m.getCurrentPP()); }
    @Test void moveListIsProtected() { assertThrows(UnsupportedOperationException.class,() -> pokemon("P").getMoves().clear()); }
    @Test void fifthMoveRejected() { Pokemon p=pokemon("P");for(int i=0;i<3;i++)p.addMove(new Move("X","Normal",40,100,1));assertThrows(ValidationException.class,() -> p.addMove(new Move("Fifth","Normal",40,100,1))); }
    @Test void duplicateTeamMemberRejected() { PokemonTeam t=new PokemonTeam();t.addPokemon(pokemon("P"));assertThrows(ValidationException.class,() -> t.addPokemon(pokemon("p"))); }
    @Test void seventhTeamMemberRejected() { PokemonTeam t=new PokemonTeam();for(int i=0;i<6;i++)t.addPokemon(pokemon("P"+i));assertThrows(ValidationException.class,() -> t.addPokemon(pokemon("Seventh"))); }
    @Test void teamListIsProtected() { PokemonTeam t=new PokemonTeam();assertThrows(UnsupportedOperationException.class,() -> t.getTeam().add(pokemon("P"))); }
    @Test void faintedPokemonCannotSwitchIn() { PokemonTeam t=new PokemonTeam();t.addPokemon(pokemon("A"));Pokemon b=pokemon("B");b.takeDamage(100);t.addPokemon(b);assertFalse(t.switchPokemon(1)); }
    @Test void noPpFallbackAllowsBattleToContinue() {
        PokemonTeam p=new PokemonTeam(),e=new PokemonTeam();Pokemon a=pokemon("A"),b=pokemon("B");a.getMoves().getFirst().useMove();p.addPokemon(a);e.addPokemon(b);
        String result=new BattleEngine(p,e).playerAttack(-1);assertTrue(result.contains("Struggle"));assertTrue(b.getCurrentHP()<100);assertEquals(0,a.getMoves().getFirst().getCurrentPP());
    }
    @Test void fallbackRejectedWhenNormalPpRemain() {
        PokemonTeam p=new PokemonTeam(),e=new PokemonTeam();p.addPokemon(pokemon("A"));e.addPokemon(pokemon("B"));assertEquals("Invalid move!",new BattleEngine(p,e).playerAttack(-1));
    }
    @Test void eitherDefeatedTeamEndsBattle() { PokemonTeam p=new PokemonTeam(),e=new PokemonTeam();p.addPokemon(pokemon("A"));e.addPokemon(pokemon("B"));BattleEngine engine=new BattleEngine(p,e);e.getActivePokemon().takeDamage(100);assertTrue(engine.isBattleOver()); }
    @Test void immunityDoesNotDealMinimumDamage() {
        PokemonTeam p=new PokemonTeam(),e=new PokemonTeam();Pokemon a=new Pokemon("A","Electric",100,80,80,80),b=new Pokemon("B","Ground",100,80,80,80);a.addMove(new Move("Shock","Electric",80,100,1));p.addPokemon(a);e.addPokemon(b);new BattleEngine(p,e).playerAttack(0);assertEquals(100,b.getCurrentHP());
    }
}
