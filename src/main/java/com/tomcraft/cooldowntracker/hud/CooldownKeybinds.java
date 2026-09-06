package com.tomcraft.cooldowntracker.hud;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CooldownKeybinds {

    public static KeyBinding editHudKey;
    public static KeyBinding toggleReadyBoxKey;
    public static KeyBinding toggleCooldownBoxKey;
    public static KeyBinding toggleBackpackBoxKey;
    public static KeyBinding toggleTotemWatchBoxKey;
    public static KeyBinding toggleSnakeEyesBoxKey;
    public static KeyBinding toggleMoodSwingsBoxKey;

    public static void register() {
        // All unbound by default (GLFW_KEY_UNKNOWN) - bind them yourself
        // under Options > Controls > Key Binds > Cooldown Tracker, or just
        // use the matching "/cooldowns" commands / the in-game Settings
        // screen (opened from the HUD editor) instead.
        editHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cooldowntracker.edit_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.cooldowntracker"
        ));
        toggleReadyBoxKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cooldowntracker.toggle_ready_box",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.cooldowntracker"
        ));
        toggleCooldownBoxKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cooldowntracker.toggle_cooldown_box",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.cooldowntracker"
        ));
        toggleBackpackBoxKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cooldowntracker.toggle_backpack_box",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.cooldowntracker"
        ));
        toggleTotemWatchBoxKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cooldowntracker.toggle_totem_watch_box",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.cooldowntracker"
        ));
        toggleSnakeEyesBoxKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cooldowntracker.toggle_snake_eyes_box",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.cooldowntracker"
        ));
        toggleMoodSwingsBoxKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cooldowntracker.toggle_mood_swings_box",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.cooldowntracker"
        ));
    }
}
