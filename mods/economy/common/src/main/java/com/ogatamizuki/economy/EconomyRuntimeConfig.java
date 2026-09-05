package com.ogatamizuki.economy;

/**
 * 実行時の機能フラグ。NeoForge は {@code Config} から、Fabric は設定画面から同期する。
 */
public final class EconomyRuntimeConfig {
    public static boolean enableBalanceHud = true;
    public static boolean enableActionRewards = true;
    public static boolean enableEtfUpdates = true;
    public static int rewardChatAggregateSeconds = 2;

    private EconomyRuntimeConfig() {}
}
