package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** クライアント → サーバー: ATM 入出金リクエスト。 */
public record BankRequestPayload(String action, int amount) implements CustomPacketPayload {

    public static final Type<BankRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyCommon.MODID, "bank_request"));

    public static final StreamCodec<ByteBuf, BankRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BankRequestPayload::action,
            ByteBufCodecs.INT, BankRequestPayload::amount,
            BankRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
