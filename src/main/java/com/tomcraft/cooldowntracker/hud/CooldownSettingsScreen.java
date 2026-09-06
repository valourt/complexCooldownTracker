package com.tomcraft.cooldowntracker.hud;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

/**
 * GUI front-end for the global HUD settings (equivalent to /cooldowns
 * togglecooldown, toggleready, setbgcolor, setopacity). Per-item settings
 * (setcolor, additem, importcsv etc.) aren't included here - with 500+
 * possible items, a proper picker for those is a bigger feature on its
 * own, so those stay as chat commands for now.
 */
public class CooldownSettingsScreen extends Screen {

    private final Screen parent;
    private ButtonWidget readyToggle;
    private ButtonWidget cooldownToggle;
    private ButtonWidget toastToggle;
    private ButtonWidget backpackToggle;
    private ButtonWidget totemWatchToggle;
    private ButtonWidget snakeEyesToggle;
    private ButtonWidget moodSwingsToggle;
    private TextFieldWidget colorField;
    private TextFieldWidget opacityField;

    public CooldownSettingsScreen(Screen parent) {
        super(new LiteralText("Cooldown Tracker - Settings"));
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        HudLayoutConfig.Layout layout = HudLayoutConfig.get();
        int centerX = width / 2;
        int y = height / 2 - 90;
        int rowHeight = 24;

        readyToggle = addDrawableChild(new ButtonWidget(centerX - 100, y, 200, 20,
                readyLabel(layout), b -> {
                    layout.readyBoxVisible = !layout.readyBoxVisible;
                    HudLayoutConfig.save();
                    readyToggle.setMessage(readyLabel(layout));
                }));
        y += rowHeight;

        cooldownToggle = addDrawableChild(new ButtonWidget(centerX - 100, y, 200, 20,
                cooldownLabel(layout), b -> {
                    layout.cooldownBoxVisible = !layout.cooldownBoxVisible;
                    HudLayoutConfig.save();
                    cooldownToggle.setMessage(cooldownLabel(layout));
                }));
        y += rowHeight;

        toastToggle = addDrawableChild(new ButtonWidget(centerX - 100, y, 200, 20,
                toastLabel(layout), b -> {
                    layout.toastVisible = !layout.toastVisible;
                    HudLayoutConfig.save();
                    toastToggle.setMessage(toastLabel(layout));
                }));
        y += rowHeight;

        backpackToggle = addDrawableChild(new ButtonWidget(centerX - 100, y, 200, 20,
                backpackLabel(layout), b -> {
                    layout.backpackBoxVisible = !layout.backpackBoxVisible;
                    HudLayoutConfig.save();
                    backpackToggle.setMessage(backpackLabel(layout));
                }));
        y += rowHeight;

        totemWatchToggle = addDrawableChild(new ButtonWidget(centerX - 100, y, 200, 20,
                totemWatchLabel(layout), b -> {
                    layout.totemWatchBoxVisible = !layout.totemWatchBoxVisible;
                    HudLayoutConfig.save();
                    totemWatchToggle.setMessage(totemWatchLabel(layout));
                }));
        y += rowHeight;

        snakeEyesToggle = addDrawableChild(new ButtonWidget(centerX - 100, y, 200, 20,
                snakeEyesLabel(layout), b -> {
                    layout.snakeEyesBoxVisible = !layout.snakeEyesBoxVisible;
                    HudLayoutConfig.save();
                    snakeEyesToggle.setMessage(snakeEyesLabel(layout));
                }));
        y += rowHeight;

        moodSwingsToggle = addDrawableChild(new ButtonWidget(centerX - 100, y, 200, 20,
                moodSwingsLabel(layout), b -> {
                    layout.moodSwingsBoxVisible = !layout.moodSwingsBoxVisible;
                    HudLayoutConfig.save();
                    moodSwingsToggle.setMessage(moodSwingsLabel(layout));
                }));
        y += rowHeight + 10;

        colorField = new TextFieldWidget(client.textRenderer, centerX - 100, y + 12, 140, 18, new LiteralText("Background color"));
        colorField.setMaxLength(7);
        colorField.setText(layout.backgroundColorHex);
        colorField.setChangedListener(text -> {
            String cleaned = text.startsWith("#") ? text.substring(1) : text;
            if (cleaned.length() == 6 && cleaned.matches("[0-9a-fA-F]{6}")) {
                layout.backgroundColorHex = cleaned;
                HudLayoutConfig.save();
                colorField.setEditableColor(0xFFFFFF);
            } else {
                colorField.setEditableColor(0xFF5555);
            }
        });
        addDrawableChild(colorField);
        addDrawableChild(new ButtonWidget(centerX + 45, y + 12, 55, 18, new LiteralText("Default"), b -> {
            layout.backgroundColorHex = "1E1E1E";
            HudLayoutConfig.save();
            colorField.setText("1E1E1E");
        }));
        y += rowHeight + 12;

        opacityField = new TextFieldWidget(client.textRenderer, centerX - 100, y + 12, 60, 18, new LiteralText("Opacity"));
        opacityField.setMaxLength(3);
        opacityField.setText(String.valueOf(layout.backgroundOpacityPercent));
        opacityField.setChangedListener(text -> {
            try {
                int percent = Integer.parseInt(text.trim());
                if (percent >= 0 && percent <= 100) {
                    layout.backgroundOpacityPercent = percent;
                    HudLayoutConfig.save();
                    opacityField.setEditableColor(0xFFFFFF);
                } else {
                    opacityField.setEditableColor(0xFF5555);
                }
            } catch (NumberFormatException e) {
                opacityField.setEditableColor(0xFF5555);
            }
        });
        addDrawableChild(opacityField);
        addDrawableChild(new ButtonWidget(centerX - 30, y + 12, 20, 18, new LiteralText("-"), b -> adjustOpacity(layout, -10)));
        addDrawableChild(new ButtonWidget(centerX - 5, y + 12, 20, 18, new LiteralText("+"), b -> adjustOpacity(layout, 10)));
        addDrawableChild(new ButtonWidget(centerX + 20, y + 12, 80, 18, new LiteralText("Text Only"), b -> {
            layout.backgroundOpacityPercent = 0;
            HudLayoutConfig.save();
            opacityField.setText("0");
        }));
        y += rowHeight + 30;

        addDrawableChild(new ButtonWidget(centerX - 100, y, 95, 20, new LiteralText("Reposition"),
                b -> client.setScreen(new HudEditScreen())));
        addDrawableChild(new ButtonWidget(centerX + 5, y, 95, 20, new LiteralText("Back"),
                b -> client.setScreen(parent)));
    }

    private void adjustOpacity(HudLayoutConfig.Layout layout, int delta) {
        int newValue = Math.max(0, Math.min(100, layout.backgroundOpacityPercent + delta));
        layout.backgroundOpacityPercent = newValue;
        HudLayoutConfig.save();
        opacityField.setText(String.valueOf(newValue));
    }

    private static net.minecraft.text.Text readyLabel(HudLayoutConfig.Layout layout) {
        return new LiteralText("Ready Box: " + (layout.readyBoxVisible ? "Shown" : "Hidden"));
    }

    private static net.minecraft.text.Text cooldownLabel(HudLayoutConfig.Layout layout) {
        return new LiteralText("On Cooldown Box: " + (layout.cooldownBoxVisible ? "Shown" : "Hidden"));
    }

    private static net.minecraft.text.Text toastLabel(HudLayoutConfig.Layout layout) {
        return new LiteralText("Ready Pop-up: " + (layout.toastVisible ? "Shown" : "Hidden"));
    }

    private static net.minecraft.text.Text backpackLabel(HudLayoutConfig.Layout layout) {
        return new LiteralText("Backpack Box: " + (layout.backpackBoxVisible ? "Shown" : "Hidden"));
    }

    private static net.minecraft.text.Text totemWatchLabel(HudLayoutConfig.Layout layout) {
        return new LiteralText("Totem Watch Box: " + (layout.totemWatchBoxVisible ? "Shown" : "Hidden"));
    }

    private static net.minecraft.text.Text snakeEyesLabel(HudLayoutConfig.Layout layout) {
        return new LiteralText("Snake Eyes Box: " + (layout.snakeEyesBoxVisible ? "Shown" : "Hidden"));
    }

    private static net.minecraft.text.Text moodSwingsLabel(HudLayoutConfig.Layout layout) {
        return new LiteralText("Mood Swings Box: " + (layout.moodSwingsBoxVisible ? "Shown" : "Hidden"));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        net.minecraft.client.gui.DrawableHelper.drawCenteredText(matrices, client.textRenderer, title, width / 2, height / 2 - 110, 0xFFFFFF);

        int centerX = width / 2;
        int labelY = height / 2 - 90 + 24 * 7 + 10;
        client.textRenderer.drawWithShadow(matrices, "Background color", centerX - 100, labelY, 0xFFAAAAAA);
        client.textRenderer.drawWithShadow(matrices, "Opacity %", centerX - 100, labelY + 34, 0xFFAAAAAA);

        String hint = "Per-item colors and bulk import still use chat commands (see README)";
        client.textRenderer.drawWithShadow(matrices, hint, (width - client.textRenderer.getWidth(hint)) / 2f, height - 20, 0xFF888888);

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void removed() {
        HudLayoutConfig.save();
        super.removed();
    }
}
