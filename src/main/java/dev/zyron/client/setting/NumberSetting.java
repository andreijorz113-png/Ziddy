package dev.zyron.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

public class NumberSetting extends Setting<Double> {
    public final double min;
    public final double max;
    public final double step;

    public NumberSetting(String name, double def, double min, double max, double step) {
        super(name, def);
        this.min = min;
        this.max = max;
        this.step = step;
        set(def);
    }

    @Override
    public void set(Double v) {
        double snapped = Math.round((v - min) / step) * step + min;
        value = Math.max(min, Math.min(max, snapped));
    }

    public int getInt() {
        return (int) Math.round(value);
    }

    public float getFloat() {
        return value.floatValue();
    }

    public double fraction() {
        return (value - min) / (max - min);
    }

    public void setFraction(double f) {
        set(min + Math.max(0, Math.min(1, f)) * (max - min));
    }

    public String display() {
        if (step >= 1) return Integer.toString(getInt());
        return String.format(Locale.ROOT, "%.2f", value);
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement element) {
        set(element.getAsDouble());
    }
}
