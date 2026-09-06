package com.tomcraft.cooldowntracker.hud;

import com.tomcraft.cooldowntracker.cooldown.CooldownManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Watches for items leaving the active-cooldown map (i.e. just became
 * ready) and queues a short-lived toast for CooldownHud to render.
 */
public class ReadyToastManager {

    public static final long DISPLAY_MILLIS = 2500;
    public static final long FADE_MILLIS = 500;

    public static class Toast {
        public final String text;
        private final long spawnTime;

        public Toast(String text) {
            this.text = text;
            this.spawnTime = System.currentTimeMillis();
        }

        public long age() {
            return System.currentTimeMillis() - spawnTime;
        }
    }

    private static Map<String, String> previousActiveNames = new HashMap<>();
    private static final List<Toast> toasts = new ArrayList<>();

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    private static void tick() {
        boolean nothingActiveNow = CooldownManager.getActive().isEmpty();
        boolean nothingActiveBefore = previousActiveNames.isEmpty();

        if (nothingActiveNow && nothingActiveBefore) {
            // Common case (no cooldowns running at all) - nothing to
            // compare, skip the snapshot allocation entirely. Toasts still
            // need to age out even with nothing active, so don't skip that.
            if (!toasts.isEmpty()) toasts.removeIf(t -> t.age() > DISPLAY_MILLIS);
            return;
        }

        CooldownManager.tickCleanup();

        for (Map.Entry<String, String> entry : previousActiveNames.entrySet()) {
            if (!CooldownManager.getActive().containsKey(entry.getKey())) {
                toasts.add(new Toast(CooldownBoxes.shortName(entry.getValue()) + " is ready!"));
            }
        }

        // Rebuild from what's ACTUALLY still active after cleanup, not the
        // pre-cleanup snapshot - using the pre-cleanup snapshot here left a
        // just-expired item in this map for one extra tick, which then got
        // detected as "newly removed" a second time and fired a duplicate
        // toast.
        Map<String, String> afterCleanup = new HashMap<>();
        for (Map.Entry<String, CooldownManager.Active> e : CooldownManager.getActive().entrySet()) {
            afterCleanup.put(e.getKey(), e.getValue().displayName);
        }
        previousActiveNames = afterCleanup;
        toasts.removeIf(t -> t.age() > DISPLAY_MILLIS);
    }

    public static List<Toast> getToasts() {
        return toasts;
    }
}
