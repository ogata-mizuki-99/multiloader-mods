package com.ogatamizuki.sleep;

/**
 * Dedicated 接続中にサーバーから同期されたクライアント向けフラグ。
 */
public final class SleepClientFlags {
    private static boolean hasServerSync;
    private static boolean healWhileSleeping = true;

    private SleepClientFlags() {
    }

    public static void apply(SleepClientFlagsPayload payload) {
        hasServerSync = true;
        healWhileSleeping = payload.healWhileSleeping();
        SleepMod.LOGGER.info(
                "Good Sleep client flags synced from server: healWhileSleeping={}",
                healWhileSleeping);
    }

    public static void clear() {
        hasServerSync = false;
        healWhileSleeping = Config.HEAL_WHILE_SLEEPING.get();
    }

    public static boolean healWhileSleeping() {
        if (hasServerSync) {
            return healWhileSleeping;
        }
        return Config.HEAL_WHILE_SLEEPING.get();
    }
}
