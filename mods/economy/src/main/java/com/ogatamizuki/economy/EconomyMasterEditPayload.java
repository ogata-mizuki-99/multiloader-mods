package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** クライアント → サーバー: マスタの報酬・ショップ価格の一括保存。 */
public record EconomyMasterEditPayload(String action, String jsonBody) implements CustomPacketPayload {

    public static final Type<EconomyMasterEditPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyMod.MODID, "master_edit"));

    public static final StreamCodec<ByteBuf, EconomyMasterEditPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EconomyMasterEditPayload::action,
            ByteBufCodecs.STRING_UTF8, EconomyMasterEditPayload::jsonBody,
            EconomyMasterEditPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
