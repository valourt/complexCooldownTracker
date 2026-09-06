package com.tomcraft.cooldowntracker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Loads/saves config/cooldowntracker/cooldowns.json. Edit that file (or use
 * the /cooldowns commands) to add new runes as they come out.
 */
public class CooldownConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<TrackedItem>>() {
    }.getType();

    private static Path configDir;
    private static Path configFile;

    private static List<TrackedItem> items = new ArrayList<>();

    /**
     * Pre-filtered view of items list: only enabled, chat-triggered items
     * with a valid compiled pattern. Rebuilt whenever items load/change so
     * ChatCooldownListener - which runs on every single chat line - doesn't
     * have to branch on trigger type and enabled/null checks for all ~500+
     * items on every line, most of which aren't chat-triggered at all.
     */
    private static List<TrackedItem> chatTriggerItems = new ArrayList<>();

    public static void init() {
        configDir = FabricLoader.getInstance().getConfigDir().resolve("cooldowntracker");
        configFile = configDir.resolve("cooldowns.json");
        load();
    }

    public static synchronized List<TrackedItem> getItems() {
        return items;
    }

    public static synchronized List<TrackedItem> getChatTriggerItems() {
        return chatTriggerItems;
    }

    public static synchronized TrackedItem findById(String id) {
        for (TrackedItem item : items) {
            if (item.id.equalsIgnoreCase(id)) return item;
        }
        return null;
    }

    public static synchronized void load() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            if (!Files.exists(configFile)) {
                writeDefaults();
            }
            try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                List<TrackedItem> loaded = GSON.fromJson(reader, LIST_TYPE);
                items = loaded != null ? loaded : new ArrayList<>();
            }
            items.removeIf(java.util.Objects::isNull);
            validate();
            compileAll();
            rebuildChatTriggerCache();
        } catch (IOException e) {
            e.printStackTrace();
            items = new ArrayList<>();
        }
    }

    private static void compileAll() {
        for (TrackedItem item : items) {
            item.compilePattern();
        }
    }

    private static void rebuildChatTriggerCache() {
        List<TrackedItem> filtered = new ArrayList<>();
        for (TrackedItem item : items) {
            if (item.enabled && "chat".equals(item.trigger) && item.compiledPattern != null) {
                filtered.add(item);
            }
        }
        chatTriggerItems = filtered;
    }

    private static void validate() {
        for (TrackedItem item : items) {
            if ("chat".equals(item.trigger) && item.pattern != null) {
                try {
                    Pattern.compile(item.pattern);
                } catch (PatternSyntaxException ex) {
                    System.err.println("[CooldownTracker] Invalid regex for '" + item.id + "': " + ex.getMessage());
                }
            }
        }
    }

    public static synchronized void save() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                GSON.toJson(items, LIST_TYPE, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeDefaults() throws IOException {
        List<TrackedItem> defaults = new ArrayList<>();

        // EXAMPLE: your Tier 2 Halloween Axe rune.
        // IMPORTANT: update `pattern` to match the EXACT wording of the chat
        // line Complex Gaming Factions sends when you use it, and update
        // `itemName` to the item's EXACT in-game name (hover over it in your
        // inventory) - itemName is what makes the "Ready" box only show items
        // you actually own.
        TrackedItem axe = new TrackedItem(
                "halloween_axe_t2",
                "Halloween Axe [T2]",
                "chat",
                "(?i).*halloween axe.*",
                220
        );
        axe.itemName = "Halloween Axe II";
        defaults.add(axe);

        // Built-in triggers - detected directly from game events, no chat
        // message needed. The cooldownSeconds values below are PLACEHOLDERS:
        // set them to whatever Complex Gaming Factions actually enforces.
        defaults.add(new TrackedItem("totem", "Totem of Undying", "totem", null, 0));
        defaults.add(new TrackedItem("golden_apple", "Golden Apple", "golden_apple", null, 0));
        defaults.add(new TrackedItem("enchanted_golden_apple", "Enchanted Golden Apple", "enchanted_golden_apple", null, 600));

        items = defaults;
        save();
    }

    public static synchronized void addOrUpdate(TrackedItem item) {
        item.compilePattern();
        items.removeIf(i -> i.id.equalsIgnoreCase(item.id));
        items.add(item);
        rebuildChatTriggerCache();
        save();
    }

    public static synchronized boolean remove(String id) {
        boolean removed = items.removeIf(i -> i.id.equalsIgnoreCase(id));
        if (removed) {
            rebuildChatTriggerCache();
            save();
        }
        return removed;
    }
}
