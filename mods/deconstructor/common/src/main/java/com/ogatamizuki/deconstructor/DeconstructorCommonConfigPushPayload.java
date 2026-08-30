package com.ogatamizuki.deconstructor;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * クライアント → サーバー: Mods Config 画面の COMMON 設定を Dedicated / 統合サーバーの toml へ反映。
 */
public record DeconstructorCommonConfigPushPayload(String excludedItems) implements CustomPacketPayload {

    public static final Type<DeconstructorCommonConfigPushPayload> TYPE = new Type<>(
            DeconstructorCommon.id("common_config_push"));

    public static final StreamCodec<ByteBuf, DeconstructorCommonConfigPushPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DeconstructorCommonConfigPushPayload::excludedItems,
            DeconstructorCommonConfigPushPayload::new);

    public static DeconstructorCommonConfigPushPayload fromLocalConfig() {
        String raw = Config.getExcludedItems();
        return new DeconstructorCommonConfigPushPayload(raw == null ? "" : raw);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
