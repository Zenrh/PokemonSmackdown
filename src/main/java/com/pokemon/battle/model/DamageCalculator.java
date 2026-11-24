
package com.pokemon.battle.model;

import java.util.Random;

public class DamageCalculator {

    private static final Random random = new Random();

    public static int calculateDamage(Pokemon attacker,
                                      Pokemon defender,
                                      Move move) {

        if (move.getPower() <= 0) {
            return 0;
        }

        double level = 50.0;

        double baseDamage =
                (((2 * level / 5 + 2)
                * move.getPower()
                * attacker.getAttack()
                / defender.getDefense()) / 50) + 2;

        double effectiveness =
                TypeChart.getEffectiveness(
                        move.getType(),
                        defender.getType()
                );

        double randomMultiplier =
                0.85 + random.nextDouble() * 0.15;

        double finalDamage =
                baseDamage * effectiveness * randomMultiplier;

        return Math.max(1, (int) finalDamage);
    }

    public static String getEffectivenessMessage(
            Pokemon defender, Move move) {

        double effectiveness =
                TypeChart.getEffectiveness(
                        move.getType(),
                        defender.getType()
                );

        if (effectiveness == 0) {
            return "It had no effect!";
        } else if (effectiveness >= 2) {
            return "It's super effective!";
        } else if (effectiveness <= 0.5) {
            return "It's not very effective...";
        }

        return "";
    }
}
