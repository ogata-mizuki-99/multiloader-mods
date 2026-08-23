package com.ogatamizuki.instantstructure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record TemplateInfo(
        String category,
        String name,
        String description,
        int sizeX,
        int sizeY,
        int sizeZ
) {
    public static final StreamCodec<ByteBuf, TemplateInfo> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TemplateInfo::category,
            ByteBufCodecs.STRING_UTF8, TemplateInfo::name,
            ByteBufCodecs.STRING_UTF8, TemplateInfo::description,
            ByteBufCodecs.INT, TemplateInfo::sizeX,
            ByteBufCodecs.INT, TemplateInfo::sizeY,
            ByteBufCodecs.INT, TemplateInfo::sizeZ,
            TemplateInfo::new
    );
}
