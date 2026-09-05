package com.ogatamizuki.instantstructure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SelectionSyncPayload(
        boolean hasStart,
        boolean hasBoth,
        boolean confirmed,
        int x1, int y1, int z1,
        int x2, int y2, int z2
) implements CustomPacketPayload {

    public static final Type<SelectionSyncPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(InstantStructureCommon.MODID, "selection_sync"));

    public static final StreamCodec<ByteBuf, SelectionSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SelectionSyncPayload::hasStart,
            ByteBufCodecs.BOOL, SelectionSyncPayload::hasBoth,
            ByteBufCodecs.BOOL, SelectionSyncPayload::confirmed,
            ByteBufCodecs.INT, SelectionSyncPayload::x1,
            ByteBufCodecs.INT, SelectionSyncPayload::y1,
            ByteBufCodecs.INT, SelectionSyncPayload::z1,
            ByteBufCodecs.INT, SelectionSyncPayload::x2,
            ByteBufCodecs.INT, SelectionSyncPayload::y2,
            ByteBufCodecs.INT, SelectionSyncPayload::z2,
            SelectionSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
