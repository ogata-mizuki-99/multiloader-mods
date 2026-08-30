package com.ogatamizuki.radialteleport.client;

import net.minecraft.client.Minecraft;

public final class RadialTeleportMouseCapture {
    private static boolean restoreGrabOnEnd;

    private RadialTeleportMouseCapture() {
    }

    public static void captureForRadialMenu(Minecraft mc) {
        restoreGrabOnEnd = mc.mouseHandler.isMouseGrabbed();
        mc.mouseHandler.releaseMouse();
    }

    public static void restore(Minecraft mc) {
        if (!restoreGrabOnEnd) {
            return;
        }

        if (mc.screen == null && mc.player != null) {
            mc.mouseHandler.grabMouse();
            mc.mouseHandler.setIgnoreFirstMove();
        }

        restoreGrabOnEnd = false;
    }
}
