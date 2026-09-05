package com.ogatamizuki.radialteleport;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record WaypointListPayload(List<WaypointListEntry> waypoints) implements CustomPacketPayload {
    public static final Type<WaypointListPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RadialTeleportCommon.MODID, "waypoint_list"));

    public static final StreamCodec<ByteBuf, WaypointListPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, WaypointListEntry.STREAM_CODEC),
            WaypointListPayload::waypoints,
            WaypointListPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
