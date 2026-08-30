package com.ogatamizuki.instantstructure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestPreviewPayload(
        String category,
        String templateName
) implements CustomPacketPayload {

    public static final Type<RequestPreviewPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(InstantStructureCommon.MODID, "request_preview"));

    public static final StreamCodec<ByteBuf, RequestPreviewPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, RequestPreviewPayload::category,
            ByteBufCodecs.STRING_UTF8, RequestPreviewPayload::templateName,
            RequestPreviewPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
