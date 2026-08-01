package com.ogatamizuki.economy;

/**
 * クライアント側に同期されたサーバー機能フラグ。
 * Dedicated Server 接続中は {@link #hasServerSync()} が true になり、ローカル toml より優先される。
 */
public final class EconomyClientFeatureFlags {
    private static boolean hasServerSync;
    private static boolean enableBalanceHud = true;
    private static boolean enableActionRewards = true;
    private static boolean enableEtfUpdates = true;
    private static int rewardChatAggregateSeconds = 2;

    private EconomyClientFeatureFlags() {
    }

    public static void apply(EconomyFeatureFlagsPayload payload) {
        hasServerSync = true;
        enableBalanceHud = payload.enableBalanceHud();
        enableActionRewards = payload.enableActionRewards();
        enableEtfUpdates = payload.enableEtfUpdates();
        rewardChatAggregateSeconds = payload.rewardChatAggregateSeconds();
        EconomyMod.LOGGER.info(
                "Economy feature flags synced from server: hud={}, actionRewards={}, etfUpdates={}, rewardChatAggregateSeconds={}",
                enableBalanceHud, enableActionRewards, enableEtfUpdates, rewardChatAggregateSeconds
        );
    }

    public static void clear() {
        hasServerSync = false;
        enableBalanceHud = Config.ENABLE_BALANCE_HUD.get();
        enableActionRewards = Config.ENABLE_ACTION_REWARDS.get();
        enableEtfUpdates = Config.ENABLE_ETF_UPDATES.get();
        rewardChatAggregateSeconds = Config.REWARD_CHAT_AGGREGATE_SECONDS.get();
    }

    public static boolean hasServerSync() {
        return hasServerSync;
    }

    public static boolean enableBalanceHud() {
        return enableBalanceHud;
    }

    public static boolean enableActionRewards() {
        return enableActionRewards;
    }

    public static boolean enableEtfUpdates() {
        return enableEtfUpdates;
    }

    public static int rewardChatAggregateSeconds() {
        return rewardChatAggregateSeconds;
    }
}
