package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** クライアント → サーバー: 読み取り専用データクエリ。 */
public record EconomyQueryRequestPayload(String queryType, String arg1, int arg2) implements CustomPacketPayload {

    public static final Type<EconomyQueryRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyCommon.MODID, "economy_query_request"));

    public static final StreamCodec<ByteBuf, EconomyQueryRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EconomyQueryRequestPayload::queryType,
            ByteBufCodecs.STRING_UTF8, EconomyQueryRequestPayload::arg1,
            ByteBufCodecs.INT, EconomyQueryRequestPayload::arg2,
            EconomyQueryRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
