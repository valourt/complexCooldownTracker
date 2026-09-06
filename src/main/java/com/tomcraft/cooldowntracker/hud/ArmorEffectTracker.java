package com.tomcraft.cooldowntracker.hud;

import com.tomcraft.cooldowntracker.config.TextMatch;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * Checks whether the Snake Eyes helmet / Mood Swings boots are currently
 * worn, so their HUD boxes can auto-hide when not equipped, the same way
 * the Backpack box does. The actual buff/mood values themselves come from
 * chat/action-bar parsing (SnakeEyesTracker/MoodSwingsTracker) - this
 * class only tracks whether the relevant armor piece is being worn at all.
 *
 * Checks lore lines as well as the item's own display name - like the
 * rune abilities tracked elsewhere in this mod, these ability names
 * commonly live in an item's lore rather than its actual name.
 */
public class ArmorEffectTracker {

    private static final int SCAN_INTERVAL_TICKS = 10; // twice a second
    private static int tickCounter = 0;

    private static volatile boolean wearingSnakeEyes = false;
    private static volatile boolean wearingMoodSwings = false;

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
        ItemStack helmet = client.player.getEquippedStack(EquipmentSlot.HEAD);
        wearingSnakeEyes = matches(helmet, "snake eyes");

        ItemStack boots = client.player.getEquippedStack(EquipmentSlot.FEET);
        wearingMoodSwings = matches(boots, "mood swings");
    }

    private static boolean matches(ItemStack stack, String needle) {
        if (stack.isEmpty()) return false;
        if (TextMatch.containsWholeWord(TextMatch.normalize(stack.getName().getString()), needle)) {
            return true;
        }
        for (String loreLine : InventoryOwnershipScanner.getLoreLines(stack)) {
            if (TextMatch.containsWholeWord(TextMatch.normalize(loreLine), needle)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWearingSnakeEyes() {
        return wearingSnakeEyes;
    }

    public static boolean isWearingMoodSwings() {
        return wearingMoodSwings;
    }
}

