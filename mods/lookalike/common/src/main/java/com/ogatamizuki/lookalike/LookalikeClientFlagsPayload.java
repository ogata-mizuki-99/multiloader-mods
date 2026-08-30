package com.ogatamizuki.lookalike;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** サーバー → クライアント: COMMON のうちクライアント描画に効くフラグの同期。 */
public record LookalikeClientFlagsPayload(boolean hideAllNametags) implements CustomPacketPayload {

    public static final Type<LookalikeClientFlagsPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LookalikeCommon.MODID, "client_flags"));

    public static final StreamCodec<ByteBuf, LookalikeClientFlagsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, LookalikeClientFlagsPayload::hideAllNametags,
            LookalikeClientFlagsPayload::new);

    public static LookalikeClientFlagsPayload fromConfig() {
        return new LookalikeClientFlagsPayload(Config.hideAllNametags.get());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
