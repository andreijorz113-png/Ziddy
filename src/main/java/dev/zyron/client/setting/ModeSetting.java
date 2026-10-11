package dev.zyron.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Arrays;

public class ModeSetting extends Setting<String> {
    public final String[] modes;

    public ModeSetting(String name, String def, String... modes) {
        super(name, def);
        this.modes = modes;
    }

    public boolean is(String mode) {
        return value.equalsIgnoreCase(mode);
    }

    public void cycle(int direction) {
        int i = Arrays.asList(modes).indexOf(value);
        i = Math.floorMod(i + direction, modes.length);
        value = modes[i];
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement element) {
        String s = element.getAsString();
        if (Arrays.asList(modes).contains(s)) value = s;
    }
}
