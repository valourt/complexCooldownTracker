package com.tomcraft.cooldowntracker.listener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mood Swings announces its new mood via the action bar (the text above
 * the hotbar), not regular chat - e.g. "Mood Swings has made you feel
 * Lazy!". That's a different network message than normal chat, handled
 * separately in NetworkChatMixin.
 */
public class MoodSwingsTracker {

    private static final Pattern PATTERN = Pattern.compile("(?i)mood swings has made you feel ([a-z]+)!?");

    private static volatile String currentMood = null;

    public static void onActionBarLine(String line) {
        Matcher m = PATTERN.matcher(line);
        if (m.find()) {
            String mood = m.group(1);
            currentMood = mood.substring(0, 1).toUpperCase() + mood.substring(1).toLowerCase();
        }
    }

    public static String getCurrentMood() {
        return currentMood;
    }
}
