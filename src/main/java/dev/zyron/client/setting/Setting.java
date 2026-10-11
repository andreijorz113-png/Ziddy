package dev.zyron.client.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

/** Base class for every customizable value a feature exposes in its right-click settings window. */
public abstract class Setting<T> {
    public final String name;
    protected T value;
    private BooleanSupplier visibleWhen = () -> true;

    protected Setting(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    /** Show this setting only while the condition is true (e.g. a sub-option of a toggle). */
    public void visibleWhen(BooleanSupplier condition) {
        this.visibleWhen = condition;
    }

    /** Never show this setting in the GUI (still saved to the config). */
    public void hide() {
        this.visibleWhen = () -> false;
    }

    public boolean isVisible() {
        return visibleWhen.getAsBoolean();
    }

    public abstract JsonElement save();

    public abstract void load(JsonElement element);
}
