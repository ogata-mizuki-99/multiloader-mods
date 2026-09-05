package com.ogatamizuki.lookalike.client;

import net.minecraft.client.Minecraft;

public final class LookalikeMouseCapture {
    private static boolean restoreGrabOnEnd;

    private LookalikeMouseCapture() {
    }

    public static void captureForRadialMenu(Minecraft mc) {
        restoreGrabOnEnd = mc.mouseHandler.isMouseGrabbed();
        mc.mouseHandler.releaseMouse();
    }

    public static void restore(Minecraft mc) {
        if (!restoreGrabOnEnd) {
            return;
        }

        if (mc.gui.screen() == null && mc.player != null) {
            mc.mouseHandler.grabMouse();
            mc.mouseHandler.setIgnoreFirstMove();
        }

        restoreGrabOnEnd = false;
    }
}
