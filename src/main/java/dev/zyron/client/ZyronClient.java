package dev.zyron.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.zyron.client.config.ConfigManager;
import dev.zyron.client.feature.FeatureManager;
import dev.zyron.client.feature.impl.*;
import dev.zyron.client.gui.ClickGuiScreen;
import dev.zyron.client.gui.HudRenderer;
import dev.zyron.client.render.EspRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZyronClient implements ClientModInitializer {
    public static final String MOD_ID = "zyronclient";
    public static final String NAME = "Zyron Client";
    public static final String VERSION = "1.1.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static FeatureManager features;
    private static KeyMapping openGui;

    public static FeatureManager features() {
        return features;
    }

    @Override
    public void onInitializeClient() {
        features = new FeatureManager();

        // Render
        features.register(new Fullbright());
        features.register(new StorageEsp());
        // HUD
        features.register(new WatermarkHud());
        features.register(new CoordinatesHud());
        features.register(new FpsHud());
        features.register(new ModuleListHud());
        features.register(new MusicHud());
        // Misc
        features.register(new HudEditorFeature());

        ConfigManager.load(features);

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        openGui = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key." + MOD_ID + ".open_gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());
        HudRenderer.register(features);
        EspRenderer.init(features);

        LOGGER.info("{} v{} loaded", NAME, VERSION);
    }

    private void tick(Minecraft mc) {
        while (openGui.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new ClickGuiScreen(features));
        }
        features.tick();
    }
}
