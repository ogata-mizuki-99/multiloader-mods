package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * クライアント → サーバー: ETF取引リクエストパケット
 */
public record StockTradeRequestPayload(
        String stockCategoryId,
        String tradeType,
        int quantity
) implements CustomPacketPayload {

    public static final Type<StockTradeRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyMod.MODID, "stock_trade_request"));

    public static final StreamCodec<ByteBuf, StockTradeRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, StockTradeRequestPayload::stockCategoryId,
            ByteBufCodecs.STRING_UTF8, StockTradeRequestPayload::tradeType,
            ByteBufCodecs.INT, StockTradeRequestPayload::quantity,
            (stockCategoryId, tradeType, quantity) -> new StockTradeRequestPayload(stockCategoryId, tradeType, quantity));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
