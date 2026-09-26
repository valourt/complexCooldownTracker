package com.tomcraft.cooldowntracker.listener;

import com.tomcraft.cooldowntracker.config.CooldownConfig;
import com.tomcraft.cooldowntracker.config.TextMatch;
import com.tomcraft.cooldowntracker.config.TrackedItem;
import com.tomcraft.cooldowntracker.cooldown.CooldownManager;
import com.tomcraft.cooldowntracker.hud.InventoryOwnershipScanner;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Detects items with trigger "vanilla_cooldown" - ones with no chat or
 * action-bar signal at all, but that use vanilla's own built-in item
 * cooldown system (the same one ender pearls use, shown as a diagonal grey
 * sweep on the hotbar slot). Fires the moment that sweep starts, using YOUR
 * configured cooldownSeconds - we don't need the game to tell us the
 * duration, just the moment it begins.
 *
 * Vanilla tracks this cooldown per raw Item type (e.g. "any crossbow"), not
 * per custom NBT variant - if several different tracked items are all
 * reskinned crossbows, they all share that same underlying cooldown state.
 * Naively checking "is whatever I'm currently holding on cooldown" would
 * misfire: swap to a second crossbow-based rune while the first one is
 * still cooling down, and it would look like the second one just fired
 * too. Instead, this watches for the cooldown's actual start transition on
 * the underlying Item type itself, and attributes it to whichever matching
 * tracked item was ACTUALLY held at that exact moment - since only one
 * item can be held at a time, that's a reliable way to tell which specific
 * rune caused a cooldown that vanilla itself can't distinguish.
 */
public class VanillaCooldownPoller {

    private static final Map<Item, Boolean> wasCoolingDownByBaseItem = new HashMap<>();

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(VanillaCooldownPoller::tick);
    }

    private static void tick(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null) return;

        checkHand(player, player.getMainHandStack());
        checkHand(player, player.getOffHandStack());
    }

    private static void checkHand(PlayerEntity player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        Item baseItem = stack.getItem();
        boolean coolingDown = player.getItemCooldownManager().isCoolingDown(baseItem);
        Boolean previous = wasCoolingDownByBaseItem.put(baseItem, coolingDown);
        boolean risingEdge = coolingDown && (previous == null || !previous);

        if (!risingEdge) return;

        for (TrackedItem item : CooldownConfig.getItems()) {
            if (!item.enabled || !"vanilla_cooldown".equals(item.trigger) || item.itemNameNormalized == null) {
                continue;
            }
            if (matches(stack, item)) {
                CooldownManager.start(item.id, item.displayName, item.cooldownSeconds);
                return; // exactly one tracked item should match a given held stack
            }
        }
    }

    private static boolean matches(ItemStack stack, TrackedItem item) {
        if (TextMatch.containsWholeWord(TextMatch.normalize(stack.getName().getString()), item.itemNameNormalized)) {
            return true;
        }
        for (String loreLine : InventoryOwnershipScanner.getLoreLines(stack)) {
            if (TextMatch.containsWholeWord(TextMatch.normalize(loreLine), item.itemNameNormalized)) {
                return true;
            }
        }
        return false;
    }
}

