package com.tomcraft.cooldowntracker.hud;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

/**
 * Resistance is a real vanilla status effect, so unlike most of this mod's
 * trackers there's no guessing involved here at all - just a direct,
 * reliable read of LivingEntity.getStatusEffect(). Purpose: give a clear,
 * hard-to-miss visual so you don't waste a golden/enchanted golden apple
 * while you're already resistant from a previous one (or anything else
 * that grants Resistance).
 */
public class ResistanceTracker {

    private static Integer amplifier = null; // 0 = Resistance I, 1 = II, etc.
    private static int durationTicks = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            StatusEffectInstance instance = client.player.getStatusEffect(StatusEffects.RESISTANCE);
            if (instance != null) {
                amplifier = instance.getAmplifier();
                durationTicks = instance.getDuration();
            } else {
                amplifier = null;
                durationTicks = 0;
            }
        });
    }

    public static boolean isActive() {
        return amplifier != null;
    }

    public static int getAmplifier() {
        return amplifier == null ? 0 : amplifier;
    }

    public static long getRemainingMillis() {
        return durationTicks * 50L; // 20 ticks/sec = 50ms/tick
    }

    private static final String[] ROMAN = {"I", "II", "III", "IV", "V"};

    public static String getTierRoman() {
        int a = getAmplifier();
        return a >= 0 && a < ROMAN.length ? ROMAN[a] : String.valueOf(a + 1);
    }
}
