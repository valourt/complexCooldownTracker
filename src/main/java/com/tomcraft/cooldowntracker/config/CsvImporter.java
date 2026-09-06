package com.tomcraft.cooldowntracker.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bulk-imports TrackedItems from a CSV exported by the companion Excel
 * template (columns: id,displayName,trigger,pattern,cooldownSeconds,enabled).
 * Column order in the file doesn't matter as long as the header names match -
 * only the header row is used to figure out which column is which.
 */
public class CsvImporter {

    /**
     * Same reserved set as CooldownCommands - these three ids are detected
     * directly from game state, never from chat, so a CSV row that happens
     * to generate one of these exact ids would silently replace the
     * working built-in entry with a chat-trigger one that can never fire.
     */
    private static final java.util.Set<String> RESERVED_IDS =
            java.util.Set.of("totem", "golden_apple", "enchanted_golden_apple");

    public static class Result {
        public int imported = 0;
        public int skipped = 0;
        public final List<String> errors = new ArrayList<>();
    }

    public static Result importFile(String fileName) {
        Result result = new Result();
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("cooldowntracker");
        Path file = dir.resolve(fileName);

        if (!Files.exists(file)) {
            result.errors.add("File not found: " + file);
            return result;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            result.errors.add("Could not read file: " + e.getMessage());
            return result;
        }

        if (lines.isEmpty()) {
            result.errors.add("File is empty.");
            return result;
        }

        List<String> header = parseCsvLine(lines.get(0));
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            col.put(header.get(i).trim().toLowerCase(), i);
        }

        String[] required = {"itemname", "cooldownseconds"};
        for (String req : required) {
            if (!col.containsKey(req)) {
                result.errors.add("Missing required column: " + req);
                return result;
            }
        }

        for (int lineNum = 1; lineNum < lines.size(); lineNum++) {
            String raw = lines.get(lineNum);
            if (raw.trim().isEmpty()) continue;

            List<String> fields = parseCsvLine(raw);
            try {
                String itemName = get(fields, col, "itemname");
                if (itemName.isEmpty()) {
                    result.skipped++;
                    result.errors.add("Line " + (lineNum + 1) + ": blank itemName, skipped.");
                    continue;
                }

                String id = col.containsKey("id") && !get(fields, col, "id").isEmpty()
                        ? get(fields, col, "id") : slugify(itemName);
                if (RESERVED_IDS.contains(id.toLowerCase())) {
                    result.skipped++;
                    result.errors.add("Line " + (lineNum + 1) + ": '" + itemName + "' collides with the built-in '"
                            + id + "' entry (detected from the game, not chat) - skipped to avoid breaking it.");
                    continue;
                }
                String displayName = col.containsKey("displayname") && !get(fields, col, "displayname").isEmpty()
                        ? get(fields, col, "displayname") : itemName;
                String trigger = col.containsKey("trigger") && !get(fields, col, "trigger").isEmpty()
                        ? get(fields, col, "trigger") : "chat";
                boolean explicitPattern = col.containsKey("pattern") && !get(fields, col, "pattern").isEmpty();
                String pattern = explicitPattern
                        ? get(fields, col, "pattern")
                        : "(?i).*\\b" + java.util.regex.Pattern.quote(itemName) + "\\b.*";
                double cooldown = Double.parseDouble(get(fields, col, "cooldownseconds"));
                boolean enabled = true;
                if (col.containsKey("enabled")) {
                    String e = get(fields, col, "enabled").trim();
                    enabled = e.isEmpty() || Boolean.parseBoolean(e) || e.equalsIgnoreCase("yes") || e.equals("1");
                }

                TrackedItem item = new TrackedItem(id, displayName, trigger, pattern, cooldown);
                item.enabled = enabled;
                item.itemName = itemName;
                // Only set the fast literal-match path when we generated the
                // pattern ourselves from a plain name - a user-supplied
                // pattern might be genuine regex, so leave that on the
                // (slower but general-purpose) regex path.
                if (!explicitPattern) {
                    item.literalMatch = itemName.toLowerCase();
                }
                if (col.containsKey("color") && !get(fields, col, "color").isEmpty()) {
                    String rawColor = get(fields, col, "color");
                    item.color = rawColor.startsWith("#") ? rawColor.substring(1) : rawColor;
                }
                CooldownConfig.addOrUpdate(item);
                result.imported++;
            } catch (Exception e) {
                result.skipped++;
                result.errors.add("Line " + (lineNum + 1) + ": " + e.getMessage());
            }
        }

        return result;
    }

    private static String get(List<String> fields, Map<String, Integer> col, String name) {
        int idx = col.get(name);
        return idx < fields.size() ? fields.get(idx).trim() : "";
    }

    private static String slugify(String name) {
        String slug = name.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return slug.isEmpty() ? "item_" + Integer.toHexString(name.hashCode()) : slug;
    }

    /**
     * Minimal RFC4180-ish CSV line parser: handles quoted fields, embedded
     * commas inside quotes, and "" as an escaped quote.
     */
    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
