package dev.zyron.client.feature;

public enum Category {
    RENDER("Render"),
    WORLD("World"),
    HUD("HUD"),
    MISC("Misc");

    public final String title;

    Category(String title) {
        this.title = title;
    }
}
