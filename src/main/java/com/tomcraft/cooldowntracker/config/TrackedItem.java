package com.tomcraft.cooldowntracker.config;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * One tracked cooldown entry in cooldowns.json.
 *
 * trigger:
 *  - "chat"                     -> matches `pattern` against every chat/system line
 *  - "totem"                    -> fires when YOU pop a Totem of Undying
 *  - "golden_apple"             -> fires when YOU finish eating a Golden Apple
 *  - "enchanted_golden_apple"   -> fires when YOU finish eating an Enchanted Golden Apple
 */
public class TrackedItem {
    public String id;
    public String displayName;
    public String trigger;
    public String pattern;
    public double cooldownSeconds;
    public boolean enabled = true;

    /**
     * The item's EXACT in-game display name (e.g. "Green Shell II"),
     * used to check whether you currently have it in your inventory for
     * the "Ready" box. Only needed for "chat" trigger items - totem/
     * golden_apple/enchanted_golden_apple are matched by their real
     * Minecraft item id instead, automatically. Leave null/blank if you
     * don't want inventory-ownership filtering for this item (it'll then
     * always show in "Ready" once off cooldown, regardless of whether you
     * actually have it).
     */
    public String itemName;

    /**
     * Optional fixed display color for this item's line in the "On
     * Cooldown" box, as a 6-digit hex string (e.g. "FF8800", no #). When
     * set, this always wins over the automatic urgency coloring (white ->
     * amber -> red as it nears zero) - the point is to tell items apart by
     * identity rather than by how soon they're ready. Leave null to keep
     * the automatic urgency coloring.
     */
    public String color;

    /**
     * Parsed once from `color` alongside the other compiled fields (0xFFxxxxxx
     * with full alpha), or null if `color` is unset/invalid.
     */
    public transient Integer colorArgb;

    /**
     * Plain lowercase text to whole-word-match against chat lines, set by
     * CsvImporter/additem when they auto-generate a pattern from a literal
     * item name (which is ~500 of your 504 items). When set, chat matching
     * uses a hand-written substring scan instead of the regex engine -
     * checking hundreds of compiled regexes against every line of a chat
     * dump (e.g. "/f who") has real per-call overhead even pre-compiled,
     * where a plain indexOf-based scan does not. Leave null for genuinely
     * custom regex (e.g. written by hand via /cooldowns addrune) - those
     * still use compiledPattern.
     */
    public String literalMatch;

    /**
     * Compiled once when the config loads/changes, not on every chat line -
     * recompiling a regex from its string form on every single check (504
     * items x every chat line) is what was causing noticeable lag on chat-
     * heavy commands. Never serialized (transient), so Gson skips it. Only
     * used as a fallback when literalMatch isn't set.
     */
    public transient Pattern compiledPattern;

    /**
     * Compiled once alongside compiledPattern, for matching against
     * inventory item names/lore in InventoryOwnershipScanner - same
     * reasoning: avoid recompiling a regex on every scan (twice a second,
     * across every tracked item).
     */
    public transient Pattern compiledItemNamePattern;

    /**
     * Normalized (lowercased, icon-stripped) form of itemName, for the
     * fast substring-scan path in InventoryOwnershipScanner - same
     * reasoning as literalMatch/compiledItemNamePattern above: avoid the
     * regex engine for a check that runs against every tracked item, on
     * every inventory scan.
     */
    public transient String itemNameNormalized;

    public TrackedItem() {
    }

    public TrackedItem(String id, String displayName, String trigger, String pattern, double cooldownSeconds) {
        this.id = id;
        this.displayName = displayName;
        this.trigger = trigger;
        this.pattern = pattern;
        this.cooldownSeconds = cooldownSeconds;
    }

    public void compilePattern() {
        if ("chat".equals(trigger) && pattern != null) {
            try {
                compiledPattern = Pattern.compile(pattern);
            } catch (PatternSyntaxException e) {
                compiledPattern = null;
            }
        } else {
            compiledPattern = null;
        }

        if (itemName != null && !itemName.isEmpty()) {
            String needle = TextMatch.normalize(itemName);
            itemNameNormalized = needle.isEmpty() ? null : needle;
            if (!needle.isEmpty()) {
                try {
                    compiledItemNamePattern = Pattern.compile("\\b" + Pattern.quote(needle) + "\\b");
                } catch (PatternSyntaxException e) {
                    compiledItemNamePattern = null;
                }
            } else {
                compiledItemNamePattern = null;
            }
        } else {
            compiledItemNamePattern = null;
            itemNameNormalized = null;
        }

        if (color != null && color.matches("[0-9a-fA-F]{6}")) {
            try {
                colorArgb = 0xFF000000 | Integer.parseInt(color, 16);
            } catch (NumberFormatException e) {
                colorArgb = null;
            }
        } else {
            colorArgb = null;
        }
    }

    private static String normalize(String s) {
        return TextMatch.normalize(s);
    }
}
