package com.ogatamizuki.instantstructure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PreviewBlockEntry(int x, int y, int z, String blockState) {
    public static final StreamCodec<ByteBuf, PreviewBlockEntry> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PreviewBlockEntry::x,
            ByteBufCodecs.VAR_INT, PreviewBlockEntry::y,
            ByteBufCodecs.VAR_INT, PreviewBlockEntry::z,
            ByteBufCodecs.STRING_UTF8, PreviewBlockEntry::blockState,
            PreviewBlockEntry::new
    );
}
