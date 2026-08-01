package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * サーバー → クライアント: 借入・返済処理結果通知パケット
 */
public record LoanTxResultPayload(
        boolean success,
        int newBalance,
        int newDebt,
        String message
) implements CustomPacketPayload {

    public static final Type<LoanTxResultPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyMod.MODID, "loan_tx_result"));

    public static final StreamCodec<ByteBuf, LoanTxResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, LoanTxResultPayload::success,
            ByteBufCodecs.INT, LoanTxResultPayload::newBalance,
            ByteBufCodecs.INT, LoanTxResultPayload::newDebt,
            ByteBufCodecs.STRING_UTF8, LoanTxResultPayload::message,
            (success, newBalance, newDebt, message) -> new LoanTxResultPayload(success, newBalance, newDebt, message));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
