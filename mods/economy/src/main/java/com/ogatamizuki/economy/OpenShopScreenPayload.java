package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * サーバー → クライアント: ショップ画面オープン指示パケット
 */
public record OpenShopScreenPayload(int shopId, String npcType) implements CustomPacketPayload {

    public static final Type<OpenShopScreenPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyMod.MODID, "open_shop_screen"));

    public static final StreamCodec<ByteBuf, OpenShopScreenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, OpenShopScreenPayload::shopId,
            ByteBufCodecs.STRING_UTF8, OpenShopScreenPayload::npcType,
            (id, type) -> new OpenShopScreenPayload(id, type));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
