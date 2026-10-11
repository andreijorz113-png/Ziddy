package dev.zyron.client.feature.impl;

import dev.zyron.client.feature.Category;
import dev.zyron.client.feature.Feature;
import dev.zyron.client.setting.BoolSetting;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Client-side infinite Night Vision. Nothing is sent to the server. */
public class Fullbright extends Feature {
    private final BoolSetting showIcon = add(new BoolSetting("Show potion icon", false));

    public Fullbright() {
        super("Fullbright", "Maximum brightness everywhere (client-side Night Vision).", Category.RENDER);
    }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null) return;
        MobEffectInstance current = p.getEffect(MobEffects.NIGHT_VISION);
        if (current != null && current.isInfiniteDuration() && current.showIcon() == showIcon.get()) return;
        if (current != null && !current.isInfiniteDuration()) return; // a real potion is active, leave it alone
        if (current != null) p.removeEffect(MobEffects.NIGHT_VISION);
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0,
                false, false, showIcon.get()));
    }

    @Override
    protected void onDisable() {
        var p = mc.player;
        if (p == null) return;
        MobEffectInstance current = p.getEffect(MobEffects.NIGHT_VISION);
        if (current != null && current.isInfiniteDuration()) p.removeEffect(MobEffects.NIGHT_VISION);
    }
}
