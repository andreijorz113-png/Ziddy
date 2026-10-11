package dev.zyron.client.gui;

import dev.zyron.client.setting.BoolSetting;
import dev.zyron.client.setting.ColorSetting;
import dev.zyron.client.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Colors and tiny drawing helpers shared by the ClickGUI and HUD editor. */
public final class Theme {
    public static final ColorSetting ACCENT = new ColorSetting("Accent color", 0xFF35E0D0);
    public static final BoolSetting DIM = new BoolSetting("Dim background", true);
    public static final List<Setting<?>> SETTINGS = List.of(ACCENT, DIM);

    public static final int BG = 0xE00B0F14;
    public static final int ROW_HOVER = 0xFF161C24;
    public static final int TEXT = 0xFFE6EDF3;
    public static final int TEXT_DIM = 0xFF8B98A5;
    public static final int TRACK = 0xFF1E252D;
    public static final int DOT_OFF = 0xFF2A323B;

    private Theme() {
    }

    public static int accent() {
        return ACCENT.get() | 0xFF000000;
    }

    public static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }

    public static void rect(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }

    public static void border(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void circle(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.floor(Math.sqrt(r * r + r - dy * dy));
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    public static void text(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        g.text(Minecraft.getInstance().font, s, x, y, color, false);
    }

    public static void textRight(GuiGraphicsExtractor g, String s, int rightX, int y, int color) {
        text(g, s, rightX - textWidth(s), y, color);
    }

    public static int textWidth(String s) {
        return Minecraft.getInstance().font.width(s);
    }

    public static List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (textWidth(test) > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }
}
