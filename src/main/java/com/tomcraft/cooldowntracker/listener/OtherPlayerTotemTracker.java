package com.tomcraft.cooldowntracker.listener;

import com.tomcraft.cooldowntracker.config.CooldownConfig;
import com.tomcraft.cooldowntracker.config.TrackedItem;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Records when OTHER players (not you - your own totem is already tracked
 * via GameStatePoller) pop a totem, using the same vanilla entity-status
 * packet (byte 35) that triggers the totem-pop particle effect for nearby
 * players - that packet is broadcast to everyone nearby, not just the
 * totem's owner, so this works the same way regardless of who used it.
 *
 * The cooldown duration applied to other players is whatever you've
 * configured for your own "totem" entry, on the assumption this server
 * enforces the same cooldown length for everyone (typical for a factions
 * server's balance mechanic).
 */
public class OtherPlayerTotemTracker {

    private static final Map<String, Long> cooldownEndByPlayer = new LinkedHashMap<>();

    public static void onOtherPlayerTotemPop(String playerName) {
        TrackedItem totem = CooldownConfig.findById("totem");
        double seconds = (totem != null) ? totem.cooldownSeconds : 0;
        if (seconds <= 0) return; // no configured duration - nothing meaningful to show

        long endTime = System.currentTimeMillis() + (long) (seconds * 1000L);
        cooldownEndByPlayer.put(playerName, endTime);
    }

    public static Map<String, Long> getActiveEndTimes() {
        long now = System.currentTimeMillis();
        cooldownEndByPlayer.entrySet().removeIf(e -> e.getValue() <= now);
        return cooldownEndByPlayer;
    }
}
