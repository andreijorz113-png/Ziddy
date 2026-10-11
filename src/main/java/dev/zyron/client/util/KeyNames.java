package dev.zyron.client.util;

import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.Map;

public final class KeyNames {
    private static final Map<Integer, String> SPECIAL = Map.ofEntries(
            Map.entry(GLFW.GLFW_KEY_SPACE, "SPACE"),
            Map.entry(GLFW.GLFW_KEY_TAB, "TAB"),
            Map.entry(GLFW.GLFW_KEY_ENTER, "ENTER"),
            Map.entry(GLFW.GLFW_KEY_LEFT_SHIFT, "LSHIFT"),
            Map.entry(GLFW.GLFW_KEY_RIGHT_SHIFT, "RSHIFT"),
            Map.entry(GLFW.GLFW_KEY_LEFT_CONTROL, "LCTRL"),
            Map.entry(GLFW.GLFW_KEY_RIGHT_CONTROL, "RCTRL"),
            Map.entry(GLFW.GLFW_KEY_LEFT_ALT, "LALT"),
            Map.entry(GLFW.GLFW_KEY_RIGHT_ALT, "RALT"),
            Map.entry(GLFW.GLFW_KEY_UP, "UP"),
            Map.entry(GLFW.GLFW_KEY_DOWN, "DOWN"),
            Map.entry(GLFW.GLFW_KEY_LEFT, "LEFT"),
            Map.entry(GLFW.GLFW_KEY_RIGHT, "RIGHT"),
            Map.entry(GLFW.GLFW_KEY_INSERT, "INSERT"),
            Map.entry(GLFW.GLFW_KEY_HOME, "HOME"),
            Map.entry(GLFW.GLFW_KEY_END, "END"),
            Map.entry(GLFW.GLFW_KEY_PAGE_UP, "PGUP"),
            Map.entry(GLFW.GLFW_KEY_PAGE_DOWN, "PGDN"),
            Map.entry(GLFW.GLFW_KEY_CAPS_LOCK, "CAPS"));

    private KeyNames() {
    }

    public static String of(int key) {
        if (key < 0) return "NONE";
        String special = SPECIAL.get(key);
        if (special != null) return special;
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25) return "F" + (key - GLFW.GLFW_KEY_F1 + 1);
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null) return name.toUpperCase(Locale.ROOT);
        return "KEY" + key;
    }
}
