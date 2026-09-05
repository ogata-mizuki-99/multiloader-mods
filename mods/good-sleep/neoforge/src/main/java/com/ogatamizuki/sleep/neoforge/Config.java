package com.ogatamizuki.sleep.neoforge;

import com.ogatamizuki.sleep.SleepCommon;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ALLOW_DAY_SLEEP = BUILDER
            .comment("昼間・朝でもベッドで眠れる。明るい時間に眠ると夜へ、夜に眠ると朝へ時間が進む")
            .define("allowDaySleep", true);

    public static final ModConfigSpec.BooleanValue HEAL_WHILE_SLEEPING = BUILDER
            .comment("ベッドで眠っている間、体力を回復する")
            .define("healWhileSleeping", true);

    public static final ModConfigSpec.IntValue HEAL_INTERVAL_TICKS = BUILDER
            .comment("睡眠中に体力を回復する間隔（tick）。20 tick = 1 秒。0 のときベッドに横になった瞬間に全回復")
            .defineInRange("healIntervalTicks", 40, 0, 200);

    public static final ModConfigSpec.BooleanValue ONE_PLAYER_SKIP = BUILDER
            .comment("マルチプレイで1人でも寝れば夜をスキップできる（playersSleepingPercentage を 0 に設定）")
            .define("onePlayerSkip", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static void syncToCommon() {
        SleepCommon.allowDaySleep = ALLOW_DAY_SLEEP.get();
        SleepCommon.healWhileSleeping = HEAL_WHILE_SLEEPING.get();
        SleepCommon.healIntervalTicks = HEAL_INTERVAL_TICKS.get();
        SleepCommon.onePlayerSkip = ONE_PLAYER_SKIP.get();
    }
}
