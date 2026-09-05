package com.ogatamizuki.radialteleport;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record TeleportDestination(String id, String displayName, byte kind) {
    public static final byte KIND_SPAWN = 0;
    public static final byte KIND_PLAYER = 1;
    public static final byte KIND_WAYPOINT = 2;

    public static final StreamCodec<ByteBuf, TeleportDestination> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TeleportDestination::id,
            ByteBufCodecs.STRING_UTF8, TeleportDestination::displayName,
            ByteBufCodecs.BYTE, TeleportDestination::kind,
            TeleportDestination::new
    );
}
