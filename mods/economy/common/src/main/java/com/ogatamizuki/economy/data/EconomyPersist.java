package com.ogatamizuki.economy.data;

import com.ogatamizuki.economy.EconomyCommon;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** Economy の WorldSavedData をディスクへ明示的に書き出す。 */
public final class EconomyPersist {
    /** ワールド自動保存と同程度の間隔（tick）。 */
    private static final int PERIODIC_SAVE_INTERVAL_TICKS = 6000;

    private EconomyPersist() {
    }

    public static void saveAll(MinecraftServer server) {
        if (server == null) {
            return;
        }
        try {
            ServerLevel overworld = server.overworld();
            overworld.getDataStorage().saveAndJoin();
            EconomyCommon.LOGGER.debug("Economy SavedData flushed to disk");
        } catch (Exception e) {
            EconomyCommon.LOGGER.error("Failed to flush Economy SavedData", e);
        }
    }

    /** 定期 tick から呼び出し、一定間隔で SavedData をディスクへ書き出す。 */
    public static void onServerTick(MinecraftServer server) {
        if (server == null || server.getTickCount() <= 0 || server.getTickCount() % PERIODIC_SAVE_INTERVAL_TICKS != 0) {
            return;
        }
        saveAll(server);
    }
}
