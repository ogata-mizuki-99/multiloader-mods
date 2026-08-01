package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** クライアント → サーバー: ショップ詳細取得リクエスト。 */
public record ShopDetailsRequestPayload(int shopId) implements CustomPacketPayload {

    public static final Type<ShopDetailsRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyMod.MODID, "shop_details_request"));

    public static final StreamCodec<ByteBuf, ShopDetailsRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ShopDetailsRequestPayload::shopId,
            ShopDetailsRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
