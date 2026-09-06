package com.tomcraft.cooldowntracker.listener;

import com.tomcraft.cooldowntracker.config.TextMatch;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

import java.util.HashSet;
import java.util.Set;

/**
 * Some servers broadcast a public chat line whenever ANY player uses an
 * ability (e.g. "Lightning Crash I | BaconEggAnCheese summoned 5 lightning
 * bolts!"), not just your own. Since matching is purely text-based, that
 * line contains the item name just like your own activation message would,
 * and would otherwise wrongly start YOUR cooldown for something someone
 * else used.
 *
 * This keeps a refreshed set of currently-online player names (from the
 * tab list, excluding yourself) - if a chat line mentions any of them, it's
 * treated as someone else's activation and skipped before item matching.
 */
public class OnlinePlayerCache {

    private static Set<String> otherPlayerNamesLower = new HashSet<>();
    private static long lastRefresh = 0;
    private static final long REFRESH_INTERVAL_MS = 2000;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            long now = System.currentTimeMillis();
            if (now - lastRefresh < REFRESH_INTERVAL_MS) return;
            lastRefresh = now;
            refresh(client);
        });
    }

    private static void refresh(MinecraftClient client) {
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) return;

        String selfName = client.player.getGameProfile().getName();
        Set<String> names = new HashSet<>();
        for (PlayerListEntry entry : handler.getPlayerList()) {
            String name = entry.getProfile().getName();
            if (name != null && !name.equalsIgnoreCase(selfName)) {
                names.add(name.toLowerCase());
            }
        }
        otherPlayerNamesLower = names;
    }

    /**
     * @param lowercaseLine an already-lowercased chat line to check
     */
    public static boolean mentionsOtherPlayer(String lowercaseLine) {
        for (String name : otherPlayerNamesLower) {
            if (TextMatch.containsWholeWord(lowercaseLine, name)) {
                return true;
            }
        }
        return false;
    }
}
