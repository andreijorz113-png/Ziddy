package dev.zyron.client.feature;

import dev.zyron.client.util.Keys;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FeatureManager {
    private final List<Feature> features = new ArrayList<>();
    private final Map<Feature, Boolean> held = new HashMap<>();

    public void register(Feature feature) {
        features.add(feature);
    }

    public List<Feature> all() {
        return features;
    }

    public List<Feature> in(Category category) {
        return features.stream().filter(f -> f.category == category).toList();
    }

    public <T extends Feature> T get(Class<T> type) {
        for (Feature f : features) if (type.isInstance(f)) return type.cast(f);
        throw new IllegalStateException(type.getSimpleName() + " is not registered");
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        boolean inWorld = mc.player != null && mc.level != null;
        for (Feature f : features) {
            int key = f.bind.get();
            if (key >= 0 && inWorld && mc.screen == null) {
                boolean down = Keys.isDown(key);
                if (down && !held.getOrDefault(f, false)) f.toggle();
                held.put(f, down);
            } else {
                held.put(f, false);
            }
            if (inWorld && f.isEnabled()) f.onTick();
        }
    }
}
