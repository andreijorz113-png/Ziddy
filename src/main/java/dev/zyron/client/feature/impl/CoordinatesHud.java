package dev.zyron.client.feature.impl;

import dev.zyron.client.feature.HudFeature;
import dev.zyron.client.setting.BoolSetting;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CoordinatesHud extends HudFeature {
    private final BoolSetting direction = add(new BoolSetting("Show facing", true));
    private final BoolSetting otherDimension = add(new BoolSetting("Show Overworld/Nether equivalent", true));

    public CoordinatesHud() {
        super("Coordinates", "Shows your position, facing and the matching Nether/Overworld coordinates.", 6, 24);
        setEnabled(true);
    }

    @Override
    protected List<String> lines() {
        var p = mc.player;
        if (p == null || mc.level == null) return List.of();
        List<String> out = new ArrayList<>();
        out.add(String.format(Locale.ROOT, "XYZ: %.1f / %.1f / %.1f", p.getX(), p.getY(), p.getZ()));
        if (direction.get()) out.add("Facing: " + p.getDirection().getName());
        if (otherDimension.get()) {
            boolean nether = mc.level.dimension() == Level.NETHER;
            double f = nether ? 8.0 : 0.125;
            out.add(String.format(Locale.ROOT, "%s: %.1f / %.1f", nether ? "Overworld" : "Nether", p.getX() * f, p.getZ() * f));
        }
        return out;
    }
}
