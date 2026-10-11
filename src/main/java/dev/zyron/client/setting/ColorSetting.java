package dev.zyron.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** ARGB color. Channels: 0 = R, 1 = G, 2 = B, 3 = A. */
public class ColorSetting extends Setting<Integer> {
    public ColorSetting(String name, int argb) {
        super(name, argb);
    }

    public int channel(int i) {
        int v = value;
        return switch (i) {
            case 0 -> (v >> 16) & 255;
            case 1 -> (v >> 8) & 255;
            case 2 -> v & 255;
            default -> (v >>> 24) & 255;
        };
    }

    public void setChannel(int i, int c) {
        c = Math.max(0, Math.min(255, c));
        int shift = switch (i) {
            case 0 -> 16;
            case 1 -> 8;
            case 2 -> 0;
            default -> 24;
        };
        value = (value & ~(255 << shift)) | (c << shift);
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
