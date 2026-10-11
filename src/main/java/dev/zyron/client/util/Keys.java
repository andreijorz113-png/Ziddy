package dev.zyron.client.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

/**
 * All raw keyboard polling goes through here. If a Minecraft update changes the InputConstants
 * signature, this is the only line that needs fixing.
 */
public final class Keys {
    private Keys() {
    }

    public static boolean isDown(int glfwKey) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), glfwKey);
    }
}
