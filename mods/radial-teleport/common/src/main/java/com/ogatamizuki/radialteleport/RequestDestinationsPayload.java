package com.ogatamizuki.radialteleport;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestDestinationsPayload() implements CustomPacketPayload {

    public static final RequestDestinationsPayload INSTANCE = new RequestDestinationsPayload();

    public static final Type<RequestDestinationsPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RadialTeleportCommon.MODID, "request_destinations"));

    public static final StreamCodec<ByteBuf, RequestDestinationsPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
