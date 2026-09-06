package com.tomcraft.cooldowntracker.hud;

import com.tomcraft.cooldowntracker.config.CooldownConfig;
import com.tomcraft.cooldowntracker.config.TrackedItem;
import com.tomcraft.cooldowntracker.cooldown.CooldownManager;
import net.minecraft.client.font.TextRenderer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Builds the Ready / On Cooldown line lists and works out each box's
 * multi-column wrapped layout (up to MAX_ROWS_PER_COLUMN rows per column,
 * wrapping into a new column as needed) - shared by the real HUD renderer
 * and the drag-to-reposition edit screen so both always agree on size.
 * Sizes returned here are UNSCALED (as if scale = 1.0) - the caller
 * applies scale via a matrix transform.
 */
public class CooldownBoxes {

    public static final int PADDING = 6;
    public static final int LINE_HEIGHT = 11;
    public static final int COLUMN_GAP = 14;
    public static final int MARKER_SIZE = 4;
    public static final int MARKER_GAP = 4;
    public static final int MAX_ROWS_PER_COLUMN = 5;

    public static final int MIN_SCALE_PERCENT = 50;
    public static final int MAX_SCALE_PERCENT = 300;

    public static final int READY_DEFAULT_COLOR = 0xFFFFFFFF;

    public static class Line {
        public final String text;
        public final int color;
        /** Name-portion length in characters, for split-color rendering - equals text.length() when there's no separate suffix (Ready lines). */
        public final int nameLength;
        public final int suffixColor;

        /** Ready lines: no separate suffix, whole text uses one color. */
        public Line(String text, int color) {
            this.text = text;
            this.color = color;
            this.nameLength = text.length();
            this.suffixColor = color;
        }

        /**
         * Cooldown lines: name and countdown suffix can have different
         * colors - a custom color (if set) should only tint the name, the
         * countdown always follows the automatic urgency coloring.
         */
        public Line(String name, int nameColor, String suffix, int suffixColor) {
            this.text = name + suffix;
            this.color = nameColor;
            this.nameLength = name.length();
            this.suffixColor = suffixColor;
        }
    }

    /**
     * Strips a trailing Roman-numeral tier (e.g. "Illusion V" -> "Illusion")
     * for display only - the underlying id/pattern/itemName keep the exact
     * tier so matching and cooldown durations are unaffected. Anchored to
     * the end of the string, so which alternative "wins" doesn't matter -
     * only a token that reaches all the way to the end can match at all.
     */
    private static final Pattern TIER_SUFFIX = Pattern.compile("(?i)\\s+(I|II|III|IV|V|VI|VII|VIII|IX|X)$");

    public static String shortName(String name) {
        return name == null ? "" : TIER_SUFFIX.matcher(name).replaceAll("");
    }

    private static long lastComputeTime = 0;
    private static List<Line> cachedCooldownLines = new ArrayList<>();
    private static List<Line> cachedReadyLines = new ArrayList<>();
    private static List<Line> cachedOtherTotemLines = new ArrayList<>();
    private static final long RECOMPUTE_INTERVAL_MS = 200; // 5x/sec is plenty for a mm:ss display

    public static List<Line> getCooldownLines() {
        recomputeIfNeeded();
        return cachedCooldownLines;
    }

    public static List<Line> getReadyLines() {
        recomputeIfNeeded();
        return cachedReadyLines;
    }

    public static List<Line> getOtherTotemLines() {
        recomputeIfNeeded();
        return cachedOtherTotemLines;
    }

    private static void recomputeIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastComputeTime < RECOMPUTE_INTERVAL_MS) return;
        lastComputeTime = now;

        CooldownManager.tickCleanup();
        cachedCooldownLines = CooldownManager.getActive().entrySet().stream()
                .sorted(Comparator.comparingLong(e -> e.getValue().remainingMillis()))
                .map(e -> {
                    CooldownManager.Active active = e.getValue();
                    TrackedItem source = CooldownConfig.findById(e.getKey());
                    int urgency = urgencyColor(active.remainingMillis());
                    int nameColor = (source != null && source.colorArgb != null) ? source.colorArgb : urgency;
                    String name = shortName(active.displayName);
                    String suffix = " " + formatTime(active.remainingMillis());
                    return new Line(name, nameColor, suffix, urgency);
                })
                .collect(java.util.stream.Collectors.toList());

        List<Line> ready = new ArrayList<>();
        for (TrackedItem item : CooldownConfig.getItems()) {
            if (!item.enabled) continue;
            if (!InventoryOwnershipScanner.isOwned(item.id)) continue;
            if (CooldownManager.getActive().containsKey(item.id)) continue;
            int color = item.colorArgb != null ? item.colorArgb : READY_DEFAULT_COLOR;
            ready.add(new Line(shortName(item.displayName), color));
        }
        ready.sort(Comparator.comparing(l -> l.text.toLowerCase()));
        cachedReadyLines = ready;

        List<Line> otherTotems = new ArrayList<>();
        long now2 = System.currentTimeMillis();
        java.util.Map<String, Long> otherActive = com.tomcraft.cooldowntracker.listener.OtherPlayerTotemTracker.getActiveEndTimes();
        otherActive.entrySet().stream()
                .sorted(Comparator.comparingLong(java.util.Map.Entry::getValue))
                .forEach(e -> {
                    long remaining = Math.max(0, e.getValue() - now2);
                    int color = urgencyColor(remaining);
                    otherTotems.add(new Line(e.getKey(), color, " " + formatTime(remaining), color));
                });
        cachedOtherTotemLines = otherTotems;
    }

    private static int urgencyColor(long remainingMillis) {
        if (remainingMillis < 3000) return 0xFFFF5C5C;   // red - about to be ready
        if (remainingMillis < 10000) return 0xFFFFC94D;  // amber - almost ready
        return 0xFFE8E8E8;                                // light grey - plenty left
    }

    public static String formatTime(long millis) {
        long totalSeconds = (millis + 999) / 1000; // round up so it doesn't flash "0s"
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Multi-column wrapped layout for one box: up to MAX_ROWS_PER_COLUMN
     * items per column, additional columns added to the right as needed.
     * Each column is only as wide as its own widest entry. Collapses to a
     * tiny single line when there's nothing to show at all.
     *
     * @return {totalWidth, totalHeight, columnCount}
     */
    public static int[] measure(TextRenderer tr, String title, List<Line> lines) {
        if (lines.isEmpty()) {
            int compactWidth = tr.getWidth(CooldownFont.styled(title + " \u2014", CooldownFont.BOLD)) + PADDING * 2;
            int compactHeight = LINE_HEIGHT + PADDING * 2;
            return new int[]{compactWidth, compactHeight, 0};
        }

        int columns = (int) Math.ceil(lines.size() / (double) MAX_ROWS_PER_COLUMN);
        int[] colWidths = columnWidths(tr, lines, columns);
        int titleWidth = tr.getWidth(CooldownFont.styled(title, CooldownFont.BOLD));

        int maxRows = 0;
        for (int c = 0; c < columns; c++) {
            int start = c * MAX_ROWS_PER_COLUMN;
            int end = Math.min(start + MAX_ROWS_PER_COLUMN, lines.size());
            maxRows = Math.max(maxRows, end - start);
        }

        int contentWidth = 0;
        for (int c = 0; c < columns; c++) {
            contentWidth += colWidths[c];
            if (c > 0) contentWidth += COLUMN_GAP;
        }
        contentWidth = Math.max(contentWidth, titleWidth);

        int headerHeight = PADDING + LINE_HEIGHT;
        int width = PADDING * 2 + contentWidth;
        int height = headerHeight + maxRows * LINE_HEIGHT + PADDING;
        return new int[]{width, height, columns};
    }

    public static int[] columnWidths(TextRenderer tr, List<Line> lines, int columns) {
        int[] widths = new int[columns];
        for (int i = 0; i < lines.size(); i++) {
            int col = i / MAX_ROWS_PER_COLUMN;
            int w = MARKER_SIZE + MARKER_GAP + tr.getWidth(CooldownFont.styled(lines.get(i).text, CooldownFont.REGULAR));
            widths[col] = Math.max(widths[col], w);
        }
        return widths;
    }

    public static float clampScale(float scale) {
        float min = MIN_SCALE_PERCENT / 100f;
        float max = MAX_SCALE_PERCENT / 100f;
        return Math.max(min, Math.min(max, scale));
    }
}
