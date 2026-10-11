package dev.zyron.client.gui;

import dev.zyron.client.feature.Category;
import dev.zyron.client.feature.Feature;
import dev.zyron.client.feature.FeatureManager;
import dev.zyron.client.feature.HudFeature;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class HudRenderer {
    private HudRenderer() {
    }

    public static void register(FeatureManager features) {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("zyronclient", "hud"),
                (graphics, tracker) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.options.hideGui || mc.player == null) return;
                    for (Feature f : features.in(Category.HUD)) {
                        if (f.isEnabled() && f instanceof HudFeature hud) hud.renderHud(graphics);
                    }
                });
    }
}
