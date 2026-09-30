package com.tomcraft.cooldowntracker.hud;

import com.tomcraft.cooldowntracker.listener.MoodSwingsTracker;
import com.tomcraft.cooldowntracker.listener.SnakeEyesTracker;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

import java.util.List;

/**
 * Draws the two separate boxes (Ready, On Cooldown), the backpack capacity
 * box, and the ready pop-up toasts. Position/scale/visibility/background
 * color all come from HudLayoutConfig, changed via HudEditScreen, the
 * toggle keybinds/commands, or the setbgcolor/setopacity commands.
 */
public class CooldownHud {

    private static final int SHADOW_COLOR = 0x30000000;
    private static final int BORDER_COLOR = 0xB0D0D0D8;
    private static final int EMPTY_COLOR = 0xFF7D7D82;
    private static final int READY_TITLE_COLOR = 0xFFA9F5C4;
    private static final int COOLDOWN_TITLE_COLOR = 0xFFFFC49A;
    private static final int BACKPACK_TITLE_COLOR = 0xFFC7B8F5;
    private static final int TOTEM_WATCH_TITLE_COLOR = 0xFF8FD6FF;
    private static final int SNAKE_EYES_TITLE_COLOR = 0xFFFF9E9E;
    private static final int MOOD_SWINGS_TITLE_COLOR = 0xFFB8F5C7;
    private static final int GRILLED_TITLE_COLOR = 0xFFFFB870;
    private static final int POTIONS_TITLE_COLOR = 0xFFFF5C5C;

    public static void register() {
        HudRenderCallback.EVENT.register(CooldownHud::render);
    }

    private static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (client.currentScreen != null) return;

        HudLayoutConfig.Layout layout = HudLayoutConfig.get();

        if (layout.readyBoxVisible) {
            drawReadyList(matrices, client, CooldownBoxes.getReadyLines(),
                    layout.readyBoxX, layout.readyBoxY, layout.readyBoxScale, false);
        }
        if (layout.cooldownBoxVisible) {
            drawBox(matrices, client, "On Cooldown", CooldownBoxes.getCooldownLines(),
                    layout.cooldownBoxX, layout.cooldownBoxY, layout.cooldownBoxScale, COOLDOWN_TITLE_COLOR, layout);
        }
        if (layout.backpackBoxVisible && BackpackTracker.hasBackpack()) {
            drawBackpackBox(matrices, client, layout.backpackBoxX, layout.backpackBoxY, layout.backpackBoxScale, layout);
        }
        if (layout.totemWatchBoxVisible) {
            drawBox(matrices, client, "Totem Watch", CooldownBoxes.getOtherTotemLines(),
                    layout.totemWatchBoxX, layout.totemWatchBoxY, layout.totemWatchBoxScale, TOTEM_WATCH_TITLE_COLOR, layout);
        }
        if (layout.snakeEyesBoxVisible && ArmorEffectTracker.isWearingSnakeEyes()) {
            drawSnakeEyesBox(matrices, client, layout.snakeEyesBoxX, layout.snakeEyesBoxY, layout.snakeEyesBoxScale, layout);
        }
        if (layout.moodSwingsBoxVisible && ArmorEffectTracker.isWearingMoodSwings()) {
            drawMoodSwingsBox(matrices, client, layout.moodSwingsBoxX, layout.moodSwingsBoxY, layout.moodSwingsBoxScale, layout);
        }
        if (layout.grilledBoxVisible) {
            drawBox(matrices, client, "Grilled", CooldownBoxes.getGrilledLines(),
                    layout.grilledBoxX, layout.grilledBoxY, layout.grilledBoxScale, GRILLED_TITLE_COLOR, layout);
        }
        if (layout.potionsBoxVisible) {
            drawBox(matrices, client, "Potions", ActiveEffectsTracker.getEffectLines(),
                    layout.potionsBoxX, layout.potionsBoxY, layout.potionsBoxScale, POTIONS_TITLE_COLOR, layout);
        }

        if (layout.toastVisible) {
            renderToasts(matrices, client, layout);
        }
    }

    private static void renderToasts(MatrixStack matrices, MinecraftClient client, HudLayoutConfig.Layout layout) {
        List<ReadyToastManager.Toast> toasts = ReadyToastManager.getToasts();
        if (toasts.isEmpty()) return;

        int y = layout.toastY;
        int markerSize = 5;
        int innerPad = 9;

        for (ReadyToastManager.Toast toast : toasts) {
            long age = toast.age();
            float alpha = 1f;
            long fadeStart = ReadyToastManager.DISPLAY_MILLIS - ReadyToastManager.FADE_MILLIS;
            if (age > fadeStart) {
                alpha = Math.max(0f, 1f - (float) (age - fadeStart) / ReadyToastManager.FADE_MILLIS);
            }
            int a = (int) (alpha * 255) & 0xFF;
            int bg = (a << 24) | 0x16211B;
            int marker = (a << 24) | 0x57E389;
            int text = (a << 24) | 0xFFFFFF;

            var styledText = CooldownFont.styled(toast.text, CooldownFont.BOLD);
            int textWidth = client.textRenderer.getWidth(styledText);
            int boxWidth = innerPad + markerSize + 6 + textWidth + innerPad;
            int boxHeight = 18;
            int x = layout.toastX;

            RoundedPanel.draw(matrices, x, y, boxWidth, boxHeight, bg);
            int markerY = y + (boxHeight - markerSize) / 2;
            DrawableHelper.fill(matrices, x + innerPad, markerY, x + innerPad + markerSize, markerY + markerSize, marker);
            int textY = y + (boxHeight - client.textRenderer.fontHeight) / 2;
            client.textRenderer.drawWithShadow(matrices, styledText, x + innerPad + markerSize + 6, textY, text);

            y += boxHeight + 4;
        }
    }

    /** Crisp flat panel with a thin light outline. Opacity 0 = no panel at all (clean text only). */
    private static void drawOutlinedPanel(MatrixStack matrices, int width, int height, HudLayoutConfig.Layout layout) {
        if (layout.backgroundOpacityPercent <= 0) return;
        int bg = RoundedPanel.toArgb(layout.backgroundColorHex, layout.backgroundOpacityPercent);
        DrawableHelper.fill(matrices, 0, 0, width, height, bg);
        DrawableHelper.fill(matrices, 0, 0, width, 1, BORDER_COLOR);
        DrawableHelper.fill(matrices, 0, height - 1, width, height, BORDER_COLOR);
        DrawableHelper.fill(matrices, 0, 0, 1, height, BORDER_COLOR);
        DrawableHelper.fill(matrices, width - 1, 0, width, height, BORDER_COLOR);
    }

    /**
     * Size of the plain-text Ready list. Empty = nothing drawn in-game; in the
     * HUD editor a placeholder is shown so the list can still be found and moved.
     */
    static int[] measureReadyList(MinecraftClient client, List<CooldownBoxes.Line> lines, boolean editor) {
        if (lines.isEmpty()) {
            if (!editor) return new int[]{0, 0};
            return new int[]{client.textRenderer.getWidth(CooldownFont.styled("Ready \u2014", CooldownFont.REGULAR)), CooldownBoxes.LINE_HEIGHT};
        }
        int per = CooldownBoxes.MAX_ROWS_PER_COLUMN;
        int columns = (int) Math.ceil(lines.size() / (double) per);
        int width = 0;
        int maxRows = 0;
        for (int c = 0; c < columns; c++) {
            int start = c * per;
            int end = Math.min(start + per, lines.size());
            int colW = 0;
            for (int i = start; i < end; i++) {
                colW = Math.max(colW, client.textRenderer.getWidth(CooldownFont.styled(lines.get(i).text + " Ready", CooldownFont.REGULAR)));
            }
            width += colW + (c > 0 ? CooldownBoxes.COLUMN_GAP : 0);
            maxRows = Math.max(maxRows, end - start);
        }
        return new int[]{width, maxRows * CooldownBoxes.LINE_HEIGHT};
    }

    /** Ready list as small plain coloured text ("Green Shell Ready") - no panel, no title, no markers. */
    static void drawReadyList(MatrixStack matrices, MinecraftClient client, List<CooldownBoxes.Line> lines,
                              int x, int y, float scale, boolean editor) {
        if (lines.isEmpty() && !editor) return;

        matrices.push();
        matrices.translate(x, y, 0);
        matrices.scale(scale, scale, 1f);

        if (lines.isEmpty()) {
            client.textRenderer.drawWithShadow(matrices, CooldownFont.styled("Ready \u2014", CooldownFont.REGULAR), 0, 0, EMPTY_COLOR);
            matrices.pop();
            return;
        }

        int per = CooldownBoxes.MAX_ROWS_PER_COLUMN;
        int columns = (int) Math.ceil(lines.size() / (double) per);
        int colX = 0;
        for (int c = 0; c < columns; c++) {
            int start = c * per;
            int end = Math.min(start + per, lines.size());
            int colW = 0;
            int rowY = 0;
            for (int i = start; i < end; i++) {
                net.minecraft.text.Text t = CooldownFont.styled(lines.get(i).text + " Ready", CooldownFont.REGULAR);
                client.textRenderer.drawWithShadow(matrices, t, colX, rowY, lines.get(i).color);
                colW = Math.max(colW, client.textRenderer.getWidth(t));
                rowY += CooldownBoxes.LINE_HEIGHT;
            }
            colX += colW + CooldownBoxes.COLUMN_GAP;
        }
        matrices.pop();
    }

    static void drawBox(MatrixStack matrices, MinecraftClient client, String title, List<CooldownBoxes.Line> lines,
                         int x, int y, float scale, int titleColor, HudLayoutConfig.Layout layout) {
        int[] size = CooldownBoxes.measure(client.textRenderer, title, lines);
        int width = size[0];
        int height = size[1];
        int columns = size[2];

        matrices.push();
        matrices.translate(x, y, 0);
        matrices.scale(scale, scale, 1f);

        // Opacity 0 means "clean text only, no panel at all" - the drop
        // shadow rect needs to be skipped too, or you'd still see a faint
        // dark box even with the background itself fully transparent.
        drawOutlinedPanel(matrices, width, height, layout);

        if (lines.isEmpty()) {
            client.textRenderer.drawWithShadow(matrices, CooldownFont.styled(title + " \u2014", CooldownFont.BOLD),
                    CooldownBoxes.PADDING, CooldownBoxes.PADDING, EMPTY_COLOR);
            matrices.pop();
            return;
        }

        client.textRenderer.drawWithShadow(matrices, CooldownFont.styled(title, CooldownFont.BOLD),
                CooldownBoxes.PADDING, CooldownBoxes.PADDING, titleColor);

        int headerHeight = CooldownBoxes.PADDING + CooldownBoxes.LINE_HEIGHT;
        int[] colWidths = CooldownBoxes.columnWidths(client.textRenderer, lines, columns);
        int colX = CooldownBoxes.PADDING;
        for (int col = 0; col < columns; col++) {
            int startIdx = col * CooldownBoxes.MAX_ROWS_PER_COLUMN;
            int endIdx = Math.min(startIdx + CooldownBoxes.MAX_ROWS_PER_COLUMN, lines.size());
            int rowY = headerHeight;
            for (int i = startIdx; i < endIdx; i++) {
                CooldownBoxes.Line line = lines.get(i);
                int slot = CooldownBoxes.slotWidth(lines);
                if (line.icon != null && line.icon.glyph != null && !line.icon.glyph.isEmpty()) {
                    net.minecraft.text.Text glyphText = CooldownFont.styled(line.icon.glyph, line.icon.fontId());
                    int glyphWidth = client.textRenderer.getWidth(glyphText);
                    client.textRenderer.drawWithShadow(matrices, glyphText, colX + Math.max(0f, (slot - glyphWidth) / 2f), rowY, line.icon.glyphColor);
                } else {
                    int markerX = colX + (slot - CooldownBoxes.MARKER_SIZE) / 2;
                    int markerY = rowY + (client.textRenderer.fontHeight - CooldownBoxes.MARKER_SIZE) / 2;
                    DrawableHelper.fill(matrices, markerX, markerY, markerX + CooldownBoxes.MARKER_SIZE, markerY + CooldownBoxes.MARKER_SIZE, line.color);
                }
                int textX = colX + slot + CooldownBoxes.MARKER_GAP;

                String name = line.text.substring(0, line.nameLength);
                client.textRenderer.drawWithShadow(matrices, CooldownFont.styled(name, CooldownFont.REGULAR), textX, rowY, line.color);

                if (line.nameLength < line.text.length()) {
                    // Countdown suffix always follows the automatic urgency
                    // coloring, even when the name itself has a custom color.
                    String suffix = line.text.substring(line.nameLength);
                    int suffixX = textX + client.textRenderer.getWidth(CooldownFont.styled(name, CooldownFont.REGULAR));
                    client.textRenderer.drawWithShadow(matrices, CooldownFont.styled(suffix, CooldownFont.REGULAR), suffixX, rowY, line.suffixColor);
                }

                rowY += CooldownBoxes.LINE_HEIGHT;
            }
            colX += colWidths[col] + CooldownBoxes.COLUMN_GAP;
        }

        matrices.pop();
    }

    static int[] measureSingleValueBox(MinecraftClient client, String title, String value) {
        int titleWidth = client.textRenderer.getWidth(CooldownFont.styled(title, CooldownFont.BOLD));
        int valueWidth = client.textRenderer.getWidth(CooldownFont.styled(value, CooldownFont.REGULAR));
        int width = CooldownBoxes.PADDING * 2 + Math.max(titleWidth, valueWidth);
        int height = CooldownBoxes.PADDING * 2 + CooldownBoxes.LINE_HEIGHT * 2;
        return new int[]{width, height};
    }

    static void drawSingleValueBox(MatrixStack matrices, MinecraftClient client, String title, String value,
                                    int titleColor, int valueColor, int x, int y, float scale, HudLayoutConfig.Layout layout) {
        int[] size = measureSingleValueBox(client, title, value);
        int width = size[0];
        int height = size[1];

        matrices.push();
        matrices.translate(x, y, 0);
        matrices.scale(scale, scale, 1f);

        drawOutlinedPanel(matrices, width, height, layout);

        client.textRenderer.drawWithShadow(matrices, CooldownFont.styled(title, CooldownFont.BOLD),
                CooldownBoxes.PADDING, CooldownBoxes.PADDING, titleColor);
        client.textRenderer.drawWithShadow(matrices, CooldownFont.styled(value, CooldownFont.REGULAR),
                CooldownBoxes.PADDING, CooldownBoxes.PADDING + CooldownBoxes.LINE_HEIGHT, valueColor);

        matrices.pop();
    }

    static int[] measureBackpackBox(MinecraftClient client) {
        String value = BackpackTracker.hasBackpack()
                ? BackpackTracker.getCurrent() + " / " + BackpackTracker.getMax()
                : "\u2014";
        return measureSingleValueBox(client, "Backpack", value);
    }

    static void drawBackpackBox(MatrixStack matrices, MinecraftClient client, int x, int y, float scale, HudLayoutConfig.Layout layout) {
        String value;
        int valueColor;
        if (BackpackTracker.hasBackpack()) {
            value = BackpackTracker.getCurrent() + " / " + BackpackTracker.getMax();
            float fullness = BackpackTracker.getFullness();
            if (fullness >= 0.9f) {
                valueColor = 0xFFFF5C5C;
            } else if (fullness >= 0.7f) {
                valueColor = 0xFFFFC94D;
            } else {
                valueColor = 0xFFFFFFFF;
            }
        } else {
            value = "\u2014";
            valueColor = EMPTY_COLOR;
        }
        drawSingleValueBox(matrices, client, "Backpack", value, BACKPACK_TITLE_COLOR, valueColor, x, y, scale, layout);
    }

    static int[] measureSnakeEyesBox(MinecraftClient client) {
        String value = SnakeEyesTracker.hasActiveBuff() ? SnakeEyesTracker.getCurrentBuff() : "Rolling\u2026";
        return measureSingleValueBox(client, "Snake Eyes", value);
    }

    static void drawSnakeEyesBox(MatrixStack matrices, MinecraftClient client, int x, int y, float scale, HudLayoutConfig.Layout layout) {
        String value;
        int valueColor;
        if (SnakeEyesTracker.hasActiveBuff()) {
            long remaining = SnakeEyesTracker.getRemainingMillis();
            value = SnakeEyesTracker.getCurrentBuff() + " (" + ((remaining + 999) / 1000) + "s)";
            valueColor = remaining < 3000 ? 0xFFFF5C5C : 0xFFFFFFFF;
        } else {
            value = "Rolling\u2026";
            valueColor = EMPTY_COLOR;
        }
        drawSingleValueBox(matrices, client, "Snake Eyes", value, SNAKE_EYES_TITLE_COLOR, valueColor, x, y, scale, layout);
    }

    static int[] measureMoodSwingsBox(MinecraftClient client) {
        String value = MoodSwingsTracker.getCurrentMood() != null ? MoodSwingsTracker.getCurrentMood() : "\u2014";
        return measureSingleValueBox(client, "Mood", value);
    }

    static void drawMoodSwingsBox(MatrixStack matrices, MinecraftClient client, int x, int y, float scale, HudLayoutConfig.Layout layout) {
        String mood = MoodSwingsTracker.getCurrentMood();
        String value = mood != null ? mood : "\u2014";
        int valueColor;
        if ("Aggressive".equalsIgnoreCase(mood)) {
            valueColor = 0xFFFF6B6B;
        } else if ("Playful".equalsIgnoreCase(mood)) {
            valueColor = 0xFF7CFC98;
        } else if ("Lazy".equalsIgnoreCase(mood)) {
            valueColor = 0xFFFFC94D;
        } else {
            valueColor = EMPTY_COLOR;
        }
        drawSingleValueBox(matrices, client, "Mood", value, MOOD_SWINGS_TITLE_COLOR, valueColor, x, y, scale, layout);
    }
}
