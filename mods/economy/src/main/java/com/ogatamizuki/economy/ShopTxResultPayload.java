package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * サーバー → クライアント: ショップ取引結果通知パケット
 *
 * @param success                 取引が成功したかどうか
 * @param newBalance              成功時の新しい所持金残高
 * @param message                 チャットに表示するメッセージ（成功または失敗）
 * @param itemKey                 取引対象アイテムキー（該当なしは空文字）
 * @param matchPotion             ポーション照合（無しは空文字）
 * @param matchEnchantment        エンチャント照合（無しは空文字）
 * @param matchEnchantmentLevel   エンチャントレベル（無しは 0）
 * @param remainingItemCount      取引後のサーバー側インベントリ残数（該当なしは -1）
 */
public record ShopTxResultPayload(
        boolean success,
        int newBalance,
        String message,
        String itemKey,
        String matchPotion,
        String matchEnchantment,
        int matchEnchantmentLevel,
        int remainingItemCount
) implements CustomPacketPayload {

    public static final Type<ShopTxResultPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyMod.MODID, "shop_tx_result"));
    public static final StreamCodec<ByteBuf, ShopTxResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ShopTxResultPayload::success,
            ByteBufCodecs.INT, ShopTxResultPayload::newBalance,
            ByteBufCodecs.STRING_UTF8, ShopTxResultPayload::message,
            ByteBufCodecs.STRING_UTF8, ShopTxResultPayload::itemKey,
            ByteBufCodecs.STRING_UTF8, ShopTxResultPayload::matchPotion,
            ByteBufCodecs.STRING_UTF8, ShopTxResultPayload::matchEnchantment,
            ByteBufCodecs.INT, ShopTxResultPayload::matchEnchantmentLevel,
            ByteBufCodecs.INT, ShopTxResultPayload::remainingItemCount,
            ShopTxResultPayload::new);

    public static ShopTxResultPayload simple(
            boolean success,
            int newBalance,
            String message,
            String itemKey,
            int remainingItemCount
    ) {
        return new ShopTxResultPayload(success, newBalance, message, nullToEmpty(itemKey), "", "", 0, remainingItemCount);
    }

    public static ShopTxResultPayload withMatch(
            boolean success,
            int newBalance,
            String message,
            String itemKey,
            String matchPotion,
            String matchEnchantment,
            Integer matchEnchantmentLevel,
            int remainingItemCount
    ) {
        return new ShopTxResultPayload(
                success,
                newBalance,
                message,
                nullToEmpty(itemKey),
                nullToEmpty(matchPotion),
                nullToEmpty(matchEnchantment),
                matchEnchantmentLevel != null ? matchEnchantmentLevel : 0,
                remainingItemCount
        );
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
