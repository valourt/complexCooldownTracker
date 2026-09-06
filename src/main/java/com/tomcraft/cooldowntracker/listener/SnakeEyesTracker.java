package com.tomcraft.cooldowntracker.listener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Snake Eyes announces its roll result directly in chat with the exact
 * buff name, tier, and duration, e.g. "Snake Eyes V | You rolled a 6,
 * granting you Weakness III for 10 seconds!" - no guessing needed, this
 * is parsed straight from the message text.
 */
public class SnakeEyesTracker {

    private static final Pattern PATTERN =
            Pattern.compile("(?i)granting you ([a-z ]+?) ([ivxlcdm]+) for (\\d+) seconds");

    private static volatile String currentBuff = null;
    private static volatile long endTimeMillis = 0;

    public static void onChatLine(String line) {
        Matcher m = PATTERN.matcher(line);
        if (m.find()) {
            String buffName = m.group(1).trim();
            String tier = m.group(2).trim().toUpperCase();
            int seconds = Integer.parseInt(m.group(3));
            currentBuff = buffName + " " + tier;
            endTimeMillis = System.currentTimeMillis() + seconds * 1000L;
        }
    }

    public static boolean hasActiveBuff() {
        return currentBuff != null && System.currentTimeMillis() < endTimeMillis;
    }

    public static String getCurrentBuff() {
        return currentBuff;
    }

    public static long getRemainingMillis() {
        return Math.max(0, endTimeMillis - System.currentTimeMillis());
    }
}
