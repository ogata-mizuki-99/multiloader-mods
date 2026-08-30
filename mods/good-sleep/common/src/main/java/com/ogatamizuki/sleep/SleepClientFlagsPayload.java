package com.ogatamizuki.sleep;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** サーバー → クライアント: COMMON のうちクライアント表示に効くフラグ。 */
public record SleepClientFlagsPayload(boolean healWhileSleeping) implements CustomPacketPayload {

    public static final Type<SleepClientFlagsPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SleepCommon.MODID, "client_flags"));

    public static final StreamCodec<ByteBuf, SleepClientFlagsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SleepClientFlagsPayload::healWhileSleeping,
            SleepClientFlagsPayload::new);

    public static SleepClientFlagsPayload fromConfig() {
        return new SleepClientFlagsPayload(SleepCommon.healWhileSleeping);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
