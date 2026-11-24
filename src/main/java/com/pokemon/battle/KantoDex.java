package com.pokemon.battle;

import com.pokemon.battle.model.*;
import java.util.*;

/** Original Kanto Pokédex, indexed by National Dex number (1-151). */
public final class KantoDex {
    private KantoDex() {}
    private static final String[] DATA = {
        "Bulbasaur|Grass",
        "Ivysaur|Grass",
        "Venusaur|Grass",
        "Charmander|Fire",
        "Charmeleon|Fire",
        "Charizard|Fire",
        "Squirtle|Water",
        "Wartortle|Water",
        "Blastoise|Water",
        "Caterpie|Bug",
        "Metapod|Bug",
        "Butterfree|Bug",
        "Weedle|Bug",
        "Kakuna|Bug",
        "Beedrill|Bug",
        "Pidgey|Normal",
        "Pidgeotto|Normal",
        "Pidgeot|Normal",
        "Rattata|Normal",
        "Raticate|Normal",
        "Spearow|Normal",
        "Fearow|Normal",
        "Ekans|Poison",
        "Arbok|Poison",
        "Pikachu|Electric",
        "Raichu|Electric",
        "Sandshrew|Ground",
        "Sandslash|Ground",
        "Nidoran-F|Poison",
        "Nidorina|Poison",
        "Nidoqueen|Poison",
        "Nidoran-M|Poison",
        "Nidorino|Poison",
        "Nidoking|Poison",
        "Clefairy|Fairy",
        "Clefable|Fairy",
        "Vulpix|Fire",
        "Ninetales|Fire",
        "Jigglypuff|Normal",
        "Wigglytuff|Normal",
        "Zubat|Poison",
        "Golbat|Poison",
        "Oddish|Grass",
        "Gloom|Grass",
        "Vileplume|Grass",
        "Paras|Bug",
        "Parasect|Bug",
        "Venonat|Bug",
        "Venomoth|Bug",
        "Diglett|Ground",
        "Dugtrio|Ground",
        "Meowth|Normal",
        "Persian|Normal",
        "Psyduck|Water",
        "Golduck|Water",
        "Mankey|Fighting",
        "Primeape|Fighting",
        "Growlithe|Fire",
        "Arcanine|Fire",
        "Poliwag|Water",
        "Poliwhirl|Water",
        "Poliwrath|Water",
        "Abra|Psychic",
        "Kadabra|Psychic",
        "Alakazam|Psychic",
        "Machop|Fighting",
        "Machoke|Fighting",
        "Machamp|Fighting",
        "Bellsprout|Grass",
        "Weepinbell|Grass",
        "Victreebel|Grass",
        "Tentacool|Water",
        "Tentacruel|Water",
        "Geodude|Rock",
        "Graveler|Rock",
        "Golem|Rock",
        "Ponyta|Fire",
        "Rapidash|Fire",
        "Slowpoke|Water",
        "Slowbro|Water",
        "Magnemite|Electric",
        "Magneton|Electric",
        "Farfetch'd|Normal",
        "Doduo|Normal",
        "Dodrio|Normal",
        "Seel|Water",
        "Dewgong|Water",
        "Grimer|Poison",
        "Muk|Poison",
        "Shellder|Water",
        "Cloyster|Water",
        "Gastly|Poison",
        "Haunter|Poison",
        "Gengar|Poison",
        "Onix|Rock",
        "Drowzee|Psychic",
        "Hypno|Psychic",
        "Krabby|Water",
        "Kingler|Water",
        "Voltorb|Electric",
        "Electrode|Electric",
        "Exeggcute|Grass",
        "Exeggutor|Grass",
        "Cubone|Ground",
        "Marowak|Ground",
        "Hitmonlee|Fighting",
        "Hitmonchan|Fighting",
        "Lickitung|Normal",
        "Koffing|Poison",
        "Weezing|Poison",
        "Rhyhorn|Ground",
        "Rhydon|Ground",
        "Chansey|Normal",
        "Tangela|Grass",
        "Kangaskhan|Normal",
        "Horsea|Water",
        "Seadra|Water",
        "Goldeen|Water",
        "Seaking|Water",
        "Staryu|Water",
        "Starmie|Water",
        "Mr-Mime|Psychic",
        "Scyther|Bug",
        "Jynx|Psychic",
        "Electabuzz|Electric",
        "Magmar|Fire",
        "Pinsir|Bug",
        "Tauros|Normal",
        "Magikarp|Water",
        "Gyarados|Water",
        "Lapras|Water",
        "Ditto|Normal",
        "Eevee|Normal",
        "Vaporeon|Water",
        "Jolteon|Electric",
        "Flareon|Fire",
        "Porygon|Normal",
        "Omanyte|Water",
        "Omastar|Water",
        "Kabuto|Water",
        "Kabutops|Water",
        "Aerodactyl|Flying",
        "Snorlax|Normal",
        "Articuno|Ice",
        "Zapdos|Electric",
        "Moltres|Fire",
        "Dratini|Dragon",
        "Dragonair|Dragon",
        "Dragonite|Dragon",
        "Mewtwo|Psychic",
        "Mew|Psychic"
    };
    public static int count() { return DATA.length; }
    public static String name(int id) { return DATA[id-1].split("\\|")[0]; }
    public static String type(int id) { return DATA[id-1].split("\\|")[1]; }
    public static int idFor(String name) {
        for (int i=1;i<=count();i++) if (name(i).equalsIgnoreCase(name)) return i;
        return -1;
    }
    public static Pokemon create(int id) {
        String type=type(id);
        int hp=110, atk=85, def=85, speed=85;
        // Offline fallback stats, updated from PokéAPI when available.
        int[] stats = PokeStats.fetch(id);
        if (stats != null) { hp=stats[0]; atk=stats[1]; def=stats[2]; speed=stats[3]; }
        Pokemon p=new Pokemon(name(id),type,hp,atk,def,speed);
        String moveName="Tackle"; int power=40;
        switch(type) {
            case "Fire": moveName="Flamethrower";power=90;break;
            case "Water": moveName="Water Pulse";power=60;break;
            case "Grass": moveName="Vine Whip";power=45;break;
            case "Electric": moveName="Thunderbolt";power=90;break;
            case "Psychic": moveName="Confusion";power=50;break;
            case "Ice": moveName="Ice Beam";power=90;break;
            case "Fighting": moveName="Karate Chop";power=50;break;
            case "Rock": moveName="Rock Throw";power=50;break;
            case "Ground": moveName="Mud Slap";power=40;break;
            case "Poison": moveName="Poison Sting";power=40;break;
            case "Bug": moveName="Bug Bite";power=60;break;
            case "Flying": moveName="Wing Attack";power=60;break;
            case "Dragon": moveName="Dragon Breath";power=60;break;
            case "Fairy": moveName="Fairy Wind";power=40;break;
        }
        p.addMove(new Move(moveName,type,power,100,20));
        p.addMove(new Move("Quick Attack","Normal",40,100,30));
        p.addMove(new Move("Swift","Normal",60,100,20));
        p.addMove(new Move("Headbutt","Normal",70,100,15));
        return p;
    }
}
