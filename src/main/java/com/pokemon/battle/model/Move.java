
package com.pokemon.battle.model;

import com.pokemon.battle.exception.ValidationException;

public class Move {

    private String name;
    private String type;
    private int power;
    private int accuracy;
    private int maxPP;
    private int currentPP;

    public Move(String name, String type, int power,
                int accuracy, int maxPP) {
        if(name==null || name.isBlank() || type==null || type.isBlank())
            throw new ValidationException("Move name and type are required.");
        if(power<0 || accuracy<0 || accuracy>100 || maxPP<=0)
            throw new ValidationException("Move power must be nonnegative, accuracy 0 to 100, and PP positive.");

        this.name = name;
        this.type = type;
        this.power = power;
        this.accuracy = accuracy;
        this.maxPP = maxPP;
        this.currentPP = maxPP;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getPower() {
        return power;
    }

    public int getAccuracy() {
        return accuracy;
    }

    public int getCurrentPP() {
        return currentPP;
    }

    public boolean useMove() {
        if (currentPP <= 0) {
            return false;
        }

        currentPP--;
        return true;
    }

    public void restorePP() {
        currentPP = maxPP;
    }
}
