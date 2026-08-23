package com.ogatamizuki.instantstructure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record TemplatePreviewPayload(
        String category,
        String templateName,
        List<PreviewBlockEntry> blocks
) implements CustomPacketPayload {

    public static final Type<TemplatePreviewPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(InstantStructureMod.MODID, "template_preview"));

    public static final StreamCodec<ByteBuf, TemplatePreviewPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TemplatePreviewPayload::category,
            ByteBufCodecs.STRING_UTF8, TemplatePreviewPayload::templateName,
            PreviewBlockEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), TemplatePreviewPayload::blocks,
            TemplatePreviewPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
