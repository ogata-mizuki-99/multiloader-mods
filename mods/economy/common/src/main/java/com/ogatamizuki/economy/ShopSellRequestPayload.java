package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * クライアント → サーバー: アイテム売却リクエストパケット（マスタ itemId で特定）
 */
public record ShopSellRequestPayload(int itemId, int quantity) implements CustomPacketPayload {

    public static final Type<ShopSellRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyCommon.MODID, "shop_sell_request"));

    public static final StreamCodec<ByteBuf, ShopSellRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ShopSellRequestPayload::itemId,
            ByteBufCodecs.INT, ShopSellRequestPayload::quantity,
            (itemId, qty) -> new ShopSellRequestPayload(itemId, qty));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
