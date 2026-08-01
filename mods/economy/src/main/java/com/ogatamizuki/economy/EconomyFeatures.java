package com.ogatamizuki.economy;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 機能フラグの実効値。
 * <ul>
 *   <li>サーバー処理（報酬付与・ETF 更新・報酬チャット集約）: 常にこのプロセスの {@link Config}</li>
 *   <li>残高 HUD（クライアント表示）: サーバー同期済みなら同期値、未同期時はローカル {@link Config}</li>
 * </ul>
 */
public final class EconomyFeatures {
    private EconomyFeatures() {
    }

    /** クライアント HUD。Dedicated 接続中はサーバー同期値を優先。 */
    public static boolean isBalanceHudEnabled() {
        if (FMLEnvironment.getDist() == Dist.CLIENT && EconomyClientFeatureFlags.hasServerSync()) {
            return EconomyClientFeatureFlags.enableBalanceHud();
        }
        return Config.ENABLE_BALANCE_HUD.get();
    }

    /** サーバー権威。このプロセスの Config（Dedicated では server の toml）。 */
    public static boolean isActionRewardsEnabled() {
        return Config.ENABLE_ACTION_REWARDS.get();
    }

    /** サーバー権威。このプロセスの Config（Dedicated では server の toml）。 */
    public static boolean isEtfUpdatesEnabled() {
        return Config.ENABLE_ETF_UPDATES.get();
    }

    /** サーバー権威。報酬チャット集約秒（Dedicated では server の toml）。 */
    public static int rewardChatAggregateSeconds() {
        return Config.REWARD_CHAT_AGGREGATE_SECONDS.get();
    }

    public static void syncToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, EconomyFeatureFlagsPayload.fromConfig());
    }

    public static void syncToAllPlayers() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        PacketDistributor.sendToAllPlayers(EconomyFeatureFlagsPayload.fromConfig());
    }
}
