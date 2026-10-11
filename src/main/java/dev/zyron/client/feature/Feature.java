package dev.zyron.client.feature;

import dev.zyron.client.setting.BoolSetting;
import dev.zyron.client.setting.KeybindSetting;
import dev.zyron.client.setting.Setting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * A toggleable feature. Every feature automatically gets a Keybind and a "Show in module list"
 * setting, so every one of them has a right-click settings window in the ClickGUI.
 */
public abstract class Feature {
    public final String name;
    public final String description;
    public final Category category;
    protected final Minecraft mc = Minecraft.getInstance();

    public final KeybindSetting bind = new KeybindSetting("Keybind", KeybindSetting.NONE);
    public final BoolSetting showInList = new BoolSetting("Show in module list", true);

    private final List<Setting<?>> own = new ArrayList<>();
    private boolean enabled;

    protected Feature(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <S extends Setting<?>> S add(S setting) {
        own.add(setting);
        return setting;
    }

    /** Feature-specific settings first, then the shared ones. */
    public List<Setting<?>> settings() {
        List<Setting<?>> all = new ArrayList<>(own);
        all.add(bind);
        all.add(showInList);
        return all;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        if (value == enabled) return;
        enabled = value;
        if (value) onEnable();
        else onDisable();
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    /** False for "button" features (e.g. HUD Editor) that should not remember an on/off state. */
    public boolean persistsEnabled() {
        return true;
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    /** Called every client tick while enabled and a world is loaded. */
    public void onTick() {
    }
}
