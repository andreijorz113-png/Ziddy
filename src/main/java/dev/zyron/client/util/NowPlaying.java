package dev.zyron.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Reads the track the game's music manager is currently playing. This is the only place that touches
 * MusicManager, so if a Minecraft update renames the method, only this file needs a fix.
 */
public final class NowPlaying {
    private NowPlaying() {
    }

    /** Display title of the current track, or null when nothing is playing. */
    public static String title() {
        String key = Minecraft.getInstance().getMusicManager().getCurrentMusicTranslationKey();
        if (key == null || key.isEmpty()) return null;

        String translated = Component.translatable(key).getString();
        if (!translated.equals(key)) return translated;

        // No translation entry: prettify the last path segment ("music.game.aerie" -> "Aerie").
        String last = key.substring(key.lastIndexOf('.') + 1).replace('_', ' ');
        return last.isEmpty() ? key : last.substring(0, 1).toUpperCase(Locale.ROOT) + last.substring(1);
    }
}
