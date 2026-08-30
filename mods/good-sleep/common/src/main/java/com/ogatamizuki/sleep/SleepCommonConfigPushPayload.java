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
            Identifier.fromNamespaceAndPath(SleepCommon.MODID, "common_config_push"));

    public static final StreamCodec<ByteBuf, SleepCommonConfigPushPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SleepCommonConfigPushPayload::allowDaySleep,
            ByteBufCodecs.BOOL, SleepCommonConfigPushPayload::healWhileSleeping,
            ByteBufCodecs.VAR_INT, SleepCommonConfigPushPayload::healIntervalTicks,
            ByteBufCodecs.BOOL, SleepCommonConfigPushPayload::onePlayerSkip,
            SleepCommonConfigPushPayload::new);

    public static SleepCommonConfigPushPayload fromLocalConfig() {
        return new SleepCommonConfigPushPayload(
                SleepCommon.allowDaySleep,
                SleepCommon.healWhileSleeping,
                SleepCommon.healIntervalTicks,
                SleepCommon.onePlayerSkip
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
