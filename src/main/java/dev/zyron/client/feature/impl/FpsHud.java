package dev.zyron.client.feature.impl;

import dev.zyron.client.feature.HudFeature;

import java.util.List;

public class FpsHud extends HudFeature {
    public FpsHud() {
        super("FPS", "Shows your frames per second.", 6, 60);
        setEnabled(true);
    }

    @Override
    protected List<String> lines() {
        return List.of("FPS: " + mc.getFps());
    }
}
