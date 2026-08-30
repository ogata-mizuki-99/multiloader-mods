package com.ogatamizuki.instantstructure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record TemplatesListPayload(
        List<TemplateInfo> templates
) implements CustomPacketPayload {
    public static final Type<TemplatesListPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(InstantStructureCommon.MODID, "templates_list"));

    public static final StreamCodec<ByteBuf, TemplatesListPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, TemplateInfo.STREAM_CODEC), TemplatesListPayload::templates,
            TemplatesListPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
