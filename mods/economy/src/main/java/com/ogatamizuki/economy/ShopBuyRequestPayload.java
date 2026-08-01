package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * クライアント → サーバー: アイテム購入リクエストパケット
 */
public record ShopBuyRequestPayload(int shopItemId, int quantity) implements CustomPacketPayload {

    public static final Type<ShopBuyRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyMod.MODID, "shop_buy_request"));

    public static final StreamCodec<ByteBuf, ShopBuyRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ShopBuyRequestPayload::shopItemId,
            ByteBufCodecs.INT, ShopBuyRequestPayload::quantity,
            (id, qty) -> new ShopBuyRequestPayload(id, qty));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
