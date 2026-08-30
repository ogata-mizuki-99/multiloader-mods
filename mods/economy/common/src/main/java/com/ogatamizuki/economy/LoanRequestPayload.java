package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * クライアント → サーバー: 借入・返済リクエストパケット
 */
public record LoanRequestPayload(String action, int amount) implements CustomPacketPayload {

    public static final Type<LoanRequestPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyCommon.MODID, "loan_request"));

    public static final StreamCodec<ByteBuf, LoanRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, LoanRequestPayload::action,
            ByteBufCodecs.INT, LoanRequestPayload::amount,
            (action, amount) -> new LoanRequestPayload(action, amount));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
