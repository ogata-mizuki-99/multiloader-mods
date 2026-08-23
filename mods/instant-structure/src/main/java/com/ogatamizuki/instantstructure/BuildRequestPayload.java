package com.ogatamizuki.instantstructure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BuildRequestPayload(
        String category,
        String templateName,
        int x, int y, int z,
        int rotation,
        boolean mirrorLeftRight,
        boolean mirrorFrontBack,
        boolean hasAnchor,
        int anchorX, int anchorY, int anchorZ
) implements CustomPacketPayload {

    public static final Type<BuildRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(InstantStructureMod.MODID, "build_request"));

    public static final StreamCodec<ByteBuf, BuildRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BuildRequestPayload::category,
            ByteBufCodecs.STRING_UTF8, BuildRequestPayload::templateName,
            ByteBufCodecs.INT, BuildRequestPayload::x,
            ByteBufCodecs.INT, BuildRequestPayload::y,
            ByteBufCodecs.INT, BuildRequestPayload::z,
            ByteBufCodecs.INT, BuildRequestPayload::rotation,
            ByteBufCodecs.BOOL, BuildRequestPayload::mirrorLeftRight,
            ByteBufCodecs.BOOL, BuildRequestPayload::mirrorFrontBack,
            ByteBufCodecs.BOOL, BuildRequestPayload::hasAnchor,
            ByteBufCodecs.INT, BuildRequestPayload::anchorX,
            ByteBufCodecs.INT, BuildRequestPayload::anchorY,
            ByteBufCodecs.INT, BuildRequestPayload::anchorZ,
            BuildRequestPayload::new
    );

    public PlacementTransform placementTransform() {
        return new PlacementTransform(rotation, mirrorLeftRight, mirrorFrontBack);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
