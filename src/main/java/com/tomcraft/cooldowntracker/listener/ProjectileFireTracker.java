package com.tomcraft.cooldowntracker.listener;

import com.tomcraft.cooldowntracker.config.CooldownConfig;
import com.tomcraft.cooldowntracker.config.TextMatch;
import com.tomcraft.cooldowntracker.config.TrackedItem;
import com.tomcraft.cooldowntracker.cooldown.CooldownManager;
import com.tomcraft.cooldowntracker.hud.InventoryOwnershipScanner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;

/**
 * Detects items with trigger "projectile" - for items with no chat/action-
 * bar signal AND a vanilla item-cooldown sweep that's shared across
 * multiple different items (so unreliable as a trigger on its own), but
 * that DO fire a real, visible projectile.
 *
 * Called from NetworkChatMixin's onEntitySpawn injection whenever a
 * projectile entity spawns that's owned by the local player - projectile
 * entities carry real owner/shooter data synced to the client specifically
 * so it can answer "who fired this" (used for things like damage
 * attribution), which this repurposes as a fire-detection signal. At that
 * moment, whichever tracked "projectile" item is currently held (by name/
 * lore match) is credited - since only one item can be held at a time,
 * this ties the trigger to the literal moment of the shot rather than to
 * any shared cooldown state, sidestepping the "multiple items share one
 * base weapon" ambiguity that made the vanilla-cooldown-sweep approach
 * unreliable for this specific case.
 */
public class ProjectileFireTracker {

    public static void onOwnedProjectileSpawn(Entity entity) {
        if (!(entity instanceof ProjectileEntity)) return;

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) return;

        Entity owner = ((ProjectileEntity) entity).getOwner();
        if (owner != player) return;

        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();

        for (TrackedItem item : CooldownConfig.getItems()) {
            if (!item.enabled || !"projectile".equals(item.trigger) || item.itemNameNormalized == null) {
                continue;
            }
            if (matches(mainHand, item) || matches(offHand, item)) {
                CooldownManager.start(item.id, item.displayName, item.cooldownSeconds);
                return; // exactly one tracked item should match whatever's held
            }
        }
    }

    private static boolean matches(ItemStack stack, TrackedItem item) {
        if (stack == null || stack.isEmpty()) return false;
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
