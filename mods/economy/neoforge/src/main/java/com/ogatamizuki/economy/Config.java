package com.ogatamizuki.economy;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * MOD 設定（表示・自動処理の ON/OFF）。
 * ゲームバランス（死亡ペナルティ率・報酬額・ショップ価格）は経済管理ブロックのマスタタブで調整。
 * Dedicated Server ではサーバー側の値が権威となり、ログイン時にクライアントへ同期される。
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_BALANCE_HUD = BUILDER
            .comment(
                    "Show wallet / bank balance HUD on the client.",
                    "On a dedicated server, the server's value is synced to clients and overrides local client config."
            )
            .translation("economy.configuration.enableBalanceHud")
            .define("enableBalanceHud", true);

    public static final ModConfigSpec.BooleanValue ENABLE_ACTION_REWARDS = BUILDER
            .comment(
                    "Grant currency for gameplay actions (mining, harvesting, fishing, kills) and apply death cash penalty.",
                    "Shop / ATM / ETF trades / flea market are not affected.",
                    "On a dedicated server, the server's value is authoritative."
            )
            .translation("economy.configuration.enableActionRewards")
            .define("enableActionRewards", true);

    public static final ModConfigSpec.BooleanValue ENABLE_ETF_UPDATES = BUILDER
            .comment(
                    "Run ETF random-walk price updates and apply shop-trade price influence.",
                    "Players can still open the stock UI and trade at the current price when this is false.",
                    "On a dedicated server, the server's value is authoritative."
            )
            .translation("economy.configuration.enableEtfUpdates")
            .define("enableEtfUpdates", true);

    public static final ModConfigSpec.IntValue REWARD_CHAT_AGGREGATE_SECONDS = BUILDER
            .comment(
                    "Reward chat messages are combined over this many seconds (0 = show each reward immediately).",
                    "On a dedicated server, the server's value is authoritative and synced to clients in feature flags.",
                    "Game balance, shop prices, and action reward amounts are edited in the Economy Admin block (Master tab)."
            )
            .translation("economy.configuration.rewardChatAggregateSeconds")
            .defineInRange("rewardChatAggregateSeconds", 2, 0, 30);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static void syncToRuntimeConfig() {
        EconomyRuntimeConfig.enableBalanceHud = ENABLE_BALANCE_HUD.get();
        EconomyRuntimeConfig.enableActionRewards = ENABLE_ACTION_REWARDS.get();
        EconomyRuntimeConfig.enableEtfUpdates = ENABLE_ETF_UPDATES.get();
        EconomyRuntimeConfig.rewardChatAggregateSeconds = REWARD_CHAT_AGGREGATE_SECONDS.get();
    }

    public static void syncFromRuntimeConfig() {
        ENABLE_BALANCE_HUD.set(EconomyRuntimeConfig.enableBalanceHud);
        ENABLE_BALANCE_HUD.save();
        ENABLE_ACTION_REWARDS.set(EconomyRuntimeConfig.enableActionRewards);
        ENABLE_ACTION_REWARDS.save();
        ENABLE_ETF_UPDATES.set(EconomyRuntimeConfig.enableEtfUpdates);
        ENABLE_ETF_UPDATES.save();
        REWARD_CHAT_AGGREGATE_SECONDS.set(EconomyRuntimeConfig.rewardChatAggregateSeconds);
        REWARD_CHAT_AGGREGATE_SECONDS.save();
    }

    private Config() {
    }
}
