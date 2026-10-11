package dev.zyron.client.feature.impl;

import dev.zyron.client.feature.Category;
import dev.zyron.client.feature.Feature;
import dev.zyron.client.gui.HudEditorScreen;

/** Button-style feature: enabling it opens the HUD editor and flips straight back off. */
public class HudEditorFeature extends Feature {
    public HudEditorFeature() {
        super("HUD Editor", "Drag every HUD element wherever you want it.", Category.MISC);
    }

    @Override
    public boolean persistsEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        mc.setScreen(new HudEditorScreen());
        setEnabled(false);
    }
}
