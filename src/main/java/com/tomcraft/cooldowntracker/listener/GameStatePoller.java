package com.tomcraft.cooldowntracker.listener;

import com.tomcraft.cooldowntracker.config.CooldownConfig;
import com.tomcraft.cooldowntracker.config.TrackedItem;
import com.tomcraft.cooldowntracker.cooldown.CooldownManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Detects totem-of-undying activation and golden/enchanted golden apple
 * consumption by polling public player state every tick, rather than
 * mixin-injecting into vanilla classes. This uses only Fabric API's
 * ClientTickEvents (proven to work in your setup, since it's what drives
 * the HUD and inventory scanner already) plus ordinary getters.
 *
 * Totem: vanilla sets health to exactly 1.0 and grants Absorption II
 * (8.0 absorption points) when a totem activates - these are applied via
 * separate update paths that don't always land in the same client tick
 * under normal network jitter, so requiring both simultaneously (as an
 * earlier version of this did) could miss real activations entirely.
 * Firing on whichever signal arrives first, with a short debounce so the
 * second signal (when it does land nearby) doesn't double-count the same
 * event, is far more reliable in practice.
 *
 * Apples: detected by watching isUsingItem()/getActiveItem() transition
 * from "using a golden apple" to "not using" (i.e. it just finished).
 */
public class GameStatePoller {

    private static float lastHealth = -1;
    private static float lastAbsorption = -1;
    private static boolean lastUsingItem = false;
    private static Item lastActiveItem = null;

    private static long lastTotemFireTime = 0;
    private static final long TOTEM_DEBOUNCE_MS = 1000;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(GameStatePoller::tick);
    }

    private static void tick(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null) return;

        float health = player.getHealth();
        float absorption = player.getAbsorptionAmount();
        boolean healthSignal = (health == 1.0f && lastHealth != 1.0f);
        boolean absorptionSignal = (absorption == 8.0f && lastAbsorption != 8.0f);
        if (healthSignal || absorptionSignal) {
            long now = System.currentTimeMillis();
            if (now - lastTotemFireTime > TOTEM_DEBOUNCE_MS) {
                fireTrigger("totem");
                lastTotemFireTime = now;
            }
        }
        lastHealth = health;
        lastAbsorption = absorption;

        boolean usingItem = player.isUsingItem();
        if (lastUsingItem && !usingItem) {
            if (lastActiveItem == Items.ENCHANTED_GOLDEN_APPLE) {
                fireTrigger("enchanted_golden_apple");
            } else if (lastActiveItem == Items.GOLDEN_APPLE) {
                fireTrigger("golden_apple");
            }
        }
        lastUsingItem = usingItem;
        if (usingItem) {
            ItemStack activeStack = player.getActiveItem();
            lastActiveItem = activeStack.isEmpty() ? null : activeStack.getItem();
        } else {
            lastActiveItem = null;
        }
    }

    private static void fireTrigger(String trigger) {
        for (TrackedItem item : CooldownConfig.getItems()) {
            if (item.enabled && trigger.equals(item.trigger)) {
                CooldownManager.start(item.id, item.displayName, item.cooldownSeconds);
            }
        }
    }
}
