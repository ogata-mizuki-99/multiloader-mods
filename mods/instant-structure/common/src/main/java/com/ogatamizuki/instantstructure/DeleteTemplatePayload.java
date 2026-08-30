package com.ogatamizuki.instantstructure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DeleteTemplatePayload(
        String category,
        String templateName
) implements CustomPacketPayload {

    public static final Type<DeleteTemplatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(InstantStructureCommon.MODID, "delete_template"));

    public static final StreamCodec<ByteBuf, DeleteTemplatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DeleteTemplatePayload::category,
            ByteBufCodecs.STRING_UTF8, DeleteTemplatePayload::templateName,
            DeleteTemplatePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
