package com.tomcraft.cooldowntracker.cooldown;

import java.util.LinkedHashMap;
import java.util.Map;

public class CooldownManager {

    public static class Active {
        public final String displayName;
        public final long endTimeMillis;
        public final long totalDurationMillis;

        public Active(String displayName, long endTimeMillis, long totalDurationMillis) {
            this.displayName = displayName;
            this.endTimeMillis = endTimeMillis;
            this.totalDurationMillis = totalDurationMillis;
        }

        public long remainingMillis() {
            return Math.max(0, endTimeMillis - System.currentTimeMillis());
        }

        public double progress() {
            if (totalDurationMillis <= 0) return 1.0;
            long elapsed = totalDurationMillis - remainingMillis();
            return Math.min(1.0, Math.max(0.0, (double) elapsed / totalDurationMillis));
        }
    }

    private static final Map<String, Active> ACTIVE = new LinkedHashMap<>();

    public static void start(String id, String displayName, double cooldownSeconds) {
        if (cooldownSeconds <= 0) return; // nothing to display
        long durationMillis = (long) (cooldownSeconds * 1000L);
        long end = System.currentTimeMillis() + durationMillis;
        ACTIVE.put(id, new Active(displayName, end, durationMillis));
    }

    public static void clear(String id) {
        ACTIVE.remove(id);
    }

    public static void tickCleanup() {
        ACTIVE.entrySet().removeIf(e -> e.getValue().remainingMillis() <= 0);
    }

    public static Map<String, Active> getActive() {
        return ACTIVE;
    }
}
