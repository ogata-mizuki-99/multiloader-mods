package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * クライアント → サーバー: フリーマーケット出品キャンセル（回収）リクエスト
 */
public record FleaMarketCancelRequestPayload(String listingId) implements CustomPacketPayload {

    public static final Type<FleaMarketCancelRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyMod.MODID, "flea_market_cancel_request"));

    public static final StreamCodec<ByteBuf, FleaMarketCancelRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FleaMarketCancelRequestPayload::listingId,
            (listingId) -> new FleaMarketCancelRequestPayload(listingId));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
