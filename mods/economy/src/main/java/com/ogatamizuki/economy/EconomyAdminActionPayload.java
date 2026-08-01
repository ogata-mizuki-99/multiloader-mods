package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** クライアント → サーバー: 管理ブロック操作。 */
public record EconomyAdminActionPayload(
        String action,
        boolean resetBalances,
        boolean resetRankingMetrics,
        boolean resetPortfolios,
        boolean resetShopLimits,
        boolean resetFleaMarket,
        boolean resetRankingSnapshots,
        boolean resetEtfPrices,
        boolean resetPlayTime,
        boolean resetTravelDistance,
        boolean resetBlocksBroken,
        boolean resetDeaths,
        boolean resetPlayerKills,
        boolean resetMobKills,
        boolean resetHarvests,
        boolean resetPotionsBrewed,
        boolean resetFishCaught,
        int shopId
) implements CustomPacketPayload {

    private static final int FLAG_BALANCES = 1 << 0;
    private static final int FLAG_RANKING_METRICS = 1 << 1;
    private static final int FLAG_PORTFOLIOS = 1 << 2;
    private static final int FLAG_SHOP_LIMITS = 1 << 3;
    private static final int FLAG_FLEA_MARKET = 1 << 4;
    private static final int FLAG_RANKING_SNAPSHOTS = 1 << 5;
    private static final int FLAG_ETF_PRICES = 1 << 6;
    private static final int FLAG_PLAY_TIME = 1 << 7;
    private static final int FLAG_TRAVEL_DISTANCE = 1 << 8;
    private static final int FLAG_BLOCKS_BROKEN = 1 << 9;
    private static final int FLAG_DEATHS = 1 << 10;
    private static final int FLAG_PLAYER_KILLS = 1 << 11;
    private static final int FLAG_MOB_KILLS = 1 << 12;
    private static final int FLAG_HARVESTS = 1 << 13;
    private static final int FLAG_POTIONS_BREWED = 1 << 14;
    private static final int FLAG_FISH_CAUGHT = 1 << 15;

    public static final Type<EconomyAdminActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyMod.MODID, "admin_action"));

    public static final StreamCodec<ByteBuf, EconomyAdminActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EconomyAdminActionPayload::action,
            ByteBufCodecs.INT, EconomyAdminActionPayload::encodeResetFlags,
            ByteBufCodecs.INT, EconomyAdminActionPayload::shopId,
            EconomyAdminActionPayload::decodePayload);

    public static EconomyAdminActionPayload forAction(String action, int shopId) {
        return decodePayload(action, 0, shopId);
    }

    static EconomyAdminActionPayload decodePayload(String action, int flags, int shopId) {
        return new EconomyAdminActionPayload(
                action,
                (flags & FLAG_BALANCES) != 0,
                (flags & FLAG_RANKING_METRICS) != 0,
                (flags & FLAG_PORTFOLIOS) != 0,
                (flags & FLAG_SHOP_LIMITS) != 0,
                (flags & FLAG_FLEA_MARKET) != 0,
                (flags & FLAG_RANKING_SNAPSHOTS) != 0,
                (flags & FLAG_ETF_PRICES) != 0,
                (flags & FLAG_PLAY_TIME) != 0,
                (flags & FLAG_TRAVEL_DISTANCE) != 0,
                (flags & FLAG_BLOCKS_BROKEN) != 0,
                (flags & FLAG_DEATHS) != 0,
                (flags & FLAG_PLAYER_KILLS) != 0,
                (flags & FLAG_MOB_KILLS) != 0,
                (flags & FLAG_HARVESTS) != 0,
                (flags & FLAG_POTIONS_BREWED) != 0,
                (flags & FLAG_FISH_CAUGHT) != 0,
                shopId
        );
    }

    int encodeResetFlags() {
        int flags = 0;
        if (resetBalances) {
            flags |= FLAG_BALANCES;
        }
        if (resetRankingMetrics) {
            flags |= FLAG_RANKING_METRICS;
        }
        if (resetPortfolios) {
            flags |= FLAG_PORTFOLIOS;
        }
        if (resetShopLimits) {
            flags |= FLAG_SHOP_LIMITS;
        }
        if (resetFleaMarket) {
            flags |= FLAG_FLEA_MARKET;
        }
        if (resetRankingSnapshots) {
            flags |= FLAG_RANKING_SNAPSHOTS;
        }
        if (resetEtfPrices) {
            flags |= FLAG_ETF_PRICES;
        }
        if (resetPlayTime) {
            flags |= FLAG_PLAY_TIME;
        }
        if (resetTravelDistance) {
            flags |= FLAG_TRAVEL_DISTANCE;
        }
        if (resetBlocksBroken) {
            flags |= FLAG_BLOCKS_BROKEN;
        }
        if (resetDeaths) {
            flags |= FLAG_DEATHS;
        }
        if (resetPlayerKills) {
            flags |= FLAG_PLAYER_KILLS;
        }
        if (resetMobKills) {
            flags |= FLAG_MOB_KILLS;
        }
        if (resetHarvests) {
            flags |= FLAG_HARVESTS;
        }
        if (resetPotionsBrewed) {
            flags |= FLAG_POTIONS_BREWED;
        }
        if (resetFishCaught) {
            flags |= FLAG_FISH_CAUGHT;
        }
        return flags;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
