package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * サーバー → クライアント: ETF取引結果通知パケット
 */
public record StockTradeResultPayload(
        boolean success,
        int newBalance,
        int currentPrice,
        int portfolioQuantity,
        String message
) implements CustomPacketPayload {

    public static final Type<StockTradeResultPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyMod.MODID, "stock_trade_result"));

    public static final StreamCodec<ByteBuf, StockTradeResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, StockTradeResultPayload::success,
            ByteBufCodecs.INT, StockTradeResultPayload::newBalance,
            ByteBufCodecs.INT, StockTradeResultPayload::currentPrice,
            ByteBufCodecs.INT, StockTradeResultPayload::portfolioQuantity,
            ByteBufCodecs.STRING_UTF8, StockTradeResultPayload::message,
            (success, newBalance, currentPrice, portfolioQuantity, message) -> new StockTradeResultPayload(success, newBalance, currentPrice, portfolioQuantity, message));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
