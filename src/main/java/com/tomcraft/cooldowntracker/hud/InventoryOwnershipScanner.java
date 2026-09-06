package com.tomcraft.cooldowntracker.hud;

import com.tomcraft.cooldowntracker.config.CooldownConfig;
import com.tomcraft.cooldowntracker.config.TextMatch;
import com.tomcraft.cooldowntracker.config.TrackedItem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Every few ticks, scans the player's inventory (main + armor + offhand) and
 * records which tracked items are currently owned - "chat" trigger items are
 * matched by exact custom ability name (checked against both the item's own
 * display name AND its lore lines, since these servers commonly show the
 * ability name in lore rather than as the item's actual name), the built-in
 * triggers (totem/apples) by their real Minecraft item id.
 *
 * The expensive parts here - lore JSON parsing per slot, and matching
 * against every one of 500+ tracked items - only need to happen when the
 * inventory has actually changed. A cheap fingerprint check up front skips
 * the whole thing on every scan where nothing moved, which is the vast
 * majority of the time during normal play.
 */
public class InventoryOwnershipScanner {

    private static final int SCAN_INTERVAL_TICKS = 10; // twice a second
    private static int tickCounter = 0;

    private static Set<String> ownedIds = new HashSet<>();
    private static int lastFingerprint = 0;
    private static boolean initialized = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            tickCounter++;
            if (tickCounter < SCAN_INTERVAL_TICKS) return;
            tickCounter = 0;
            scan(client);
        });
    }

    public static boolean isOwned(String id) {
        return ownedIds.contains(id);
    }

    private static void scan(MinecraftClient client) {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.addAll(client.player.getInventory().main);
        stacks.addAll(client.player.getInventory().armor);
        stacks.add(client.player.getInventory().offHand.get(0));

        int fingerprint = fingerprint(stacks);
        if (initialized && fingerprint == lastFingerprint) {
            return; // nothing changed since last scan - skip all the expensive work below
        }
        lastFingerprint = fingerprint;
        initialized = true;

        List<String> namesInInventory = new ArrayList<>();
        boolean hasTotem = false;
        boolean hasGoldenApple = false;
        boolean hasEnchantedGoldenApple = false;

        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;

            namesInInventory.add(TextMatch.normalize(stack.getName().getString()));
            for (String loreLine : getLoreLines(stack)) {
                namesInInventory.add(TextMatch.normalize(loreLine));
            }

            if (stack.isOf(Items.TOTEM_OF_UNDYING)) hasTotem = true;
            if (stack.isOf(Items.GOLDEN_APPLE)) hasGoldenApple = true;
            if (stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) hasEnchantedGoldenApple = true;
        }

        Set<String> newOwned = new HashSet<>();
        for (TrackedItem item : CooldownConfig.getItems()) {
            if (!item.enabled) continue;

            String trigger = item.trigger == null ? "" : item.trigger;
            if (trigger.equals("totem")) {
                if (hasTotem) newOwned.add(item.id);
            } else if (trigger.equals("golden_apple")) {
                if (hasGoldenApple) newOwned.add(item.id);
            } else if (trigger.equals("enchanted_golden_apple")) {
                if (hasEnchantedGoldenApple) newOwned.add(item.id);
            } else if (item.itemNameNormalized != null) {
                for (String haystack : namesInInventory) {
                    if (TextMatch.containsWholeWord(haystack, item.itemNameNormalized)) {
                        newOwned.add(item.id);
                        break;
                    }
                }
            }
        }

        ownedIds = newOwned;
    }

    /**
     * Cheap per-slot hash combining item identity, count, and NBT content
     * (lore lives in NBT, so a lore change shows up here too) - if this
     * doesn't change between scans, nothing the rest of this class cares
     * about has changed either.
     */
    private static int fingerprint(List<ItemStack> stacks) {
        int hash = 1;
        for (ItemStack stack : stacks) {
            int slotHash;
            if (stack == null || stack.isEmpty()) {
                slotHash = 0;
            } else {
                int nbtHash = stack.getNbt() != null ? stack.getNbt().hashCode() : 0;
                slotHash = stack.getItem().hashCode() * 31 + stack.getCount() * 17 + nbtHash;
            }
            hash = hash * 31 + slotHash;
        }
        return hash;
    }

    /**
     * Returns the plain text of each lore line on the item (the flavor text
     * shown below the item name in the tooltip), where custom ability names
     * commonly live on servers that use lore-based "fake enchantments"
     * rather than the item's own display name. Package-visible so
     * ArmorEffectTracker can reuse this instead of duplicating it.
     */
    static List<String> getLoreLines(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        NbtCompound display = stack.getSubNbt("display");
        if (display == null || !display.contains("Lore", NbtElement.LIST_TYPE)) {
            return lines;
        }
        NbtList lore = display.getList("Lore", NbtElement.STRING_TYPE);
        for (int i = 0; i < lore.size(); i++) {
            String json = lore.getString(i);
            try {
                Text text = Text.Serializer.fromJson(json);
                lines.add(text != null ? text.getString() : json);
            } catch (Exception e) {
                lines.add(json);
            }
        }
        return lines;
    }
}
