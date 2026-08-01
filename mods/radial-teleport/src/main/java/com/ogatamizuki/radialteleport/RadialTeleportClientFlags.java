package com.ogatamizuki.radialteleport;

/**
 * Dedicated 接続中にサーバーから同期されたクライアント向けフラグ。
 */
public final class RadialTeleportClientFlags {
    private static boolean hasServerSync;
    private static boolean enableWaypoints = true;

    private RadialTeleportClientFlags() {
    }

    public static void apply(RadialTeleportClientFlagsPayload payload) {
        hasServerSync = true;
        enableWaypoints = payload.enableWaypoints();
        RadialTeleportMod.LOGGER.info(
                "Radial Teleport client flags synced from server: enableWaypoints={}",
                enableWaypoints);
    }

    public static void clear() {
        hasServerSync = false;
        enableWaypoints = Config.ENABLE_WAYPOINTS.get();
    }

    public static boolean enableWaypoints() {
        if (hasServerSync) {
            return enableWaypoints;
        }
        return Config.ENABLE_WAYPOINTS.get();
    }
}
