package com.ogatamizuki.radialteleport;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** サーバー → クライアント: ウェイポイント UI 用フラグ。 */
public record RadialTeleportClientFlagsPayload(boolean enableWaypoints) implements CustomPacketPayload {

    public static final Type<RadialTeleportClientFlagsPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RadialTeleportMod.MODID, "client_flags"));

    public static final StreamCodec<ByteBuf, RadialTeleportClientFlagsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, RadialTeleportClientFlagsPayload::enableWaypoints,
            RadialTeleportClientFlagsPayload::new);

    public static RadialTeleportClientFlagsPayload fromConfig() {
        return new RadialTeleportClientFlagsPayload(Config.ENABLE_WAYPOINTS.get());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
