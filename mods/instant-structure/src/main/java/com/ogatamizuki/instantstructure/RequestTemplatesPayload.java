package com.ogatamizuki.instantstructure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestTemplatesPayload() implements CustomPacketPayload {
    public static final Type<RequestTemplatesPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(InstantStructureMod.MODID, "request_templates"));

    public static final StreamCodec<ByteBuf, RequestTemplatesPayload> STREAM_CODEC = StreamCodec.unit(new RequestTemplatesPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
