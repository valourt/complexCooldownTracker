package com.tomcraft.cooldowntracker.hud;

import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.util.List;

/**
 * Opened via the keybind or "/cooldowns hud". Click and drag either box (or
 * the sample "ready" pop-up) to move it, or scroll while hovering over one
 * to resize it - the pop-up doesn't resize, only moves. Position/scale save
 * the moment you let go or scroll, and again when the screen closes. Esc
 * (or the normal "close screen" key) exits.
 */
public class HudEditScreen extends Screen {

    private static final int BOX_OUTLINE = 0xFF66E0FF;
    private static final int TOAST_OUTLINE = 0xFF57E389;
    private static final float SCROLL_STEP = 0.1f;
    private static final String SAMPLE_TOAST_TEXT = "Example is ready!";

    private enum Dragging {NONE, READY, COOLDOWN, TOAST, BACKPACK, TOTEM_WATCH, SNAKE_EYES, MOOD_SWINGS}

    private Dragging dragging = Dragging.NONE;
    private int dragOffsetX;
    private int dragOffsetY;

    public HudEditScreen() {
        super(new LiteralText("Cooldown Tracker - HUD Editor"));
    }

    @Override
    protected void init() {
        addDrawableChild(new ButtonWidget(width / 2 - 60, height - 40, 120, 20,
                new LiteralText("Settings"), b -> client.setScreen(new CooldownSettingsScreen(this))));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);

        HudLayoutConfig.Layout layout = HudLayoutConfig.get();
        List<CooldownBoxes.Line> readyLines = CooldownBoxes.getReadyLines();
        List<CooldownBoxes.Line> cooldownLines = CooldownBoxes.getCooldownLines();

        CooldownHud.drawBox(matrices, client, "Ready", readyLines, layout.readyBoxX, layout.readyBoxY, layout.readyBoxScale, 0xFFA9F5C4, layout);
        drawOutlineAround(matrices, BOX_OUTLINE, "Ready", readyLines, layout.readyBoxX, layout.readyBoxY, layout.readyBoxScale);

        CooldownHud.drawBox(matrices, client, "On Cooldown", cooldownLines, layout.cooldownBoxX, layout.cooldownBoxY, layout.cooldownBoxScale, 0xFFFFC49A, layout);
        drawOutlineAround(matrices, BOX_OUTLINE, "On Cooldown", cooldownLines, layout.cooldownBoxX, layout.cooldownBoxY, layout.cooldownBoxScale);

        CooldownHud.drawBackpackBox(matrices, client, layout.backpackBoxX, layout.backpackBoxY, layout.backpackBoxScale, layout);
        drawSingleValueOutline(matrices, CooldownHud.measureBackpackBox(client), layout.backpackBoxX, layout.backpackBoxY, layout.backpackBoxScale);

        List<CooldownBoxes.Line> totemWatchLines = CooldownBoxes.getOtherTotemLines();
        CooldownHud.drawBox(matrices, client, "Totem Watch", totemWatchLines, layout.totemWatchBoxX, layout.totemWatchBoxY, layout.totemWatchBoxScale, 0xFF8FD6FF, layout);
        drawOutlineAround(matrices, BOX_OUTLINE, "Totem Watch", totemWatchLines, layout.totemWatchBoxX, layout.totemWatchBoxY, layout.totemWatchBoxScale);

        CooldownHud.drawSnakeEyesBox(matrices, client, layout.snakeEyesBoxX, layout.snakeEyesBoxY, layout.snakeEyesBoxScale, layout);
        drawSingleValueOutline(matrices, CooldownHud.measureSnakeEyesBox(client), layout.snakeEyesBoxX, layout.snakeEyesBoxY, layout.snakeEyesBoxScale);

        CooldownHud.drawMoodSwingsBox(matrices, client, layout.moodSwingsBoxX, layout.moodSwingsBoxY, layout.moodSwingsBoxScale, layout);
        drawSingleValueOutline(matrices, CooldownHud.measureMoodSwingsBox(client), layout.moodSwingsBoxX, layout.moodSwingsBoxY, layout.moodSwingsBoxScale);

        drawSampleToast(matrices, layout);

        String help = "Drag to move, scroll to resize - Esc to save & close";
        client.textRenderer.drawWithShadow(matrices, help,
                (width - client.textRenderer.getWidth(help)) / 2f, height - 62, 0xFFFFFFFF);
        String help2 = "The green box is where the ready pop-up appears";
        client.textRenderer.drawWithShadow(matrices, help2,
                (width - client.textRenderer.getWidth(help2)) / 2f, height - 52, 0xFFAAAAAA);

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void drawSampleToast(MatrixStack matrices, HudLayoutConfig.Layout layout) {
        int textWidth = client.textRenderer.getWidth(CooldownFont.styled(SAMPLE_TOAST_TEXT, CooldownFont.BOLD));
        int boxWidth = 9 + 5 + 6 + textWidth + 9;
        int boxHeight = 18;
        int x = layout.toastX;
        int y = layout.toastY;

        RoundedPanel.draw(matrices, x, y, boxWidth, boxHeight, 0xE016211B);
        int markerY = y + (boxHeight - 5) / 2;
        DrawableHelper.fill(matrices, x + 9, markerY, x + 14, markerY + 5, 0xFF57E389);
        int textY = y + (boxHeight - client.textRenderer.fontHeight) / 2;
        client.textRenderer.drawWithShadow(matrices, CooldownFont.styled(SAMPLE_TOAST_TEXT, CooldownFont.BOLD), x + 20, textY, 0xFFFFFFFF);

        drawOutline(matrices, TOAST_OUTLINE, x, y, boxWidth, boxHeight);
    }

    private void drawOutlineAround(MatrixStack matrices, int color, String title, List<CooldownBoxes.Line> lines, int x, int y, float scale) {
        int[] size = CooldownBoxes.measure(client.textRenderer, title, lines);
        int w = (int) (size[0] * scale);
        int h = (int) (size[1] * scale);
        drawOutline(matrices, color, x, y, w, h);
    }

    private void drawSingleValueOutline(MatrixStack matrices, int[] size, int x, int y, float scale) {
        int w = (int) (size[0] * scale);
        int h = (int) (size[1] * scale);
        drawOutline(matrices, BOX_OUTLINE, x, y, w, h);
    }

    private void drawOutline(MatrixStack matrices, int color, int x, int y, int w, int h) {
        DrawableHelper.fill(matrices, x, y, x + w, y + 1, color);
        DrawableHelper.fill(matrices, x, y + h - 1, x + w, y + h, color);
        DrawableHelper.fill(matrices, x, y, x + 1, y + h, color);
        DrawableHelper.fill(matrices, x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        HudLayoutConfig.Layout layout = HudLayoutConfig.get();

        if (insideBox(mouseX, mouseY, "Ready", CooldownBoxes.getReadyLines(), layout.readyBoxX, layout.readyBoxY, layout.readyBoxScale)) {
            dragging = Dragging.READY;
            dragOffsetX = (int) mouseX - layout.readyBoxX;
            dragOffsetY = (int) mouseY - layout.readyBoxY;
            return true;
        }
        if (insideBox(mouseX, mouseY, "On Cooldown", CooldownBoxes.getCooldownLines(), layout.cooldownBoxX, layout.cooldownBoxY, layout.cooldownBoxScale)) {
            dragging = Dragging.COOLDOWN;
            dragOffsetX = (int) mouseX - layout.cooldownBoxX;
            dragOffsetY = (int) mouseY - layout.cooldownBoxY;
            return true;
        }
        if (insideToast(mouseX, mouseY, layout)) {
            dragging = Dragging.TOAST;
            dragOffsetX = (int) mouseX - layout.toastX;
            dragOffsetY = (int) mouseY - layout.toastY;
            return true;
        }
        if (insideBackpackBox(mouseX, mouseY, layout)) {
            dragging = Dragging.BACKPACK;
            dragOffsetX = (int) mouseX - layout.backpackBoxX;
            dragOffsetY = (int) mouseY - layout.backpackBoxY;
            return true;
        }
        if (insideBox(mouseX, mouseY, "Totem Watch", CooldownBoxes.getOtherTotemLines(), layout.totemWatchBoxX, layout.totemWatchBoxY, layout.totemWatchBoxScale)) {
            dragging = Dragging.TOTEM_WATCH;
            dragOffsetX = (int) mouseX - layout.totemWatchBoxX;
            dragOffsetY = (int) mouseY - layout.totemWatchBoxY;
            return true;
        }
        if (insideSnakeEyesBox(mouseX, mouseY, layout)) {
            dragging = Dragging.SNAKE_EYES;
            dragOffsetX = (int) mouseX - layout.snakeEyesBoxX;
            dragOffsetY = (int) mouseY - layout.snakeEyesBoxY;
            return true;
        }
        if (insideMoodSwingsBox(mouseX, mouseY, layout)) {
            dragging = Dragging.MOOD_SWINGS;
            dragOffsetX = (int) mouseX - layout.moodSwingsBoxX;
            dragOffsetY = (int) mouseY - layout.moodSwingsBoxY;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging == Dragging.NONE) return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);

        HudLayoutConfig.Layout layout = HudLayoutConfig.get();
        int newX = clamp((int) mouseX - dragOffsetX, 0, width - 10);
        int newY = clamp((int) mouseY - dragOffsetY, 0, height - 10);

        switch (dragging) {
            case READY:
                layout.readyBoxX = newX;
                layout.readyBoxY = newY;
                break;
            case COOLDOWN:
                layout.cooldownBoxX = newX;
                layout.cooldownBoxY = newY;
                break;
            case TOAST:
                layout.toastX = newX;
                layout.toastY = newY;
                break;
            case BACKPACK:
                layout.backpackBoxX = newX;
                layout.backpackBoxY = newY;
                break;
            case TOTEM_WATCH:
                layout.totemWatchBoxX = newX;
                layout.totemWatchBoxY = newY;
                break;
            case SNAKE_EYES:
                layout.snakeEyesBoxX = newX;
                layout.snakeEyesBoxY = newY;
                break;
            case MOOD_SWINGS:
                layout.moodSwingsBoxX = newX;
                layout.moodSwingsBoxY = newY;
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != Dragging.NONE) {
            dragging = Dragging.NONE;
            HudLayoutConfig.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        HudLayoutConfig.Layout layout = HudLayoutConfig.get();

        if (insideBox(mouseX, mouseY, "Ready", CooldownBoxes.getReadyLines(), layout.readyBoxX, layout.readyBoxY, layout.readyBoxScale)) {
            layout.readyBoxScale = CooldownBoxes.clampScale(layout.readyBoxScale + (float) amount * SCROLL_STEP);
            HudLayoutConfig.save();
            return true;
        }
        if (insideBox(mouseX, mouseY, "On Cooldown", CooldownBoxes.getCooldownLines(), layout.cooldownBoxX, layout.cooldownBoxY, layout.cooldownBoxScale)) {
            layout.cooldownBoxScale = CooldownBoxes.clampScale(layout.cooldownBoxScale + (float) amount * SCROLL_STEP);
            HudLayoutConfig.save();
            return true;
        }
        if (insideBackpackBox(mouseX, mouseY, layout)) {
            layout.backpackBoxScale = CooldownBoxes.clampScale(layout.backpackBoxScale + (float) amount * SCROLL_STEP);
            HudLayoutConfig.save();
            return true;
        }
        if (insideBox(mouseX, mouseY, "Totem Watch", CooldownBoxes.getOtherTotemLines(), layout.totemWatchBoxX, layout.totemWatchBoxY, layout.totemWatchBoxScale)) {
            layout.totemWatchBoxScale = CooldownBoxes.clampScale(layout.totemWatchBoxScale + (float) amount * SCROLL_STEP);
            HudLayoutConfig.save();
            return true;
        }
        if (insideSnakeEyesBox(mouseX, mouseY, layout)) {
            layout.snakeEyesBoxScale = CooldownBoxes.clampScale(layout.snakeEyesBoxScale + (float) amount * SCROLL_STEP);
            HudLayoutConfig.save();
            return true;
        }
        if (insideMoodSwingsBox(mouseX, mouseY, layout)) {
            layout.moodSwingsBoxScale = CooldownBoxes.clampScale(layout.moodSwingsBoxScale + (float) amount * SCROLL_STEP);
            HudLayoutConfig.save();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void removed() {
        HudLayoutConfig.save();
        super.removed();
    }

    private boolean insideBox(double mx, double my, String title, List<CooldownBoxes.Line> lines, int x, int y, float scale) {
        int[] size = CooldownBoxes.measure(client.textRenderer, title, lines);
        int w = (int) (size[0] * scale);
        int h = (int) (size[1] * scale);
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private boolean insideToast(double mx, double my, HudLayoutConfig.Layout layout) {
        int textWidth = client.textRenderer.getWidth(CooldownFont.styled(SAMPLE_TOAST_TEXT, CooldownFont.BOLD));
        int boxWidth = 9 + 5 + 6 + textWidth + 9;
        int boxHeight = 18;
        return mx >= layout.toastX && mx <= layout.toastX + boxWidth && my >= layout.toastY && my <= layout.toastY + boxHeight;
    }

    private boolean insideBackpackBox(double mx, double my, HudLayoutConfig.Layout layout) {
        return insideSingleValueBox(mx, my, CooldownHud.measureBackpackBox(client), layout.backpackBoxX, layout.backpackBoxY, layout.backpackBoxScale);
    }

    private boolean insideSnakeEyesBox(double mx, double my, HudLayoutConfig.Layout layout) {
        return insideSingleValueBox(mx, my, CooldownHud.measureSnakeEyesBox(client), layout.snakeEyesBoxX, layout.snakeEyesBoxY, layout.snakeEyesBoxScale);
    }

    private boolean insideMoodSwingsBox(double mx, double my, HudLayoutConfig.Layout layout) {
        return insideSingleValueBox(mx, my, CooldownHud.measureMoodSwingsBox(client), layout.moodSwingsBoxX, layout.moodSwingsBoxY, layout.moodSwingsBoxScale);
    }

    private boolean insideSingleValueBox(double mx, double my, int[] size, int x, int y, float scale) {
        int w = (int) (size[0] * scale);
        int h = (int) (size[1] * scale);
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
