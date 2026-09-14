package com.tomcraft.cooldowntracker.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * Lets a mod author host a master cooldowns.csv somewhere (e.g. a GitHub
 * raw file link) and have every player's client automatically pull the
 * latest version, instead of everyone needing to manually re-import a new
 * file whenever the item roster changes. The URL is stored locally per
 * player (set once via /cooldowns setupdateurl) - this is entirely
 * client-side, there's no central server component.
 */
public class RemoteUpdateManager {

    private static Path configDir;
    private static Path settingsFile;
    private static final String CACHE_FILENAME = "remote-cooldowns.csv";

    private static String updateUrl = "";

    public static void init() {
        configDir = FabricLoader.getInstance().getConfigDir().resolve("cooldowntracker");
        settingsFile = configDir.resolve("remote_update_url.txt");
        load();
    }

    private static void load() {
        try {
            if (Files.exists(settingsFile)) {
                updateUrl = Files.readString(settingsFile, StandardCharsets.UTF_8).trim();
            }
        } catch (IOException e) {
            updateUrl = "";
        }
    }

    private static void save() {
        try {
            if (!Files.exists(configDir)) Files.createDirectories(configDir);
            Files.writeString(settingsFile, updateUrl == null ? "" : updateUrl, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getUpdateUrl() {
        return updateUrl;
    }

    public static void setUpdateUrl(String url) {
        updateUrl = url;
        save();
    }

    public static boolean hasUpdateUrl() {
        return updateUrl != null && !updateUrl.isEmpty();
    }

    /**
     * Fetches the configured URL on a background thread (never blocks the
     * game), saves it locally, then imports it via the same CsvImporter
     * used for local files. Both callbacks are dispatched through
     * mainThreadExecutor (pass MinecraftClient::execute) so it's always
     * safe to touch chat/UI state from them.
     */
    public static void update(Consumer<CsvImporter.Result> onComplete, Consumer<String> onError,
                               Consumer<Runnable> mainThreadExecutor) {
        if (!hasUpdateUrl()) {
            mainThreadExecutor.accept(() -> onError.accept(
                    "No update URL configured. Use /cooldowns setupdateurl <url> first."));
            return;
        }

        String url = updateUrl;
        Path cacheFile = configDir.resolve(CACHE_FILENAME);

        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().timeout(Duration.ofSeconds(15)).build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    mainThreadExecutor.accept(() -> onError.accept("Update server returned HTTP " + response.statusCode() + "."));
                    return;
                }

                if (!Files.exists(configDir)) Files.createDirectories(configDir);
                Files.writeString(cacheFile, response.body(), StandardCharsets.UTF_8);

                CsvImporter.Result result = CsvImporter.importFile(CACHE_FILENAME);
                mainThreadExecutor.accept(() -> onComplete.accept(result));
            } catch (Exception e) {
                mainThreadExecutor.accept(() -> onError.accept("Update failed: " + e.getMessage()));
            }
        }, "cooldowntracker-update").start();
    }
}
