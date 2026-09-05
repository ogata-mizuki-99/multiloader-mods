package com.ogatamizuki.lookalike;

public final class WerewolfBundleDetection {
    private WerewolfBundleDetection() {
    }

    public static boolean isBundled() {
        return LookalikePlatform.isModLoaded("werewolf");
    }
}
