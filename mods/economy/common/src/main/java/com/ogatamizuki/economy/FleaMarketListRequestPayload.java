package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * クライアント → サーバー: フリーマーケット出品リクエスト。
 * ItemStack.STREAM_CODEC はカスタムペイロード上で DataComponent レジストリ ID が
 * ずれて DecoderException（例: No value with id 114）になりうるため、
 * itemKey + SNBT 文字列で送る。
 */
public record FleaMarketListRequestPayload(
        String itemKey,
        String itemName,
        int price,
        int quantity,
        String itemStackSnbt
) implements CustomPacketPayload {

    public static final Type<FleaMarketListRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyCommon.MODID, "flea_market_list_request"));

    public static final StreamCodec<ByteBuf, FleaMarketListRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, FleaMarketListRequestPayload::itemKey,
                    ByteBufCodecs.STRING_UTF8, FleaMarketListRequestPayload::itemName,
                    ByteBufCodecs.INT, FleaMarketListRequestPayload::price,
                    ByteBufCodecs.INT, FleaMarketListRequestPayload::quantity,
                    ByteBufCodecs.STRING_UTF8, FleaMarketListRequestPayload::itemStackSnbt,
                    FleaMarketListRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
