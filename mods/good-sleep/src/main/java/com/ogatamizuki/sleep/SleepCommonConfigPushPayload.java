package com.ogatamizuki.sleep;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * クライアント → サーバー: Mods Config 画面の COMMON 設定を Dedicated / 統合サーバーの toml へ反映。
 */
public record SleepCommonConfigPushPayload(
        boolean allowDaySleep,
        boolean healWhileSleeping,
        int healIntervalTicks,
        boolean onePlayerSkip
) implements CustomPacketPayload {

    public static final Type<SleepCommonConfigPushPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SleepMod.MODID, "common_config_push"));

    public static final StreamCodec<ByteBuf, SleepCommonConfigPushPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SleepCommonConfigPushPayload::allowDaySleep,
            ByteBufCodecs.BOOL, SleepCommonConfigPushPayload::healWhileSleeping,
            ByteBufCodecs.VAR_INT, SleepCommonConfigPushPayload::healIntervalTicks,
            ByteBufCodecs.BOOL, SleepCommonConfigPushPayload::onePlayerSkip,
            SleepCommonConfigPushPayload::new);

    public static SleepCommonConfigPushPayload fromLocalConfig() {
        return new SleepCommonConfigPushPayload(
                Config.ALLOW_DAY_SLEEP.get(),
                Config.HEAL_WHILE_SLEEPING.get(),
                Config.HEAL_INTERVAL_TICKS.get(),
                Config.ONE_PLAYER_SKIP.get()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
