package com.ogatamizuki.lookalike;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * クライアント → サーバー: Mods Config 画面の COMMON 設定を Dedicated / 統合サーバーの toml へ反映。
 */
public record LookalikeCommonConfigPushPayload(
        int disguiseDurationSeconds,
        boolean allowDefaultPlayerList,
        boolean hideAllNametags,
        boolean enableMirrorCrafting,
        int defaultCastTimeSeconds,
        String defaultEffectTemplate
) implements CustomPacketPayload {

    public static final Type<LookalikeCommonConfigPushPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LookalikeMod.MODID, "common_config_push"));

    public static final StreamCodec<ByteBuf, LookalikeCommonConfigPushPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LookalikeCommonConfigPushPayload::disguiseDurationSeconds,
            ByteBufCodecs.BOOL, LookalikeCommonConfigPushPayload::allowDefaultPlayerList,
            ByteBufCodecs.BOOL, LookalikeCommonConfigPushPayload::hideAllNametags,
            ByteBufCodecs.BOOL, LookalikeCommonConfigPushPayload::enableMirrorCrafting,
            ByteBufCodecs.VAR_INT, LookalikeCommonConfigPushPayload::defaultCastTimeSeconds,
            ByteBufCodecs.STRING_UTF8, LookalikeCommonConfigPushPayload::defaultEffectTemplate,
            LookalikeCommonConfigPushPayload::new);

    public static LookalikeCommonConfigPushPayload fromLocalConfig() {
        return new LookalikeCommonConfigPushPayload(
                Config.disguiseDurationSeconds.get(),
                Config.allowDefaultPlayerList.get(),
                Config.hideAllNametags.get(),
                Config.enableMirrorCrafting.get(),
                Config.defaultCastTimeSeconds.get(),
                Config.defaultEffectTemplate.get()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
