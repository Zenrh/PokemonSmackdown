
package com.pokemon.battle.model;

public class TypeChart {

    public static double getEffectiveness(
            String moveType, String defenderType) {

        moveType = moveType.toLowerCase();
        defenderType = defenderType.toLowerCase();

        switch (moveType) {

            case "fire":
                if (defenderType.equals("grass")
                        || defenderType.equals("ice")
                        || defenderType.equals("bug")
                        || defenderType.equals("steel")) {
                    return 2.0;
                }
                if (defenderType.equals("water")
                        || defenderType.equals("fire")
                        || defenderType.equals("rock")
                        || defenderType.equals("dragon")) {
                    return 0.5;
                }
                break;

            case "water":
                if (defenderType.equals("fire")
                        || defenderType.equals("ground")
                        || defenderType.equals("rock")) {
                    return 2.0;
                }
                if (defenderType.equals("water")
                        || defenderType.equals("grass")
                        || defenderType.equals("dragon")) {
                    return 0.5;
                }
                break;

            case "grass":
                if (defenderType.equals("water")
                        || defenderType.equals("ground")
                        || defenderType.equals("rock")) {
                    return 2.0;
                }
                if (defenderType.equals("fire")
                        || defenderType.equals("grass")
                        || defenderType.equals("poison")
                        || defenderType.equals("flying")
                        || defenderType.equals("bug")
                        || defenderType.equals("dragon")
                        || defenderType.equals("steel")) {
                    return 0.5;
                }
                break;

            case "electric":
                if (defenderType.equals("water")
                        || defenderType.equals("flying")) {
                    return 2.0;
                }
                if (defenderType.equals("electric")
                        || defenderType.equals("grass")
                        || defenderType.equals("dragon")) {
                    return 0.5;
                }
                if (defenderType.equals("ground")) {
                    return 0.0;
                }
                break;
        }

        return 1.0;
    }
}
