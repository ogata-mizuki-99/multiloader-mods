package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** サーバー → クライアント: ショップ詳細 JSON 応答（大きい場合は分割）。 */
public record ShopDetailsResponsePayload(int shopId, int chunkIndex, int totalChunks, String json) implements CustomPacketPayload {

    public static final Type<ShopDetailsResponsePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyCommon.MODID, "shop_details_response"));

    public static final StreamCodec<ByteBuf, ShopDetailsResponsePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ShopDetailsResponsePayload::shopId,
            ByteBufCodecs.VAR_INT, ShopDetailsResponsePayload::chunkIndex,
            ByteBufCodecs.VAR_INT, ShopDetailsResponsePayload::totalChunks,
            ByteBufCodecs.STRING_UTF8, ShopDetailsResponsePayload::json,
            ShopDetailsResponsePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
