package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * クライアント → サーバー: Mods Config 画面の COMMON 設定を Dedicated / 統合サーバーの toml へ反映。
 */
public record EconomyCommonConfigPushPayload(
        boolean enableBalanceHud,
        boolean enableActionRewards,
        boolean enableEtfUpdates,
        int rewardChatAggregateSeconds
) implements CustomPacketPayload {

    public static final Type<EconomyCommonConfigPushPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyMod.MODID, "common_config_push"));

    public static final StreamCodec<ByteBuf, EconomyCommonConfigPushPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, EconomyCommonConfigPushPayload::enableBalanceHud,
            ByteBufCodecs.BOOL, EconomyCommonConfigPushPayload::enableActionRewards,
            ByteBufCodecs.BOOL, EconomyCommonConfigPushPayload::enableEtfUpdates,
            ByteBufCodecs.VAR_INT, EconomyCommonConfigPushPayload::rewardChatAggregateSeconds,
            EconomyCommonConfigPushPayload::new);

    public static EconomyCommonConfigPushPayload fromLocalConfig() {
        return new EconomyCommonConfigPushPayload(
                Config.ENABLE_BALANCE_HUD.get(),
                Config.ENABLE_ACTION_REWARDS.get(),
                Config.ENABLE_ETF_UPDATES.get(),
                Config.REWARD_CHAT_AGGREGATE_SECONDS.get()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
