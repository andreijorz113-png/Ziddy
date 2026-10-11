package dev.zyron.client.feature;

import dev.zyron.client.render.EspBox;

import java.util.List;

/** Implemented by features that want boxes drawn in the world by EspRenderer. */
public interface EspSource {
    void collectBoxes(List<EspBox> out);
}
