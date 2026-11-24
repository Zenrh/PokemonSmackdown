package com.pokemon.battle;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
/** Loads real base stats from PokéAPI, with offline fallback handled by KantoDex. */
public final class PokeStats {
    private PokeStats() {}
    private static final Map<Integer,int[]> CACHE = new ConcurrentHashMap<>();
    public static int[] fetch(int id) {
        if (CACHE.containsKey(id)) return CACHE.get(id);
        try {
            HttpURLConnection c=(HttpURLConnection)URI.create("https://pokeapi.co/api/v2/pokemon/"+id).toURL().openConnection();
            c.setConnectTimeout(800);c.setReadTimeout(800);
            String json;
            try(InputStream in=c.getInputStream()) { json=new String(in.readAllBytes(),StandardCharsets.UTF_8); }
            finally { c.disconnect(); }
            // Each stat object has base_stat followed by the stat name in the PokéAPI response.
            Pattern p=Pattern.compile("\"base_stat\"\\s*:\\s*(\\d+).*?\"stat\"\\s*:\\s*\\{[^}]*?\"name\"\\s*:\\s*\"([^\"]+)\"",Pattern.DOTALL);
            Matcher m=p.matcher(json);int hp=0,atk=0,def=0,speed=0;
            while(m.find()) { int val=Integer.parseInt(m.group(1));switch(m.group(2)) {
                case "hp":hp=val;break;case "attack":atk=val;break;case "defense":def=val;break;case "speed":speed=val;break;
            }}
            if(hp>0&&atk>0&&def>0&&speed>0) {
                int[] result={Math.max(30,hp*2+50),atk,def,speed};CACHE.put(id,result);return result;
            }
        } catch(Exception ex) {
            java.util.logging.Logger.getLogger(PokeStats.class.getName()).log(java.util.logging.Level.FINE,"Stats unavailable for Pokemon " + id + "; using fallback.",ex);
        }
        return null;
    }
}
