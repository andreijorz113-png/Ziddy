package dev.zyron.client.render;

/** One highlighted box in world coordinates. argb alpha = fill opacity; the outline is always opaque. */
public record EspBox(double minX, double minY, double minZ,
                     double maxX, double maxY, double maxZ,
                     int argb, boolean fill, boolean outline, float thickness, boolean throughWalls) {
}
