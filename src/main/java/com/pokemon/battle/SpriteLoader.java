package com.pokemon.battle;

import com.pokemon.battle.model.Pokemon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/** Loads PokeAPI repository sprites once; network calls must run off the EDT. */
public final class SpriteLoader {
    private static final Map<String, BufferedImage> CACHE = new ConcurrentHashMap<>();
    private SpriteLoader() {}

    public static BufferedImage loadOriginal(Pokemon pokemon, boolean back) {
        int id = KantoDex.idFor(pokemon.getName());
        if (id < 1) return null;
        String key = id + (back ? "b" : "f");
        BufferedImage cached = CACHE.get(key);
        if (cached != null) return cached;
        String path = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/";
        String[] candidates = back
            ? new String[] {path + "versions/generation-v/black-white/animated/back/" + id + ".gif",
                            path + "back/" + id + ".png"}
            : new String[] {path + "versions/generation-v/black-white/animated/" + id + ".gif",
                            path + id + ".png"};
        // ImageIO reads the first GIF frame; use static frame plus Java animation.
        for (String candidate : candidates) {
            try {
                HttpURLConnection connection = (HttpURLConnection) URI.create(candidate).toURL().openConnection();
                connection.setConnectTimeout(3500);
                connection.setReadTimeout(3500);
                connection.setRequestProperty("User-Agent", "PokemonBattleSimulator/1.0");
                try (InputStream input = connection.getInputStream()) {
                    BufferedImage image = ImageIO.read(input);
                    if (image != null) {
                        CACHE.put(key, image);
                        return image;
                    }
                } finally { connection.disconnect(); }
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(SpriteLoader.class.getName()).log(java.util.logging.Level.FINE,"Sprite source unavailable; trying next candidate for Pokemon " + id,ex);
            }
        }
        return null;
    }

    public static ImageIcon loadSprite(Pokemon pokemon, boolean back, int size) {
        BufferedImage original = loadOriginal(pokemon, back);
        return original == null ? null : new ImageIcon(original.getScaledInstance(size, size, Image.SCALE_SMOOTH));
    }
}
