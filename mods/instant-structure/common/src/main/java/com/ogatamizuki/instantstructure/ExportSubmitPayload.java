package com.ogatamizuki.instantstructure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ExportSubmitPayload(
        String name,
        String category
) implements CustomPacketPayload {

    public static final Type<ExportSubmitPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(InstantStructureCommon.MODID, "export_submit"));

    public static final StreamCodec<ByteBuf, ExportSubmitPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ExportSubmitPayload::name,
            ByteBufCodecs.STRING_UTF8, ExportSubmitPayload::category,
            ExportSubmitPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
