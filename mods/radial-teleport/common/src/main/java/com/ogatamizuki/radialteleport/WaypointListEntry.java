package com.ogatamizuki.radialteleport;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record WaypointListEntry(
        String destinationId,
        String name,
        String dimensionId,
        int x,
        int y,
        int z
) {
    public static final StreamCodec<ByteBuf, WaypointListEntry> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, WaypointListEntry::destinationId,
            ByteBufCodecs.STRING_UTF8, WaypointListEntry::name,
            ByteBufCodecs.STRING_UTF8, WaypointListEntry::dimensionId,
            ByteBufCodecs.INT, WaypointListEntry::x,
            ByteBufCodecs.INT, WaypointListEntry::y,
            ByteBufCodecs.INT, WaypointListEntry::z,
            WaypointListEntry::new
    );

    public static WaypointListEntry from(PlayerWaypoint waypoint) {
        return new WaypointListEntry(
                waypoint.destinationId(),
                waypoint.name(),
                waypoint.dimension().identifier().toString(),
                (int) waypoint.x(),
                (int) waypoint.y(),
                (int) waypoint.z()
        );
    }
}
