package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** サーバー → クライアント: ATM 入出金結果。 */
public record BankResultPayload(boolean success, int balance, int bankBalance, int debt) implements CustomPacketPayload {

    public static final Type<BankResultPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyCommon.MODID, "bank_result"));

    public static final StreamCodec<ByteBuf, BankResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, BankResultPayload::success,
            ByteBufCodecs.INT, BankResultPayload::balance,
            ByteBufCodecs.INT, BankResultPayload::bankBalance,
            ByteBufCodecs.INT, BankResultPayload::debt,
            BankResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
