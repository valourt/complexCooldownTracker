package com.tomcraft.cooldowntracker.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persists the on-screen position of the two draggable HUD boxes.
 */
public class HudLayoutConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path configDir;
    private static Path configFile;

    public static class Layout {
        public int readyBoxX = 6;
        public int readyBoxY = 6;
        public float readyBoxScale = 1.0f;
        public boolean readyBoxVisible = true;

        public int cooldownBoxX = 6;
        public int cooldownBoxY = 100;
        public float cooldownBoxScale = 1.0f;
        public boolean cooldownBoxVisible = true;

        public int toastX = 100;
        public int toastY = 40;
        public boolean toastVisible = true;

        public int backpackBoxX = 6;
        public int backpackBoxY = 200;
        public float backpackBoxScale = 1.0f;
        public boolean backpackBoxVisible = true;

        public int totemWatchBoxX = 6;
        public int totemWatchBoxY = 260;
        public float totemWatchBoxScale = 1.0f;
        public boolean totemWatchBoxVisible = true;

        public int snakeEyesBoxX = 6;
        public int snakeEyesBoxY = 320;
        public float snakeEyesBoxScale = 1.0f;
        public boolean snakeEyesBoxVisible = true;

        public int moodSwingsBoxX = 6;
        public int moodSwingsBoxY = 380;
        public float moodSwingsBoxScale = 1.0f;
        public boolean moodSwingsBoxVisible = true;

        public String backgroundColorHex = "1E1E1E";
        public int backgroundOpacityPercent = 85;
    }

    private static Layout layout = new Layout();

    public static void init() {
        configDir = FabricLoader.getInstance().getConfigDir().resolve("cooldowntracker");
        configFile = configDir.resolve("hud_layout.json");
        load();
    }

    public static Layout get() {
        return layout;
    }

    public static synchronized void load() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            if (!Files.exists(configFile)) {
                save();
                return;
            }
            try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                Layout loaded = GSON.fromJson(reader, Layout.class);
                layout = loaded != null ? loaded : new Layout();
            }
        } catch (IOException e) {
            e.printStackTrace();
            layout = new Layout();
        }
    }

    public static synchronized void save() {
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                GSON.toJson(layout, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
