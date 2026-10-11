package dev.zyron.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class KeybindSetting extends Setting<Integer> {
    public static final int NONE = -1;

    public KeybindSetting(String name, int key) {
        super(name, key);
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement element) {
        value = element.getAsInt();
    }
}
