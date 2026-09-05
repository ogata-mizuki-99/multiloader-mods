package com.ogatamizuki.economy;

import com.ogatamizuki.economy.backend.EconomyEtfPriceScheduler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 機能フラグの実効値。
 */
public final class EconomyFeatures {
    private EconomyFeatures() {
    }

    /** クライアント HUD。Dedicated 接続中はサーバー同期値を優先。 */
    public static boolean isBalanceHudEnabled() {
        if (EconomyPlatform.isClient() && EconomyClientFeatureFlags.hasServerSync()) {
            return EconomyClientFeatureFlags.enableBalanceHud();
        }
        return EconomyRuntimeConfig.enableBalanceHud;
    }

    public static boolean isActionRewardsEnabled() {
        return EconomyRuntimeConfig.enableActionRewards;
    }

    public static boolean isEtfUpdatesEnabled() {
        return EconomyRuntimeConfig.enableEtfUpdates;
    }

    public static int rewardChatAggregateSeconds() {
        return EconomyRuntimeConfig.rewardChatAggregateSeconds;
    }

    public static void syncToPlayer(ServerPlayer player) {
        EconomyPlatform.send(player, EconomyFeatureFlagsPayload.fromConfig());
    }

    public static void syncToAllPlayers() {
        MinecraftServer server = EconomyPlatform.getServer();
        if (server == null) {
            return;
        }
        EconomyFeatureFlagsPayload payload = EconomyFeatureFlagsPayload.fromConfig();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            EconomyPlatform.send(player, payload);
        }
    }

    public static void onConfigReloaded(MinecraftServer server) {
        if (server == null) {
            return;
        }
        server.execute(() -> {
            syncToAllPlayers();
            EconomyEtfPriceScheduler.stop();
            EconomyEtfPriceScheduler.start(server);
            EconomyCommon.LOGGER.info(
                    "Economy feature config reloaded: hud={}, actionRewards={}, etfUpdates={}, rewardChatAggregateSeconds={}",
                    EconomyRuntimeConfig.enableBalanceHud,
                    EconomyRuntimeConfig.enableActionRewards,
                    EconomyRuntimeConfig.enableEtfUpdates,
                    EconomyRuntimeConfig.rewardChatAggregateSeconds
            );
        });
    }
}
