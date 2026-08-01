package com.ogatamizuki.radialteleport;

import net.neoforged.fml.ModList;

final class WerewolfBundleDetection {
    private WerewolfBundleDetection() {
    }

    static boolean isBundled() {
        return ModList.get().getModContainerById("werewolf").isPresent();
    }
}
