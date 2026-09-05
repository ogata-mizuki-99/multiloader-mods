package com.ogatamizuki.sleep;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 深い睡眠を経て起床したときに HP を全快する。
 * ベッドから途中で降りただけの起床では回復しない。
 */
public final class SleepWakeFullHeal {
    private static final Map<UUID, Boolean> REACHED_DEEP_SLEEP = new ConcurrentHashMap<>();

    private SleepWakeFullHeal() {
    }

    public static void clear(UUID playerId) {
        REACHED_DEEP_SLEEP.remove(playerId);
    }

    public static void clearAll() {
        REACHED_DEEP_SLEEP.clear();
    }

    /** 深い睡眠完了（時刻スキップ等）の直前に呼ぶ。 */
    public static void markDeepSleepReached(ServerPlayer player) {
        REACHED_DEEP_SLEEP.put(player.getUUID(), Boolean.TRUE);
    }

    public static void fullHealIfEnabled(ServerPlayer player) {
        if (!SleepCommon.healWhileSleeping) {
            return;
        }
        if (player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /**
     * 毎サーバー tick 呼び出す。
     * 深い睡眠に一度でも達したあと、非睡眠になったフレームで全快する。
     */
    public static void onServerPlayerTick(ServerPlayer player) {
        if (!SleepCommon.healWhileSleeping) {
            REACHED_DEEP_SLEEP.remove(player.getUUID());
            return;
        }

        UUID id = player.getUUID();
        if (player.isSleeping()) {
            if (player.isSleepingLongEnough()) {
                REACHED_DEEP_SLEEP.put(id, Boolean.TRUE);
            }
            return;
        }

        if (REACHED_DEEP_SLEEP.remove(id) != null) {
            fullHealIfEnabled(player);
        }
    }
}
