package dev.zyron.client.config;

import com.google.gson.*;
import dev.zyron.client.ZyronClient;
import dev.zyron.client.feature.Feature;
import dev.zyron.client.feature.FeatureManager;
import dev.zyron.client.gui.Theme;
import dev.zyron.client.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Saves every feature's on/off state and settings to config/zyron-client.json. */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("zyron-client.json");

    /** ClickGUI panel positions, keyed by category name. */
    public static final Map<String, int[]> PANELS = new LinkedHashMap<>();

    private static FeatureManager manager;

    private ConfigManager() {
    }

    public static void load(FeatureManager features) {
        manager = features;
        if (!Files.exists(FILE)) return;
        try (Reader reader = Files.newBufferedReader(FILE)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            JsonObject feats = root.has("features") ? root.getAsJsonObject("features") : new JsonObject();
            for (Feature f : features.all()) {
                if (!feats.has(f.name)) continue;
                JsonObject o = feats.getAsJsonObject(f.name);
                if (f.persistsEnabled() && o.has("enabled")) f.setEnabled(o.get("enabled").getAsBoolean());
                if (o.has("settings")) loadSettings(f.settings(), o.getAsJsonObject("settings"));
            }

            if (root.has("gui")) {
                JsonObject gui = root.getAsJsonObject("gui");
                if (gui.has("settings")) loadSettings(Theme.SETTINGS, gui.getAsJsonObject("settings"));
                if (gui.has("panels")) {
                    for (var e : gui.getAsJsonObject("panels").entrySet()) {
                        JsonArray a = e.getValue().getAsJsonArray();
                        PANELS.put(e.getKey(), new int[]{a.get(0).getAsInt(), a.get(1).getAsInt()});
                    }
                }
            }
        } catch (Exception e) {
            ZyronClient.LOGGER.error("Could not read {}, using defaults", FILE, e);
        }
    }

    public static void save() {
        if (manager == null) return;
        JsonObject root = new JsonObject();

        JsonObject feats = new JsonObject();
        for (Feature f : manager.all()) {
            JsonObject o = new JsonObject();
            if (f.persistsEnabled()) o.addProperty("enabled", f.isEnabled());
            o.add("settings", saveSettings(f.settings()));
            feats.add(f.name, o);
        }
        root.add("features", feats);

        JsonObject gui = new JsonObject();
        gui.add("settings", saveSettings(Theme.SETTINGS));
        JsonObject panels = new JsonObject();
        PANELS.forEach((k, v) -> {
            JsonArray a = new JsonArray();
            a.add(v[0]);
            a.add(v[1]);
            panels.add(k, a);
        });
        gui.add("panels", panels);
        root.add("gui", gui);

        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(root));
        } catch (IOException e) {
            ZyronClient.LOGGER.error("Could not write {}", FILE, e);
        }
    }

    private static JsonObject saveSettings(List<Setting<?>> settings) {
        JsonObject o = new JsonObject();
        for (Setting<?> s : settings) o.add(s.name, s.save());
        return o;
    }

    private static void loadSettings(List<Setting<?>> settings, JsonObject o) {
        for (Setting<?> s : settings) {
            if (!o.has(s.name)) continue;
            try {
                s.load(o.get(s.name));
            } catch (Exception e) {
                ZyronClient.LOGGER.warn("Bad value for setting '{}', keeping default", s.name);
            }
        }
    }
}
