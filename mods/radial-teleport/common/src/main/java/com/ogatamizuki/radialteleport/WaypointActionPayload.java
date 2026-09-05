package com.ogatamizuki.radialteleport;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record WaypointActionPayload(byte action, String argument) implements CustomPacketPayload {
    public static final byte ACTION_SAVE = 0;
    public static final byte ACTION_DELETE = 1;
    public static final byte ACTION_OPEN_EDIT = 2;
    public static final byte ACTION_RENAME = 3;
    public static final byte ACTION_MOVE = 4;

    public static final String ARG_SEPARATOR = "|";

    public static final Type<WaypointActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RadialTeleportCommon.MODID, "waypoint_action"));

    public static final StreamCodec<ByteBuf, WaypointActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, WaypointActionPayload::action,
            ByteBufCodecs.STRING_UTF8, WaypointActionPayload::argument,
            WaypointActionPayload::new
    );

    public static WaypointActionPayload save(String name) {
        return new WaypointActionPayload(ACTION_SAVE, name);
    }

    public static WaypointActionPayload delete(String destinationId) {
        return new WaypointActionPayload(ACTION_DELETE, destinationId);
    }

    public static WaypointActionPayload openEdit() {
        return new WaypointActionPayload(ACTION_OPEN_EDIT, "");
    }

    public static WaypointActionPayload rename(String destinationId, String newName) {
        return new WaypointActionPayload(ACTION_RENAME, destinationId + ARG_SEPARATOR + newName);
    }

    public static WaypointActionPayload move(String destinationId, boolean up) {
        return new WaypointActionPayload(ACTION_MOVE, destinationId + ARG_SEPARATOR + (up ? "up" : "down"));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
