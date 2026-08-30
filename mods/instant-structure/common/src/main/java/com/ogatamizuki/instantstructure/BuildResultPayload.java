package com.ogatamizuki.instantstructure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BuildResultPayload(byte result) implements CustomPacketPayload {
    public static final byte SUCCESS = 0;
    public static final byte PLAYER_INSIDE = 1;
    public static final byte FAILED = 2;

    public static final Type<BuildResultPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(InstantStructureCommon.MODID, "build_result"));

    public static final StreamCodec<ByteBuf, BuildResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, BuildResultPayload::result,
            BuildResultPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
