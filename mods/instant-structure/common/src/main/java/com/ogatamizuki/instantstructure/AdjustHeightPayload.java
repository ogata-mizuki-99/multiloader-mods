package com.ogatamizuki.instantstructure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record AdjustHeightPayload(
        int delta
) implements CustomPacketPayload {

    public static final Type<AdjustHeightPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(InstantStructureCommon.MODID, "adjust_height"));

    public static final StreamCodec<ByteBuf, AdjustHeightPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, AdjustHeightPayload::delta,
            AdjustHeightPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
