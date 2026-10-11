package dev.zyron.client.feature;

import dev.zyron.client.setting.BoolSetting;
import dev.zyron.client.setting.ColorSetting;
import dev.zyron.client.setting.NumberSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/** A draggable text box on the HUD. Position is stored in two hidden settings and edited in the HUD Editor. */
public abstract class HudFeature extends Feature {
    private final NumberSetting x = add(new NumberSetting("X", 6, 0, 8000, 1));
    private final NumberSetting y = add(new NumberSetting("Y", 6, 0, 8000, 1));
    protected final ColorSetting color = add(new ColorSetting("Text color", 0xFFFFFFFF));
    protected final BoolSetting background = add(new BoolSetting("Background", true));
    protected final BoolSetting shadow = add(new BoolSetting("Text shadow", true));

    private final int defX;
    private final int defY;
    private int lastW = 60;
    private int lastH = 12;

    protected HudFeature(String name, String description, int defX, int defY) {
        super(name, description, Category.HUD);
        this.defX = defX;
        this.defY = defY;
        x.hide();
        y.hide();
        x.set((double) defX);
        y.set((double) defY);
    }

    /** Lines of text to draw this frame. Return an empty list to draw nothing. */
    protected abstract List<String> lines();

    public final void renderHud(GuiGraphicsExtractor g) {
        List<String> lines = lines();
        if (lines.isEmpty()) return;
        Font font = mc.font;
        int pad = 3;
        int lineH = font.lineHeight + 1;
        int w = 0;
        for (String s : lines) w = Math.max(w, font.width(s));
        w += pad * 2;
        int h = lines.size() * lineH - 1 + pad * 2;
        lastW = w;
        lastH = h;

        int px = px();
        int py = py();
        if (background.get()) g.fill(px, py, px + w, py + h, 0x90000000);
        int ty = py + pad;
        for (String s : lines) {
            g.text(font, s, px + pad, ty, color.get(), shadow.get());
            ty += lineH;
        }
    }

    public int width() {
        return lastW;
    }

    public int height() {
        return lastH;
    }

    public int px() {
        int max = Math.max(0, mc.getWindow().getGuiScaledWidth() - lastW);
        return Math.max(0, Math.min(max, x.getInt()));
    }

    public int py() {
        int max = Math.max(0, mc.getWindow().getGuiScaledHeight() - lastH);
        return Math.max(0, Math.min(max, y.getInt()));
    }

    public void moveTo(int nx, int ny) {
        int maxX = Math.max(0, mc.getWindow().getGuiScaledWidth() - lastW);
        int maxY = Math.max(0, mc.getWindow().getGuiScaledHeight() - lastH);
        x.set((double) Math.max(0, Math.min(maxX, nx)));
        y.set((double) Math.max(0, Math.min(maxY, ny)));
    }

    public void resetPosition() {
        moveTo(defX, defY);
    }
}
