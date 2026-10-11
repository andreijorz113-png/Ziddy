package dev.zyron.client.feature.impl;

import dev.zyron.client.feature.HudFeature;
import dev.zyron.client.setting.BoolSetting;
import dev.zyron.client.setting.NumberSetting;
import dev.zyron.client.util.NowPlaying;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shows the in-game music track that's currently playing, with optional elapsed time. */
public class MusicHud extends HudFeature {
    private final BoolSetting showLabel = add(new BoolSetting("Show 'Now Playing' label", true));
    private final BoolSetting showElapsed = add(new BoolSetting("Show elapsed time", true));
    private final BoolSetting hideWhenSilent = add(new BoolSetting("Hide when silent", true));
    private final NumberSetting maxWidth = add(new NumberSetting("Max title width", 160, 60, 400, 5));

    private String lastTitle;
    private long startedAt;

    public MusicHud() {
        super("Music HUD", "Shows the game music track that's currently playing.", 6, 96);
    }

    @Override
    protected List<String> lines() {
        String title = NowPlaying.title();
        if (!java.util.Objects.equals(title, lastTitle)) {
            lastTitle = title;
            startedAt = System.currentTimeMillis();
        }
        if (title == null) {
            return hideWhenSilent.get() ? List.of() : List.of("\u266A Nothing playing");
        }

        List<String> out = new ArrayList<>();
        if (showLabel.get()) out.add("Now Playing");
        String shown = mc.font.plainSubstrByWidth(title, maxWidth.getInt());
        if (shown.length() < title.length()) shown += "...";
        out.add("\u266A " + shown);
        if (showElapsed.get()) {
            long sec = (System.currentTimeMillis() - startedAt) / 1000;
            out.add(String.format(Locale.ROOT, "%d:%02d", sec / 60, sec % 60));
        }
        return out;
    }
}
