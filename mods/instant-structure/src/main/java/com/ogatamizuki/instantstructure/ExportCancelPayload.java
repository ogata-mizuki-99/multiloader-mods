package com.ogatamizuki.instantstructure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ExportCancelPayload(boolean clearCompletely) implements CustomPacketPayload {

    public static final Type<ExportCancelPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(InstantStructureMod.MODID, "export_cancel"));

    public static final StreamCodec<ByteBuf, ExportCancelPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, ExportCancelPayload::clearCompletely,
                    ExportCancelPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
