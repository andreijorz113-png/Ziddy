package dev.zyron.client.gui;

import dev.zyron.client.ZyronClient;
import dev.zyron.client.config.ConfigManager;
import dev.zyron.client.feature.Category;
import dev.zyron.client.feature.Feature;
import dev.zyron.client.feature.HudFeature;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Drag HUD elements around. Edges and screen center snap. Arrow keys nudge, R resets the selected one. */
public class HudEditorScreen extends Screen {
    private static final int SNAP = 5;

    private HudFeature selected;
    private HudFeature dragging;
    private int offX, offY;

    public HudEditorScreen() {
        super(Component.literal("HUD Editor"));
    }

    private List<HudFeature> elements() {
        List<HudFeature> out = new ArrayList<>();
        for (Feature f : ZyronClient.features().in(Category.HUD)) {
            if (f.isEnabled() && f instanceof HudFeature h) out.add(h);
        }
        return out;
    }

    private static int boxW(HudFeature h) {
        return Math.max(h.width(), 40);
    }

    private static int boxH(HudFeature h) {
        return Math.max(h.height(), 12);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, 0x55000000);
        g.fill(width / 2, 0, width / 2 + 1, height, 0x22FFFFFF);
        g.fill(0, height / 2, width, height / 2 + 1, 0x22FFFFFF);

        for (HudFeature h : elements()) {
            int x = h.px(), y = h.py(), w = boxW(h), ht = boxH(h);
            boolean hot = h == selected || h == dragging || (mx >= x && mx < x + w && my >= y && my < y + ht);
            Theme.border(g, x - 1, y - 1, w + 2, ht + 2, hot ? Theme.accent() : 0x88FFFFFF);
            Theme.text(g, h.name, x, Math.max(0, y - 11), hot ? Theme.accent() : Theme.TEXT_DIM);
        }

        String hint = "Drag to move   |   Arrows nudge (Shift = 5px)   |   R resets   |   Esc to finish";
        Theme.text(g, hint, (width - Theme.textWidth(hint)) / 2, 6, Theme.TEXT);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            List<HudFeature> list = elements();
            for (int i = list.size() - 1; i >= 0; i--) {
                HudFeature h = list.get(i);
                if (event.x() >= h.px() && event.x() < h.px() + boxW(h)
                        && event.y() >= h.py() && event.y() < h.py() + boxH(h)) {
                    selected = dragging = h;
                    offX = (int) event.x() - h.px();
                    offY = (int) event.y() - h.py();
                    return true;
                }
            }
            selected = null;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging == null) return super.mouseDragged(event, dx, dy);
        int w = boxW(dragging), h = boxH(dragging);
        int nx = (int) event.x() - offX;
        int ny = (int) event.y() - offY;
        nx = snap(nx, w, width);
        ny = snap(ny, h, height);
        dragging.moveTo(nx, ny);
        return true;
    }

    /** Snap to the near edge, far edge or center of the screen. */
    private static int snap(int pos, int size, int screen) {
        if (Math.abs(pos) <= SNAP) return 0;
        if (Math.abs(pos + size - screen) <= SNAP) return screen - size;
        if (Math.abs(pos + size / 2 - screen / 2) <= SNAP) return screen / 2 - size / 2;
        return pos;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = null;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (selected != null) {
            int step = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? 5 : 1;
            switch (event.key()) {
                case GLFW.GLFW_KEY_LEFT -> selected.moveTo(selected.px() - step, selected.py());
                case GLFW.GLFW_KEY_RIGHT -> selected.moveTo(selected.px() + step, selected.py());
                case GLFW.GLFW_KEY_UP -> selected.moveTo(selected.px(), selected.py() - step);
                case GLFW.GLFW_KEY_DOWN -> selected.moveTo(selected.px(), selected.py() + step);
                case GLFW.GLFW_KEY_R -> selected.resetPosition();
                default -> {
                    return super.keyPressed(event);
                }
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        ConfigManager.save();
    }
}
