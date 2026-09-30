package com.tomcraft.cooldowntracker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Remembers each rune's icon glyph and theme colour, learned automatically
 * from the game's own styled text (item names/lore and chat lines) by
 * RuneStyleLearner. Kept in its own file (rune_styles.json), deliberately
 * separate from cooldowns.json: CSV imports and remote sync replace whole
 * TrackedItem entries, which would wipe anything stored on them.
 *
 * Keyed by the rune's name WITHOUT its tier, so learning "Lightning Crash I"
 * also styles "Lightning Crash III". Safe to read from the render thread
 * while the network/client threads learn (ConcurrentHashMap).
 */
public class RuneStyleCache {

    public static class Entry {
        public int color;        // ARGB theme colour of the rune, 0 = unknown
        public String glyph;     // icon character(s) shown before the name, may be null
        public String font;      // font id the glyph uses, null = default font
        public int glyphColor;   // ARGB colour of the glyph
        public boolean fromChat; // chat-learned data outranks item-text-learned data
        private transient Identifier fontId;

        public Entry() {}

        public Identifier fontId() {
            Identifier id = fontId;
            if (id == null) {
                try {
                    id = font == null ? new Identifier("minecraft", "default") : new Identifier(font);
                } catch (Exception e) {
                    id = new Identifier("minecraft", "default");
                }
                fontId = id;
            }
            return id;
        }
    }

    private static final Pattern TIER_SUFFIX = Pattern.compile("(?i)\\s+(I|II|III|IV|V|VI|VII|VIII|IX|X)$");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, Entry> styles = new ConcurrentHashMap<>();
    private static final Map<String, String> keyCache = new ConcurrentHashMap<>();
    private static volatile boolean dirty = false;
    private static long lastSave = 0;
    private static Path file;

    public static void init() {
        file = FabricLoader.getInstance().getConfigDir().resolve("cooldowntracker").resolve("rune_styles.json");
        load();
        // Batch writes: a first inventory scan can learn hundreds of runes in
        // one tick - flush once, at most once a second, never per rune.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (dirty && System.currentTimeMillis() - lastSave > 1000) {
                dirty = false;
                lastSave = System.currentTimeMillis();
                save();
            }
        });
    }

    /** Tier-stripped, normalized lookup key. Cached per display name so the 5x/sec HUD recompute never re-runs regex. */
    public static String keyFor(String displayName) {
        if (displayName == null) return "";
        String cached = keyCache.get(displayName);
        if (cached != null) return cached;
        String key = TextMatch.normalize(TIER_SUFFIX.matcher(displayName).replaceAll(""));
        keyCache.put(displayName, key);
        return key;
    }

    public static Entry get(String displayName) {
        return styles.get(keyFor(displayName));
    }

    public static int colorOr(String displayName, int fallback) {
        Entry e = get(displayName);
        return (e != null && e.color != 0) ? e.color : fallback;
    }

    static void learn(String key, int color, String glyph, String font, int glyphColor, boolean chat) {
        if (key == null || key.isEmpty()) return;
        Entry old = styles.get(key);
        boolean hasGlyph = glyph != null && !glyph.isEmpty();
        boolean oldChatLocked = old != null && old.fromChat && !chat;

        Entry n = new Entry();
        n.fromChat = chat || (old != null && old.fromChat);
        n.color = (color != 0 && !(oldChatLocked && old.color != 0)) ? color : (old != null ? old.color : 0);
        if (hasGlyph && !(oldChatLocked && old.glyph != null)) {
            n.glyph = glyph;
            n.font = font;
            n.glyphColor = glyphColor;
        } else if (old != null) {
            n.glyph = old.glyph;
            n.font = old.font;
            n.glyphColor = old.glyphColor;
        }

        if (old != null && old.color == n.color && old.glyphColor == n.glyphColor && old.fromChat == n.fromChat
                && Objects.equals(old.glyph, n.glyph) && Objects.equals(old.font, n.font)) {
            return; // nothing new - don't mark dirty
        }
        styles.put(key, n);
        dirty = true;
    }

    private static void load() {
        try {
            if (!Files.exists(file)) return;
            String json = Files.readString(file, StandardCharsets.UTF_8);
            Type type = new TypeToken<Map<String, Entry>>() {}.getType();
            Map<String, Entry> loaded = GSON.fromJson(json, type);
            if (loaded != null) {
                for (Map.Entry<String, Entry> e : loaded.entrySet()) {
                    if (e.getKey() != null && e.getValue() != null) styles.put(e.getKey(), e.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void save() {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(new HashMap<>(styles)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
