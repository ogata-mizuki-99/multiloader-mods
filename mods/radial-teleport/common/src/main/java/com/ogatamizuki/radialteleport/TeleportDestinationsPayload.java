package com.ogatamizuki.radialteleport;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record TeleportDestinationsPayload(int revision, List<TeleportDestination> destinations)
        implements CustomPacketPayload {

    public static final Type<TeleportDestinationsPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RadialTeleportCommon.MODID, "teleport_destinations"));

    public static final StreamCodec<ByteBuf, TeleportDestinationsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, TeleportDestinationsPayload::revision,
            ByteBufCodecs.collection(ArrayList::new, TeleportDestination.STREAM_CODEC),
            TeleportDestinationsPayload::destinations,
            TeleportDestinationsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
