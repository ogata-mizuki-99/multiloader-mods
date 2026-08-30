package com.ogatamizuki.nickname;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;

public record NicknameSyncPayload(
        UUID playerUuid,
        String nickname
) implements CustomPacketPayload {

    public static final Type<NicknameSyncPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(NicknameCommon.MODID, "nickname_sync"));

    public static final StreamCodec<ByteBuf, NicknameSyncPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtilStreamCodec(), NicknameSyncPayload::playerUuid,
            ByteBufCodecs.STRING_UTF8, NicknameSyncPayload::nickname,
            NicknameSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // UUID用のシンプルなコーデック
    private static StreamCodec<ByteBuf, UUID> UUIDUtilStreamCodec() {
        return new StreamCodec<>() {
            @Override
            public UUID decode(ByteBuf buffer) {
                long most = buffer.readLong();
                long least = buffer.readLong();
                return new UUID(most, least);
            }

            @Override
            public void encode(ByteBuf buffer, UUID value) {
                buffer.writeLong(value.getMostSignificantBits());
                buffer.writeLong(value.getLeastSignificantBits());
            }
        };
    }
}
