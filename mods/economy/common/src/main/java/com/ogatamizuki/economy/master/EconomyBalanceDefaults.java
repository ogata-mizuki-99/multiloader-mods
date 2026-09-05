package com.ogatamizuki.economy.master;

/** economy_master.json の config セクションにエントリがない場合の既定値。 */
public final class EconomyBalanceDefaults {
    public static final double DEATH_PENALTY_RATE = 0.1;
    public static final double SHORT_SELL_LIMIT_RATE = 0.5;
    public static final int ETF_RANDOM_WALK_INTERVAL_MINUTES = 10;
    public static final int LOAN_MAX_AMOUNT = 1_000_000;
    public static final double LOAN_ASSET_MULTIPLIER = 2.0;

    private EconomyBalanceDefaults() {
    }
}
