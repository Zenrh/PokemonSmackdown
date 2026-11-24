
package com.pokemon.battle.model;

import java.util.ArrayList;
import java.util.List;
import com.pokemon.battle.exception.ValidationException;

public class Pokemon {

    private String name;
    private String type;
    private int maxHP;
    private int currentHP;
    private int attack;
    private int defense;
    private int speed;
    private List<Move> moves;

    public Pokemon(String name, String type, int maxHP,
                   int attack, int defense, int speed) {
        if(name==null || name.isBlank() || type==null || type.isBlank())
            throw new ValidationException("Pokemon name and type are required.");
        if(maxHP<=0 || attack<=0 || defense<=0 || speed<=0)
            throw new ValidationException("Pokemon HP, Attack, Defense, and Speed must be positive.");

        this.name = name;
        this.type = type;
        this.maxHP = maxHP;
        this.currentHP = maxHP;
        this.attack = attack;
        this.defense = defense;
        this.speed = speed;
        this.moves = new ArrayList<>();
    }

    public void addMove(Move move) {
        if(move==null)throw new ValidationException("A move is required.");
        if(moves.size()>=4)throw new ValidationException("A Pokemon can have at most four moves.");
        moves.add(move);
    }

    public List<Move> getMoves() {
        return List.copyOf(moves);
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public int getMaxHP() { return maxHP; }
    public int getCurrentHP() { return currentHP; }
    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public int getSpeed() { return speed; }

    public boolean isFainted() {
        return currentHP <= 0;
    }

    public void takeDamage(int damage) {
        if(damage<0)throw new ValidationException("Damage cannot be negative.");
        currentHP = Math.max(0, currentHP - damage);
    }

    public void heal(int amount) {
        if(amount<0)throw new ValidationException("Healing cannot be negative.");
        currentHP = (int)Math.min(maxHP, (long)currentHP + amount);
    }
}
