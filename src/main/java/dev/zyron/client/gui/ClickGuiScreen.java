package dev.zyron.client.gui;

import dev.zyron.client.config.ConfigManager;
import dev.zyron.client.feature.Category;
import dev.zyron.client.feature.Feature;
import dev.zyron.client.feature.FeatureManager;
import dev.zyron.client.setting.*;
import dev.zyron.client.util.KeyNames;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.DoubleConsumer;

/**
 * Right Shift ClickGUI. Left-click a module to toggle it, right-click it to open its settings window.
 * Right-click a category header for GUI theme settings. Drag any header to move a window.
 */
public class ClickGuiScreen extends Screen {
    private static final int PANEL_W = 112;
    private static final int HEADER_H = 17;
    private static final int ROW_H = 15;
    private static final int WIN_W = 176;
    private static final int PAD = 6;

    private final List<Window> windows = new ArrayList<>(); // last = drawn on top
    private Window dragWindow;
    private int dragOffX, dragOffY;
    private SliderDrag slider;
    private KeybindSetting listening;

    private record SliderDrag(int x, int w, DoubleConsumer apply) {
    }

    public ClickGuiScreen(FeatureManager features) {
        super(Component.literal("Zyron Client"));
        int i = 0;
        for (Category c : Category.values()) {
            List<Feature> list = features.in(c);
            if (list.isEmpty()) continue;
            int[] saved = ConfigManager.PANELS.get(c.name());
            int px = saved != null ? saved[0] : 14 + i * (PANEL_W + 10);
            int py = saved != null ? saved[1] : 14;
            windows.add(new Panel(c, list, px, py));
            i++;
        }
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (Theme.DIM.get()) g.fill(0, 0, width, height, 0x80000000);
        Window top = windowAt(mouseX, mouseY);
        for (Window w : windows) w.render(g, mouseX, mouseY, w == top);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int btn = event.button();
        listening = null; // clicking anywhere cancels key capture unless a keybind row re-arms it
        Window hit = windowAt(mx, my);
        if (hit == null) return super.mouseClicked(event, doubleClick);
        windows.remove(hit);
        windows.add(hit);
        hit.click(mx, my, btn);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (slider != null) {
            applySlider(event.x());
            return true;
        }
        if (dragWindow != null) {
            dragWindow.x = Math.max(0, Math.min(width - dragWindow.w, (int) event.x() - dragOffX));
            dragWindow.y = Math.max(0, Math.min(height - HEADER_H, (int) event.y() - dragOffY));
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (windowAt(mx, my) instanceof SettingsWindow s && s.maxScroll() > 0) {
            s.scroll = (int) Math.max(0, Math.min(s.maxScroll(), s.scroll - scrollY * 14));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        slider = null;
        dragWindow = null;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (listening != null) {
            if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) listening.set(KeybindSetting.NONE);
            else if (key != GLFW.GLFW_KEY_ESCAPE) listening.set(key);
            listening = null;
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void removed() {
        for (Window w : windows) {
            if (w instanceof Panel p) ConfigManager.PANELS.put(p.category.name(), new int[]{p.x, p.y});
        }
        ConfigManager.save();
    }

    // ---------------------------------------------------------------- helpers

    private Window windowAt(double mx, double my) {
        for (int i = windows.size() - 1; i >= 0; i--) {
            if (windows.get(i).contains(mx, my)) return windows.get(i);
        }
        return null;
    }

    private void beginDrag(Window w, double mx, double my) {
        dragWindow = w;
        dragOffX = (int) mx - w.x;
        dragOffY = (int) my - w.y;
    }

    private void applySlider(double mx) {
        double f = Math.max(0, Math.min(1, (mx - slider.x()) / slider.w()));
        slider.apply().accept(f);
    }

    private void openSettings(Object owner, String title, String desc, List<Setting<?>> settings, int nearX, int nearY, int ownerW) {
        for (Window w : windows) {
            if (w instanceof SettingsWindow s && s.owner == owner) { // already open: bring to front
                windows.remove(w);
                windows.add(w);
                return;
            }
        }
        SettingsWindow win = new SettingsWindow(owner, title, desc, settings, 0, 0);
        int wx = nearX + ownerW + 6;
        if (wx + WIN_W > width) wx = Math.max(0, nearX - WIN_W - 6);
        win.x = wx;
        win.y = Math.max(0, Math.min(nearY, height - win.height()));
        windows.add(win);
    }

    // ---------------------------------------------------------------- windows

    private abstract class Window {
        int x, y;
        final int w;
        final String title;

        Window(String title, int x, int y, int w) {
            this.title = title;
            this.x = x;
            this.y = y;
            this.w = w;
        }

        abstract int bodyHeight();

        abstract void render(GuiGraphicsExtractor g, int mx, int my, boolean hover);

        abstract void click(double mx, double my, int btn);

        int height() {
            return HEADER_H + bodyHeight();
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + height();
        }

        boolean inHeader(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + HEADER_H;
        }

        void drawFrame(GuiGraphicsExtractor g) {
            Theme.rect(g, x, y, w, height(), Theme.BG);
            Theme.border(g, x, y, w, height(), Theme.accent());
            Theme.rect(g, x + 1, y + HEADER_H - 1, w - 2, 1, Theme.withAlpha(Theme.accent(), 0x70));
        }
    }

    /** One category column, e.g. RENDER, listing its modules with toggle dots. */
    private final class Panel extends Window {
        final Category category;
        final List<Feature> list;

        Panel(Category category, List<Feature> list, int x, int y) {
            super(category.title, x, y, PANEL_W);
            this.category = category;
            this.list = list;
        }

        @Override
        int bodyHeight() {
            return list.size() * ROW_H + 2;
        }

        @Override
        void render(GuiGraphicsExtractor g, int mx, int my, boolean hover) {
            drawFrame(g);
            String t = title.toUpperCase(Locale.ROOT);
            Theme.text(g, t, x + (w - Theme.textWidth(t)) / 2, y + 5, Theme.TEXT);
            int ry = y + HEADER_H;
            for (Feature f : list) {
                boolean over = hover && mx >= x && mx < x + w && my >= ry && my < ry + ROW_H;
                if (over) Theme.rect(g, x + 1, ry, w - 2, ROW_H, Theme.ROW_HOVER);
                Theme.text(g, f.name, x + PAD, ry + 4, f.isEnabled() ? Theme.TEXT : Theme.TEXT_DIM);
                int cx = x + w - PAD - 3, cy = ry + ROW_H / 2;
                Theme.circle(g, cx, cy, 4, f.isEnabled() ? Theme.accent() : 0xFF3A444F);
                Theme.circle(g, cx, cy, 3, f.isEnabled() ? Theme.accent() : Theme.DOT_OFF);
                ry += ROW_H;
            }
        }

        @Override
        void click(double mx, double my, int btn) {
            if (inHeader(mx, my)) {
                if (btn == 0) beginDrag(this, mx, my);
                else if (btn == 1) openSettings(Theme.SETTINGS, "GUI Settings", "Theme options for this menu.",
                        Theme.SETTINGS, x, y, w);
                return;
            }
            int idx = (int) ((my - y - HEADER_H) / ROW_H);
            if (idx < 0 || idx >= list.size()) return;
            Feature f = list.get(idx);
            if (btn == 0) f.toggle();
            else if (btn == 1) {
                int rowY = y + HEADER_H + idx * ROW_H;
                openSettings(f, f.name, f.description, f.settings(), x, rowY, w);
            }
        }
    }

    /** The right-click window: one row per setting. */
    private final class SettingsWindow extends Window {
        final Object owner;
        final List<String> descLines;
        final List<Setting<?>> settings;
        final Set<ColorSetting> expanded = new HashSet<>();
        int scroll;

        SettingsWindow(Object owner, String title, String desc, List<Setting<?>> settings, int x, int y) {
            super(title, x, y, WIN_W);
            this.owner = owner;
            this.settings = settings;
            this.descLines = Theme.wrap(desc, WIN_W - 2 * PAD);
        }

        List<Setting<?>> visible() {
            return settings.stream().filter(Setting::isVisible).toList();
        }

        int descHeight() {
            return descLines.isEmpty() ? 0 : descLines.size() * 10 + 4;
        }

        int rowsTop() {
            return y + HEADER_H + 2 + descHeight() + 2 - scroll;
        }

        int rowHeight(Setting<?> s) {
            if (s instanceof NumberSetting) return 24;
            if (s instanceof ColorSetting c) return expanded.contains(c) ? 16 + 4 * 11 : 16;
            return 16;
        }

        int fullBody() {
            int h = 2 + descHeight() + 2 + 2;
            for (Setting<?> s : visible()) h += rowHeight(s);
            return h;
        }

        int maxScroll() {
            return Math.max(0, fullBody() - bodyHeight());
        }

        /** Capped to the screen height; anything taller scrolls with the mouse wheel. */
        @Override
        int bodyHeight() {
            return Math.min(fullBody(), Math.max(80, ClickGuiScreen.this.height - HEADER_H - 16));
        }

        @Override
        void render(GuiGraphicsExtractor g, int mx, int my, boolean hover) {
            drawFrame(g);
            Theme.text(g, title, x + PAD, y + 5, Theme.TEXT);
            boolean overClose = hover && inHeader(mx, my) && mx >= x + w - 14;
            Theme.text(g, "x", x + w - 11, y + 5, overClose ? Theme.accent() : Theme.TEXT_DIM);

            scroll = Math.max(0, Math.min(scroll, maxScroll()));
            g.enableScissor(x + 1, y + HEADER_H, x + w - 1, y + height() - 1);
            int ry = y + HEADER_H + 4 - scroll;
            for (String line : descLines) {
                Theme.text(g, line, x + PAD, ry, Theme.TEXT_DIM);
                ry += 10;
            }
            ry = rowsTop();
            for (Setting<?> s : visible()) {
                renderRow(g, s, ry, mx, my, hover);
                ry += rowHeight(s);
            }
            g.disableScissor();

            if (maxScroll() > 0) {
                int trackH = bodyHeight() - 4;
                int barH = Math.max(12, trackH * bodyHeight() / fullBody());
                int barY = y + HEADER_H + 2 + (trackH - barH) * scroll / maxScroll();
                Theme.rect(g, x + w - 3, barY, 2, barH, Theme.withAlpha(Theme.accent(), 0xA0));
            }
        }

        private void renderRow(GuiGraphicsExtractor g, Setting<?> s, int ry, int mx, int my, boolean hover) {
            int lx = x + PAD, rx = x + w - PAD;
            boolean over = hover && mx >= x && mx < x + w && my >= ry && my < ry + 16;
            if (over) Theme.rect(g, x + 1, ry, w - 2, 16, Theme.ROW_HOVER);
            Theme.text(g, s.name, lx, ry + 4, Theme.TEXT);

            if (s instanceof BoolSetting b) {
                Theme.border(g, rx - 9, ry + 3, 9, 9, Theme.accent());
                if (b.get()) Theme.rect(g, rx - 7, ry + 5, 5, 5, Theme.accent());
            } else if (s instanceof NumberSetting n) {
                Theme.textRight(g, n.display(), rx, ry + 4, Theme.accent());
                int bw = w - 2 * PAD;
                Theme.rect(g, lx, ry + 16, bw, 4, Theme.TRACK);
                int fill = (int) Math.round(bw * n.fraction());
                Theme.rect(g, lx, ry + 16, fill, 4, Theme.accent());
                Theme.rect(g, lx + Math.max(0, Math.min(bw - 2, fill - 1)), ry + 14, 2, 8, Theme.TEXT);
            } else if (s instanceof ModeSetting m) {
                Theme.textRight(g, "< " + m.get() + " >", rx, ry + 4, Theme.accent());
            } else if (s instanceof ColorSetting c) {
                Theme.border(g, rx - 18, ry + 3, 18, 10, Theme.TEXT_DIM);
                Theme.rect(g, rx - 17, ry + 4, 16, 8, c.get() | 0xFF000000);
                if (expanded.contains(c)) {
                    String names = "RGBA";
                    for (int i = 0; i < 4; i++) {
                        int cy = ry + 16 + i * 11;
                        int bx = lx + 14, bw = w - 2 * PAD - 14 - 26;
                        Theme.text(g, String.valueOf(names.charAt(i)), lx + 2, cy + 1, Theme.TEXT_DIM);
                        Theme.rect(g, bx, cy + 3, bw, 4, Theme.TRACK);
                        Theme.rect(g, bx, cy + 3, (int) Math.round(bw * c.channel(i) / 255.0), 4, Theme.accent());
                        Theme.textRight(g, Integer.toString(c.channel(i)), rx, cy + 1, Theme.TEXT_DIM);
                    }
                }
            } else if (s instanceof KeybindSetting k) {
                String label = listening == k ? "[ ... ]" : "[" + KeyNames.of(k.get()) + "]";
                Theme.textRight(g, label, rx, ry + 4, Theme.accent());
            }
        }

        @Override
        void click(double mx, double my, int btn) {
            if (inHeader(mx, my)) {
                if (mx >= x + w - 14) windows.remove(this);
                else if (btn == 0) beginDrag(this, mx, my);
                return;
            }
            int ry = rowsTop();
            for (Setting<?> s : visible()) {
                int h = rowHeight(s);
                if (my >= ry && my < ry + h) {
                    clickRow(s, ry, mx, my, btn);
                    return;
                }
                ry += h;
            }
        }

        private void clickRow(Setting<?> s, int ry, double mx, double my, int btn) {
            if (s instanceof BoolSetting b) {
                if (btn == 0) b.set(!b.get());
            } else if (s instanceof NumberSetting n) {
                if (btn == 0 && my >= ry + 12) {
                    slider = new SliderDrag(x + PAD, w - 2 * PAD, n::setFraction);
                    applySlider(mx);
                }
            } else if (s instanceof ModeSetting m) {
                if (btn == 0) m.cycle(1);
                else if (btn == 1) m.cycle(-1);
            } else if (s instanceof ColorSetting c) {
                if (my < ry + 16) {
                    if (btn == 0 && !expanded.remove(c)) expanded.add(c);
                } else if (btn == 0) {
                    int idx = (int) ((my - (ry + 16)) / 11);
                    if (idx >= 0 && idx < 4) {
                        slider = new SliderDrag(x + PAD + 14, w - 2 * PAD - 14 - 26,
                                f -> c.setChannel(idx, (int) Math.round(f * 255)));
                        applySlider(mx);
                    }
                }
            } else if (s instanceof KeybindSetting k) {
                if (btn == 0) listening = k;
                else if (btn == 1) k.set(KeybindSetting.NONE);
            }
        }
    }
}
