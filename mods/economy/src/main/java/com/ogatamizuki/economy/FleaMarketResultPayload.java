package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * サーバー → クライアント: フリーマーケット取引結果通知
 */
public record FleaMarketResultPayload(boolean success, String message, int newBalance) implements CustomPacketPayload {

    public static final Type<FleaMarketResultPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyMod.MODID, "flea_market_result"));

    public static final StreamCodec<ByteBuf, FleaMarketResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, FleaMarketResultPayload::success,
            ByteBufCodecs.STRING_UTF8, FleaMarketResultPayload::message,
            ByteBufCodecs.INT, FleaMarketResultPayload::newBalance,
            (success, message, newBalance) -> new FleaMarketResultPayload(success, message, newBalance));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
