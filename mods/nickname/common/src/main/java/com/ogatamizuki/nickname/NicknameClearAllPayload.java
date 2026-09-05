package com.ogatamizuki.nickname;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record NicknameClearAllPayload() implements CustomPacketPayload {

    public static final Type<NicknameClearAllPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(NicknameCommon.MODID, "nickname_clear_all"));

    public static final StreamCodec<ByteBuf, NicknameClearAllPayload> STREAM_CODEC = StreamCodec.unit(new NicknameClearAllPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
