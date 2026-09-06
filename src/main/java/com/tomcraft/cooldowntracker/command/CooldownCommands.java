package com.tomcraft.cooldowntracker.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.tomcraft.cooldowntracker.config.CooldownConfig;
import com.tomcraft.cooldowntracker.config.CsvImporter;
import com.tomcraft.cooldowntracker.config.TrackedItem;
import com.tomcraft.cooldowntracker.hud.HudEditScreen;
import com.tomcraft.cooldowntracker.hud.HudLayoutConfig;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.literal;

/**
 * /cooldowns version          - show the running mod version
 * /cooldowns list [search]     - show item count, or search by id/name (capped to avoid chat spam)
 * /cooldowns additem <seconds> <name>  - add/update by exact item name (recommended - simplest)
 * /cooldowns addrune <id> <seconds> <regex>  - add/update with a custom regex + id
 * /cooldowns remove <id>       - remove a tracked item
 * /cooldowns importcsv [file]  - bulk-import items from a CSV (default: cooldowns.csv)
 * /cooldowns hud                - open the drag-to-reposition/resize HUD editor
 * /cooldowns settings           - open the settings GUI directly
 * /cooldowns togglereadybox     - show/hide the "Ready" box
 * /cooldowns togglecooldownbox  - show/hide the "On Cooldown" box
 * /cooldowns togglebackpackbox  - show/hide the backpack capacity box
 * /cooldowns toggletotemwatch    - show/hide the other-players' totem cooldown box
 * /cooldowns togglesnakeeyes      - show/hide the Snake Eyes current-buff box
 * /cooldowns togglemoodswings     - show/hide the Mood Swings current-mood box
 * /cooldowns togglepopup        - show/hide the ready pop-up
 * /cooldowns setscale <ready|cooldown|backpack|totem|snake|mood> <50-300> - set a box's text/size as a percentage
 * /cooldowns setcolor <id> <hex> - give one item a fixed display color, e.g. FF8800
 * /cooldowns setbgcolor <hex>   - set the HUD panels' background color
 * /cooldowns setopacity <0-100> - set the HUD panels' background opacity
 * /cooldowns fixbuiltins        - repair totem/golden_apple/enchanted_golden_apple
 *                                 if additem/addrune/importcsv ever overwrote one
 */
public class CooldownCommands {

    /**
     * These three ids are detected directly from game state (health/
     * absorption for totem, item-use transitions for the apples), never
     * from chat - additem/addrune generating or accepting one of these
     * exact ids would silently replace the working built-in entry with a
     * chat-trigger one that can never fire, since none of these three
     * actually send a chat message.
     */
    private static final java.util.Set<String> RESERVED_IDS =
            java.util.Set.of("totem", "golden_apple", "enchanted_golden_apple");

    public static void register() {
        ClientCommandManager.DISPATCHER.register(
                literal("cooldowns")
                        .then(literal("version").executes(ctx -> {
                            String version = net.fabricmc.loader.api.FabricLoader.getInstance()
                                    .getModContainer("cooldowntracker")
                                    .map(c -> c.getMetadata().getVersion().getFriendlyString())
                                    .orElse("unknown");
                            ctx.getSource().sendFeedback(new LiteralText("[CooldownTracker] Version " + version));
                            return 1;
                        }))
                        .then(literal("reload").executes(ctx -> {
                            CooldownConfig.load();
                            ctx.getSource().sendFeedback(new LiteralText(
                                    "[CooldownTracker] Config reloaded (" + CooldownConfig.getItems().size() + " items)."));
                            return 1;
                        }))
                        .then(literal("importcsv")
                                .executes(ctx -> runImport(ctx, "cooldowns.csv"))
                                .then(argument("filename", StringArgumentType.string())
                                        .executes(ctx -> runImport(ctx, getString(ctx, "filename")))))
                        .then(literal("hud").executes(ctx -> {
                            MinecraftClient.getInstance().setScreen(new HudEditScreen());
                            return 1;
                        }))
                        .then(literal("settings").executes(ctx -> {
                            MinecraftClient.getInstance().setScreen(new com.tomcraft.cooldowntracker.hud.CooldownSettingsScreen(null));
                            return 1;
                        }))
                        .then(literal("togglereadybox").executes(ctx -> {
                            HudLayoutConfig.Layout layout = HudLayoutConfig.get();
                            layout.readyBoxVisible = !layout.readyBoxVisible;
                            HudLayoutConfig.save();
                            ctx.getSource().sendFeedback(new LiteralText(
                                    "[CooldownTracker] Ready box " + (layout.readyBoxVisible ? "shown" : "hidden") + "."));
                            return 1;
                        }))
                        .then(literal("togglecooldownbox").executes(ctx -> {
                            HudLayoutConfig.Layout layout = HudLayoutConfig.get();
                            layout.cooldownBoxVisible = !layout.cooldownBoxVisible;
                            HudLayoutConfig.save();
                            ctx.getSource().sendFeedback(new LiteralText(
                                    "[CooldownTracker] On Cooldown box " + (layout.cooldownBoxVisible ? "shown" : "hidden") + "."));
                            return 1;
                        }))
                        .then(literal("togglebackpackbox").executes(ctx -> {
                            HudLayoutConfig.Layout layout = HudLayoutConfig.get();
                            layout.backpackBoxVisible = !layout.backpackBoxVisible;
                            HudLayoutConfig.save();
                            ctx.getSource().sendFeedback(new LiteralText(
                                    "[CooldownTracker] Backpack box " + (layout.backpackBoxVisible ? "shown" : "hidden") + "."));
                            return 1;
                        }))
                        .then(literal("toggletotemwatch").executes(ctx -> {
                            HudLayoutConfig.Layout layout = HudLayoutConfig.get();
                            layout.totemWatchBoxVisible = !layout.totemWatchBoxVisible;
                            HudLayoutConfig.save();
                            ctx.getSource().sendFeedback(new LiteralText(
                                    "[CooldownTracker] Totem Watch box " + (layout.totemWatchBoxVisible ? "shown" : "hidden") + "."));
                            return 1;
                        }))
                        .then(literal("togglesnakeeyes").executes(ctx -> {
                            HudLayoutConfig.Layout layout = HudLayoutConfig.get();
                            layout.snakeEyesBoxVisible = !layout.snakeEyesBoxVisible;
                            HudLayoutConfig.save();
                            ctx.getSource().sendFeedback(new LiteralText(
                                    "[CooldownTracker] Snake Eyes box " + (layout.snakeEyesBoxVisible ? "shown" : "hidden") + "."));
                            return 1;
                        }))
                        .then(literal("togglemoodswings").executes(ctx -> {
                            HudLayoutConfig.Layout layout = HudLayoutConfig.get();
                            layout.moodSwingsBoxVisible = !layout.moodSwingsBoxVisible;
                            HudLayoutConfig.save();
                            ctx.getSource().sendFeedback(new LiteralText(
                                    "[CooldownTracker] Mood Swings box " + (layout.moodSwingsBoxVisible ? "shown" : "hidden") + "."));
                            return 1;
                        }))
                        .then(literal("togglepopup").executes(ctx -> {
                            HudLayoutConfig.Layout layout = HudLayoutConfig.get();
                            layout.toastVisible = !layout.toastVisible;
                            HudLayoutConfig.save();
                            ctx.getSource().sendFeedback(new LiteralText(
                                    "[CooldownTracker] Ready pop-up " + (layout.toastVisible ? "shown" : "hidden") + "."));
                            return 1;
                        }))
                        .then(literal("setscale")
                                .then(argument("box", StringArgumentType.word())
                                        .then(argument("percent", com.mojang.brigadier.arguments.IntegerArgumentType.integer(
                                                com.tomcraft.cooldowntracker.hud.CooldownBoxes.MIN_SCALE_PERCENT,
                                                com.tomcraft.cooldowntracker.hud.CooldownBoxes.MAX_SCALE_PERCENT))
                                                .executes(ctx -> {
                                                    String box = getString(ctx, "box").toLowerCase();
                                                    int percent = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "percent");
                                                    HudLayoutConfig.Layout layout = HudLayoutConfig.get();
                                                    float scale = com.tomcraft.cooldowntracker.hud.CooldownBoxes.clampScale(percent / 100f);
                                                    if (box.startsWith("ready")) {
                                                        layout.readyBoxScale = scale;
                                                    } else if (box.startsWith("cooldown")) {
                                                        layout.cooldownBoxScale = scale;
                                                    } else if (box.startsWith("backpack")) {
                                                        layout.backpackBoxScale = scale;
                                                    } else if (box.startsWith("totem")) {
                                                        layout.totemWatchBoxScale = scale;
                                                    } else if (box.startsWith("snake")) {
                                                        layout.snakeEyesBoxScale = scale;
                                                    } else if (box.startsWith("mood")) {
                                                        layout.moodSwingsBoxScale = scale;
                                                    } else {
                                                        ctx.getSource().sendFeedback(new LiteralText(
                                                                "[CooldownTracker] Unknown box '" + box + "' - use 'ready', 'cooldown', 'backpack', 'totem', 'snake', or 'mood'."));
                                                        return 0;
                                                    }
                                                    HudLayoutConfig.save();
                                                    ctx.getSource().sendFeedback(new LiteralText(
                                                            "[CooldownTracker] " + box + " box scale set to " + percent + "%."));
                                                    return 1;
                                                }))))
                        .then(literal("setcolor")
                                .then(argument("id", StringArgumentType.word())
                                        .then(argument("hex", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    String id = getString(ctx, "id");
                                                    String hex = getString(ctx, "hex");
                                                    return runSetColor(ctx, id, hex);
                                                }))))
                        .then(literal("setbgcolor")
                                .then(argument("hex", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String hex = getString(ctx, "hex");
                                            String cleaned = hex.startsWith("#") ? hex.substring(1) : hex;
                                            if (cleaned.length() != 6 || !cleaned.matches("[0-9a-fA-F]{6}")) {
                                                ctx.getSource().sendFeedback(new LiteralText(
                                                        "[CooldownTracker] '" + hex + "' isn't a valid color - use a 6-digit hex code, e.g. 1E1E1E or #1E1E1E."));
                                                return 0;
                                            }
                                            HudLayoutConfig.Layout layout = HudLayoutConfig.get();
                                            layout.backgroundColorHex = cleaned;
                                            HudLayoutConfig.save();
                                            ctx.getSource().sendFeedback(new LiteralText(
                                                    "[CooldownTracker] HUD background set to #" + cleaned + "."));
                                            return 1;
                                        })))
                        .then(literal("setopacity")
                                .then(argument("percent", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 100))
                                        .executes(ctx -> {
                                            int percent = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "percent");
                                            HudLayoutConfig.Layout layout = HudLayoutConfig.get();
                                            layout.backgroundOpacityPercent = percent;
                                            HudLayoutConfig.save();
                                            ctx.getSource().sendFeedback(new LiteralText(
                                                    "[CooldownTracker] HUD background opacity set to " + percent + "%."));
                                            return 1;
                                        })))
                        .then(literal("list")
                                .executes(ctx -> runList(ctx, null))
                                .then(argument("search", StringArgumentType.greedyString())
                                        .executes(ctx -> runList(ctx, getString(ctx, "search")))))
                        .then(literal("additem")
                                .then(argument("seconds", DoubleArgumentType.doubleArg(0))
                                        .then(argument("name", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    double seconds = DoubleArgumentType.getDouble(ctx, "seconds");
                                                    String name = getString(ctx, "name");
                                                    String id = slugify(name);
                                                    if (RESERVED_IDS.contains(id)) {
                                                        ctx.getSource().sendFeedback(new LiteralText(
                                                                "[CooldownTracker] '" + name + "' collides with the built-in '" + id
                                                                        + "' entry (totem/golden apple/enchanted golden apple are detected "
                                                                        + "directly from the game, not chat) - this would break it. Pick a "
                                                                        + "different name, or use /cooldowns fixbuiltins if you've already "
                                                                        + "overwritten one of these by mistake."));
                                                        return 0;
                                                    }
                                                    String pattern = "(?i).*\\b" + java.util.regex.Pattern.quote(name) + "\\b.*";
                                                    TrackedItem item = new TrackedItem(id, name, "chat", pattern, seconds);
                                                    item.itemName = name;
                                                    item.literalMatch = name.toLowerCase();
                                                    CooldownConfig.addOrUpdate(item);
                                                    ctx.getSource().sendFeedback(new LiteralText(
                                                            "[CooldownTracker] Added '" + name + "' (" + seconds + "s)."));
                                                    return 1;
                                                }))))
                        .then(literal("fixbuiltins").executes(ctx -> runFixBuiltins(ctx)))
                        .then(literal("addrune")
                                .then(argument("id", StringArgumentType.word())
                                        .then(argument("seconds", DoubleArgumentType.doubleArg(0))
                                                .then(argument("pattern", StringArgumentType.greedyString())
                                                        .executes(ctx -> {
                                                            String id = getString(ctx, "id");
                                                            if (RESERVED_IDS.contains(id.toLowerCase())) {
                                                                ctx.getSource().sendFeedback(new LiteralText(
                                                                        "[CooldownTracker] '" + id + "' is a reserved built-in id "
                                                                                + "(detected directly from the game, not chat) - use a "
                                                                                + "different id, or /cooldowns fixbuiltins if you've "
                                                                                + "already overwritten it."));
                                                                return 0;
                                                            }
                                                            double seconds = DoubleArgumentType.getDouble(ctx, "seconds");
                                                            String pattern = getString(ctx, "pattern");
                                                            CooldownConfig.addOrUpdate(new TrackedItem(id, id, "chat", pattern, seconds));
                                                            ctx.getSource().sendFeedback(new LiteralText(
                                                                    "[CooldownTracker] Added/updated '" + id + "' (" + seconds + "s)."));
                                                            return 1;
                                                        })))))
                        .then(literal("remove")
                                .then(argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String id = getString(ctx, "id");
                                            boolean removed = CooldownConfig.remove(id);
                                            ctx.getSource().sendFeedback(new LiteralText(
                                                    removed ? "[CooldownTracker] Removed '" + id + "'."
                                                            : "[CooldownTracker] No item with id '" + id + "'."));
                                            return 1;
                                        })))
        );
    }

    /**
     * With 500+ items in a fully-populated config, dumping every single one
     * into chat at once is more spam than it's worth. No search term just
     * gives a count; a search term filters by id/displayName, capped so a
     * broad search doesn't flood chat either.
     */
    private static int runList(CommandContext<FabricClientCommandSource> ctx, String search) {
        java.util.List<TrackedItem> all = CooldownConfig.getItems();

        if (search == null) {
            long chatCount = all.stream().filter(i -> "chat".equals(i.trigger)).count();
            ctx.getSource().sendFeedback(new LiteralText(
                    "[CooldownTracker] Tracking " + all.size() + " item(s) (" + chatCount + " chat-triggered). "
                            + "Use /cooldowns list <search> to find a specific one."));
            return all.size();
        }

        String needle = search.toLowerCase();
        int shown = 0;
        int matched = 0;
        for (TrackedItem item : all) {
            boolean matches = item.id.toLowerCase().contains(needle)
                    || (item.displayName != null && item.displayName.toLowerCase().contains(needle));
            if (!matches) continue;
            matched++;
            if (shown >= 20) continue;
            shown++;
            ctx.getSource().sendFeedback(new LiteralText(
                    "- " + item.id + " (" + item.trigger + "): " + item.cooldownSeconds + "s"
                            + (item.enabled ? "" : " [disabled]")));
        }
        if (matched == 0) {
            ctx.getSource().sendFeedback(new LiteralText("[CooldownTracker] No items matching '" + search + "'."));
        } else if (matched > shown) {
            ctx.getSource().sendFeedback(new LiteralText(
                    "[CooldownTracker] ...and " + (matched - shown) + " more matching '" + search + "'. Narrow your search to see them."));
        }
        return matched;
    }

    private static int runImport(CommandContext<FabricClientCommandSource> ctx, String fileName) {
        CsvImporter.Result result = CsvImporter.importFile(fileName);
        ctx.getSource().sendFeedback(new LiteralText(
                "[CooldownTracker] Imported " + result.imported + " item(s) from " + fileName
                        + (result.skipped > 0 ? ", skipped " + result.skipped : "") + "."));
        int shown = 0;
        for (String error : result.errors) {
            if (shown >= 5) {
                ctx.getSource().sendFeedback(new LiteralText("[CooldownTracker] ...and " + (result.errors.size() - shown) + " more issue(s)."));
                break;
            }
            ctx.getSource().sendFeedback(new LiteralText("[CooldownTracker] " + error));
            shown++;
        }
        return result.imported;
    }

    private static int runSetColor(CommandContext<FabricClientCommandSource> ctx, String id, String hex) {
        TrackedItem match = null;
        for (TrackedItem item : CooldownConfig.getItems()) {
            if (item.id.equalsIgnoreCase(id)) {
                match = item;
                break;
            }
        }
        if (match == null) {
            ctx.getSource().sendFeedback(new LiteralText("[CooldownTracker] No item with id '" + id + "'."));
            return 0;
        }

        String cleaned = hex.startsWith("#") ? hex.substring(1) : hex;
        if (cleaned.length() != 6 || !cleaned.matches("[0-9a-fA-F]{6}")) {
            ctx.getSource().sendFeedback(new LiteralText(
                    "[CooldownTracker] '" + hex + "' isn't a valid color - use a 6-digit hex code, e.g. FF8800 or #FF8800."));
            return 0;
        }

        match.color = cleaned;
        match.compilePattern();
        CooldownConfig.save();
        ctx.getSource().sendFeedback(new LiteralText("[CooldownTracker] '" + id + "' will now show as #" + cleaned + "."));
        return 1;
    }

    /**
     * Repairs the three built-in entries if they've been overwritten (e.g.
     * by an earlier /cooldowns additem "golden apple" or "totem") - forces
     * trigger back to the correct built-in type and clears the now-
     * irrelevant chat/name fields, but keeps whatever cooldownSeconds and
     * color you'd already set, since those are still correct.
     */
    private static int runFixBuiltins(CommandContext<FabricClientCommandSource> ctx) {
        String[][] builtins = {
                {"totem", "Totem of Undying"},
                {"golden_apple", "Golden Apple"},
                {"enchanted_golden_apple", "Enchanted Golden Apple"}
        };

        int fixed = 0;
        for (String[] builtin : builtins) {
            String id = builtin[0];
            String defaultDisplayName = builtin[1];
            TrackedItem item = CooldownConfig.findById(id);
            if (item == null) {
                item = new TrackedItem(id, defaultDisplayName, id, null, 0);
                CooldownConfig.addOrUpdate(item);
                fixed++;
                continue;
            }
            if (!id.equals(item.trigger)) {
                item.trigger = id;
                item.pattern = null;
                item.itemName = null;
                item.literalMatch = null;
                item.compilePattern();
                CooldownConfig.save();
                fixed++;
            }
        }

        ctx.getSource().sendFeedback(new LiteralText(
                fixed > 0
                        ? "[CooldownTracker] Fixed " + fixed + " built-in entr" + (fixed == 1 ? "y" : "ies") + "."
                        : "[CooldownTracker] Built-in entries already look correct."));
        return fixed;
    }

    private static String slugify(String name) {
        String slug = name.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return slug.isEmpty() ? "item_" + Integer.toHexString(name.hashCode()) : slug;
    }
}
