package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** サーバー → クライアント: 読み取り専用データクエリ応答。 */
public record EconomyQueryResponsePayload(String queryType, String arg1, int arg2, String json) implements CustomPacketPayload {

    public static final Type<EconomyQueryResponsePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyMod.MODID, "economy_query_response"));

    public static final StreamCodec<ByteBuf, EconomyQueryResponsePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EconomyQueryResponsePayload::queryType,
            ByteBufCodecs.STRING_UTF8, EconomyQueryResponsePayload::arg1,
            ByteBufCodecs.INT, EconomyQueryResponsePayload::arg2,
            ByteBufCodecs.STRING_UTF8, EconomyQueryResponsePayload::json,
            EconomyQueryResponsePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
