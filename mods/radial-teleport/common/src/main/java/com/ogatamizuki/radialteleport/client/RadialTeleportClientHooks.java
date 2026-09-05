package com.ogatamizuki.radialteleport.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class RadialTeleportClientHooks {
    private static boolean wasUsingCompass = false;

    private RadialTeleportClientHooks() {}

    public static void onWaypointScreenOpened() {
        Minecraft mc = Minecraft.getInstance();
        if (RadialTeleportSession.isActive()) {
            RadialTeleportSession.end(mc);
        }
        LocalPlayer player = mc.player;
        if (player != null) {
            wasUsingCompass = isActivelyUsingCompass(player)
                    || isSpectatorRadialUse(mc, player);
        }
    }

    public static void onWaypointScreenClosed() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null) {
            wasUsingCompass = isActivelyUsingCompass(player)
                    || isSpectatorRadialUse(mc, player);
        }
        if (RadialTeleportSession.isActive()) {
            RadialTeleportSession.end(mc);
        }
    }

    public static boolean isActivelyUsingCompass(LocalPlayer player) {
        return player.isUsingItem()
                && player.getUseItem().is(com.ogatamizuki.radialteleport.RadialTeleportCommon.TELEPORT_COMPASS.get());
    }

    public static boolean isHoldingCompass(LocalPlayer player) {
        return player.getMainHandItem().is(com.ogatamizuki.radialteleport.RadialTeleportCommon.TELEPORT_COMPASS.get())
                || player.getOffhandItem().is(com.ogatamizuki.radialteleport.RadialTeleportCommon.TELEPORT_COMPASS.get());
    }

    public static boolean isSpectatorRadialUse(Minecraft mc, LocalPlayer player) {
        return player.isSpectator() && mc.options.keyUse.isDown();
    }

    public static boolean wasUsingCompass() {
        return wasUsingCompass;
    }

    public static void setWasUsingCompass(boolean value) {
        wasUsingCompass = value;
    }
}
