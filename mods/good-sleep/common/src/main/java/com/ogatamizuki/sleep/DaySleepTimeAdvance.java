package com.ogatamizuki.sleep;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * 昼間睡眠完了時に夜へ時刻を進める共通処理。
 * 仕様: 朝・昼に眠ると夜へ、夜に眠るとバニラどおり朝へ。
 * <p>
 * MC 26.1+ は WorldClock 制のため {@code setDayTime} は使わず、
 * {@code /time set night} 相当のコマンド実行で進める。
 */
public final class DaySleepTimeAdvance {
    /** バニラの深い睡眠判定と同じおおよそ 5 秒。 */
    public static final int SLEEP_TICKS_REQUIRED = 100;

    private DaySleepTimeAdvance() {
    }

    public static boolean isDaytime(ServerLevel level) {
        return !level.isDarkOutside();
    }

    public static void advanceToNight(ServerLevel level) {
        if (!isDaytime(level)) {
            return;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withSuppressedOutput().withLevel(level),
                "time set night"
        );
    }

    /**
     * 昼間の睡眠を完了させ、夜へ進めて起床させる。
     */
    public static void finishDaySleep(ServerLevel level) {
        if (!SleepCommon.allowDaySleep || !isDaytime(level)) {
            return;
        }
        advanceToNight(level);
        for (ServerPlayer player : level.players()) {
            if (!player.isSleeping()) {
                continue;
            }
            SleepWakeFullHeal.markDeepSleepReached(player);
            SleepWakeFullHeal.fullHealIfEnabled(player);
            player.stopSleeping();
            SleepWakeFullHeal.clear(player.getUUID());
        }
    }
}
