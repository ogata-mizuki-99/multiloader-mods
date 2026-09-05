package com.ogatamizuki.radialteleport;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TeleportRequestPayload(String destinationId) implements CustomPacketPayload {

    public static final Type<TeleportRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RadialTeleportCommon.MODID, "teleport_request"));

    public static final StreamCodec<ByteBuf, TeleportRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TeleportRequestPayload::destinationId,
            TeleportRequestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
