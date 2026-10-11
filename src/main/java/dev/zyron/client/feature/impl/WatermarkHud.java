package dev.zyron.client.feature.impl;

import dev.zyron.client.ZyronClient;
import dev.zyron.client.feature.HudFeature;

import java.util.List;

public class WatermarkHud extends HudFeature {
    public WatermarkHud() {
        super("Watermark", "Shows the client name and version.", 6, 6);
        color.set(0xFF35E0D0);
        setEnabled(true);
    }

    @Override
    protected List<String> lines() {
        return List.of(ZyronClient.NAME + " v" + ZyronClient.VERSION);
    }
}
