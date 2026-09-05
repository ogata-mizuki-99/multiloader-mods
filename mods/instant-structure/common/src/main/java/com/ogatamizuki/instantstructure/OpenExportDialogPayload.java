package com.ogatamizuki.instantstructure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenExportDialogPayload(
        int x1, int y1, int z1,
        int x2, int y2, int z2
) implements CustomPacketPayload {

    public static final Type<OpenExportDialogPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(InstantStructureCommon.MODID, "open_export_dialog"));

    public static final StreamCodec<ByteBuf, OpenExportDialogPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, OpenExportDialogPayload::x1,
            ByteBufCodecs.INT, OpenExportDialogPayload::y1,
            ByteBufCodecs.INT, OpenExportDialogPayload::z1,
            ByteBufCodecs.INT, OpenExportDialogPayload::x2,
            ByteBufCodecs.INT, OpenExportDialogPayload::y2,
            ByteBufCodecs.INT, OpenExportDialogPayload::z2,
            OpenExportDialogPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
