package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * クライアント → サーバー: フリーマーケット購入リクエスト
 */
public record FleaMarketBuyRequestPayload(String listingId, int quantity) implements CustomPacketPayload {

    public static final Type<FleaMarketBuyRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyCommon.MODID, "flea_market_buy_request"));

    public static final StreamCodec<ByteBuf, FleaMarketBuyRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FleaMarketBuyRequestPayload::listingId,
            ByteBufCodecs.INT, FleaMarketBuyRequestPayload::quantity,
            (listingId, quantity) -> new FleaMarketBuyRequestPayload(listingId, quantity));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
