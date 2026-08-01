package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * サーバー → クライアント: 所持金・銀行残高・借金の同期（Dedicated Server 向け）
 */
public record PlayerBalanceSyncPayload(
        int balance,
        int bankBalance,
        int debt
) implements CustomPacketPayload {

    public static final Type<PlayerBalanceSyncPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyMod.MODID, "player_balance_sync"));

    public static final StreamCodec<ByteBuf, PlayerBalanceSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, PlayerBalanceSyncPayload::balance,
            ByteBufCodecs.INT, PlayerBalanceSyncPayload::bankBalance,
            ByteBufCodecs.INT, PlayerBalanceSyncPayload::debt,
            (balance, bankBalance, debt) -> new PlayerBalanceSyncPayload(balance, bankBalance, debt));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
