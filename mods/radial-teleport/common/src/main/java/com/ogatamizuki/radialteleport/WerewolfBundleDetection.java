package com.ogatamizuki.radialteleport;

public final class WerewolfBundleDetection {
    private WerewolfBundleDetection() {
    }

    public static boolean isBundled() {
        return RadialTeleportCommon.isModLoaded("werewolf");
    }
}
