package com.tomcraft.cooldowntracker.hud;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the currently-equipped backpack's capacity straight from its item
 * name (e.g. "T1 Vkit Backpack Lvl. 1 | 28/500") - equipped means held in
 * the offhand, per this server's own item description. This is far more
 * reliable than trying to catch the fleeting pickup notification, since the
 * name is always readable, not just momentarily shown.
 */
public class BackpackTracker {

    private static final int SCAN_INTERVAL_TICKS = 10; // twice a second
    private static int tickCounter = 0;

    // Matches the "current/max" capacity pair anywhere in the item name -
    // specific enough not to accidentally match something like "Lvl. 1"
    // (no slash there), general enough to survive wording changes.
    private static final Pattern CAPACITY_PATTERN = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");

    private static Integer current = null;
    private static Integer max = null;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            tickCounter++;
            if (tickCounter < SCAN_INTERVAL_TICKS) return;
            tickCounter = 0;
            scan(client);
        });
    }

    private static void scan(MinecraftClient client) {
        ItemStack offhand = client.player.getOffHandStack();
        if (offhand.isEmpty()) {
            current = null;
            max = null;
            return;
        }

        String name = offhand.getName().getString();
        Matcher matcher = CAPACITY_PATTERN.matcher(name);
        if (matcher.find()) {
            try {
                current = Integer.parseInt(matcher.group(1));
                max = Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                current = null;
                max = null;
            }
        } else {
            current = null;
            max = null;
        }
    }

    public static boolean hasBackpack() {
        return current != null && max != null;
    }

    public static int getCurrent() {
        return current == null ? 0 : current;
    }

    public static int getMax() {
        return max == null ? 0 : max;
    }

    public static float getFullness() {
        if (max == null || max == 0) return 0f;
        return (float) getCurrent() / max;
    }
}
