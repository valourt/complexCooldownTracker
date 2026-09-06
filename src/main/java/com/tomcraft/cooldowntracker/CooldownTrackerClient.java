package com.tomcraft.cooldowntracker;

import com.tomcraft.cooldowntracker.command.CooldownCommands;
import com.tomcraft.cooldowntracker.config.CooldownConfig;
import com.tomcraft.cooldowntracker.hud.CooldownHud;
import com.tomcraft.cooldowntracker.hud.CooldownKeybinds;
import com.tomcraft.cooldowntracker.hud.ArmorEffectTracker;
import com.tomcraft.cooldowntracker.hud.BackpackTracker;
import com.tomcraft.cooldowntracker.hud.HudEditScreen;
import com.tomcraft.cooldowntracker.hud.HudLayoutConfig;
import com.tomcraft.cooldowntracker.hud.InventoryOwnershipScanner;
import com.tomcraft.cooldowntracker.hud.ReadyToastManager;
import com.tomcraft.cooldowntracker.hud.TotemWatchWorldRenderer;
import com.tomcraft.cooldowntracker.listener.GameStatePoller;
import com.tomcraft.cooldowntracker.listener.OnlinePlayerCache;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class CooldownTrackerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CooldownConfig.init();
        HudLayoutConfig.init();
        CooldownHud.register();
        CooldownCommands.register();
        CooldownKeybinds.register();
        InventoryOwnershipScanner.register();
        GameStatePoller.register();
        ReadyToastManager.register();
        OnlinePlayerCache.register();
        BackpackTracker.register();
        TotemWatchWorldRenderer.register();
        ArmorEffectTracker.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (CooldownKeybinds.editHudKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new HudEditScreen());
                }
            }
            while (CooldownKeybinds.toggleReadyBoxKey.wasPressed()) {
                HudLayoutConfig.Layout layout = HudLayoutConfig.get();
                layout.readyBoxVisible = !layout.readyBoxVisible;
                HudLayoutConfig.save();
            }
            while (CooldownKeybinds.toggleCooldownBoxKey.wasPressed()) {
                HudLayoutConfig.Layout layout = HudLayoutConfig.get();
                layout.cooldownBoxVisible = !layout.cooldownBoxVisible;
                HudLayoutConfig.save();
            }
            while (CooldownKeybinds.toggleBackpackBoxKey.wasPressed()) {
                HudLayoutConfig.Layout layout = HudLayoutConfig.get();
                layout.backpackBoxVisible = !layout.backpackBoxVisible;
                HudLayoutConfig.save();
            }
            while (CooldownKeybinds.toggleTotemWatchBoxKey.wasPressed()) {
                HudLayoutConfig.Layout layout = HudLayoutConfig.get();
                layout.totemWatchBoxVisible = !layout.totemWatchBoxVisible;
                HudLayoutConfig.save();
            }
            while (CooldownKeybinds.toggleSnakeEyesBoxKey.wasPressed()) {
                HudLayoutConfig.Layout layout = HudLayoutConfig.get();
                layout.snakeEyesBoxVisible = !layout.snakeEyesBoxVisible;
                HudLayoutConfig.save();
            }
            while (CooldownKeybinds.toggleMoodSwingsBoxKey.wasPressed()) {
                HudLayoutConfig.Layout layout = HudLayoutConfig.get();
                layout.moodSwingsBoxVisible = !layout.moodSwingsBoxVisible;
                HudLayoutConfig.save();
            }
        });
    }
}
