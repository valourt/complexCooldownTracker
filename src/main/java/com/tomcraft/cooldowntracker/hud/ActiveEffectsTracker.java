package com.tomcraft.cooldowntracker.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists every currently active vanilla status effect (Resistance, Speed,
 * Strength, poison, whatever's active) with its tier and remaining time.
 * Unlike most of this mod's trackers there's no guessing involved here at
 * all - LivingEntity.getStatusEffects() is a direct, reliable read of real
 * game state, not something parsed from chat or inferred from a shared
 * cooldown flag. Originally built as a Resistance-only box (so you don't
 * waste a golden apple while already resistant), generalized to show
 * everything since that's more broadly useful and was simple to do once
 * the same underlying API is being read anyway.
 */
public class ActiveEffectsTracker {

    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    public static List<CooldownBoxes.Line> getEffectLines() {
        List<CooldownBoxes.Line> lines = new ArrayList<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return lines;

        for (StatusEffectInstance instance : client.player.getStatusEffects()) {
            String name = instance.getEffectType().getName().getString();
            String tier = tierRoman(instance.getAmplifier());
            long remainingMillis = instance.getDuration() * 50L; // 20 ticks/sec = 50ms/tick
            int color = instance.getEffectType().isBeneficial() ? 0xFF7CFC98 : 0xFFFF6B6B;
            lines.add(new CooldownBoxes.Line(name + " " + tier, color,
                    " " + CooldownBoxes.formatTime(remainingMillis), color));
        }

        return lines;
    }

    private static String tierRoman(int amplifier) {
        return amplifier >= 0 && amplifier < ROMAN.length ? ROMAN[amplifier] : String.valueOf(amplifier + 1);
    }
}
