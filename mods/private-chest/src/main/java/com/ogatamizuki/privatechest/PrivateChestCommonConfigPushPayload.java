package com.ogatamizuki.privatechest;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * クライアント → サーバー: Mods Config 画面の COMMON 設定を Dedicated / 統合サーバーの toml へ反映。
 */
public record PrivateChestCommonConfigPushPayload(boolean enableLockerCrafting) implements CustomPacketPayload {

    public static final Type<PrivateChestCommonConfigPushPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PrivateChestMod.MODID, "common_config_push"));

    public static final StreamCodec<ByteBuf, PrivateChestCommonConfigPushPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PrivateChestCommonConfigPushPayload::enableLockerCrafting,
            PrivateChestCommonConfigPushPayload::new);

    public static PrivateChestCommonConfigPushPayload fromLocalConfig() {
        return new PrivateChestCommonConfigPushPayload(Config.enableLockerCrafting.get());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
