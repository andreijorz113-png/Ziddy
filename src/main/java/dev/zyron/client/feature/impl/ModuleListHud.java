package dev.zyron.client.feature.impl;

import dev.zyron.client.ZyronClient;
import dev.zyron.client.feature.Category;
import dev.zyron.client.feature.Feature;
import dev.zyron.client.feature.HudFeature;

import java.util.Comparator;
import java.util.List;

public class ModuleListHud extends HudFeature {
    public ModuleListHud() {
        super("Module List", "Lists every enabled module.", 6, 78);
        setEnabled(true);
    }

    @Override
    protected List<String> lines() {
        return ZyronClient.features().all().stream()
                .filter(f -> f.isEnabled() && f.showInList.get() && f.category != Category.HUD)
                .sorted(Comparator.comparingInt((Feature f) -> -mc.font.width(f.name)))
                .map(f -> f.name)
                .toList();
    }
}
