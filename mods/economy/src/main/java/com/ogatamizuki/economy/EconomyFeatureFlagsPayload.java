package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** サーバー → クライアント: 機能フラグ同期（Dedicated Server の設定をクライアント表示・挙動に反映）。 */
public record EconomyFeatureFlagsPayload(
        boolean enableBalanceHud,
        boolean enableActionRewards,
        boolean enableEtfUpdates,
        int rewardChatAggregateSeconds
) implements CustomPacketPayload {

    public static final Type<EconomyFeatureFlagsPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyMod.MODID, "feature_flags"));

    public static final StreamCodec<ByteBuf, EconomyFeatureFlagsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, EconomyFeatureFlagsPayload::enableBalanceHud,
            ByteBufCodecs.BOOL, EconomyFeatureFlagsPayload::enableActionRewards,
            ByteBufCodecs.BOOL, EconomyFeatureFlagsPayload::enableEtfUpdates,
            ByteBufCodecs.VAR_INT, EconomyFeatureFlagsPayload::rewardChatAggregateSeconds,
            EconomyFeatureFlagsPayload::new);

    public static EconomyFeatureFlagsPayload fromConfig() {
        return new EconomyFeatureFlagsPayload(
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
