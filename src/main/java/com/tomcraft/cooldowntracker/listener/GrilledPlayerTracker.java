package com.tomcraft.cooldowntracker.listener;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Barbeque announces who it hit directly in chat, e.g. "Barbeque | Grilled
 * MrBanana2492!" - parsed straight from the message, same as the Snake
 * Eyes buff. Grill duration is a fixed 5 seconds per the item's own
 * description, not something read from your own tracked-item config
 * (unlike totem/apples) since this isn't about your own cooldown at all -
 * it's about who's currently affected by an ability you just used on them.
 */
public class GrilledPlayerTracker {

    private static final Pattern PATTERN = Pattern.compile("(?i)grilled ([A-Za-z0-9_]+)!");
    private static final long GRILL_DURATION_MILLIS = 5000L;

    private static final Map<String, Long> grilledEndByPlayer = new LinkedHashMap<>();

    public static void onChatLine(String line) {
        Matcher m = PATTERN.matcher(line);
        if (m.find()) {
            String playerName = m.group(1);
            grilledEndByPlayer.put(playerName, System.currentTimeMillis() + GRILL_DURATION_MILLIS);
        }
    }

    public static Map<String, Long> getActiveEndTimes() {
        long now = System.currentTimeMillis();
        grilledEndByPlayer.entrySet().removeIf(e -> e.getValue() <= now);
        return grilledEndByPlayer;
    }
}
