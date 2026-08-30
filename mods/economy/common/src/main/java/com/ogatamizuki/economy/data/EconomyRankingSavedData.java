package com.ogatamizuki.economy.data;

import com.ogatamizuki.economy.EconomyCommon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** ランキングスナップショット（単体版）。 */
public final class EconomyRankingSavedData extends SavedData {
    private static final Gson GSON = new GsonBuilder().create();

    public record RankingRecord(
            String playerUuid,
            String username,
            int playTime,
            int balance,
            int bankBalance,
            int totalMoney,
            int totalEarnings,
            int totalDebt,
            int totalLost,
            double travelDistance,
            int blocksBroken,
            int deaths,
            int playerKills,
            int mobKills,
            int harvests,
            int potionsBrewed,
            int fishCaught,
            int etfBuyAmount,
            int etfShortAmount,
            int etfProfitAmount,
            int totalTradeCount
    ) {
    }

    public record Snapshot(long createdAtEpochSec, List<RankingRecord> records) {
    }

    private static final Codec<EconomyRankingSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("latestJson", "").forGetter(data -> data.latestJson)
    ).apply(instance, json -> {
        EconomyRankingSavedData data = new EconomyRankingSavedData();
        data.latestJson = json;
        return data;
    }));

    private static final SavedDataType<EconomyRankingSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(EconomyCommon.MODID, "ranking_data"),
            EconomyRankingSavedData::new,
            CODEC,
            null
    );

    private String latestJson = "";

    public EconomyRankingSavedData() {
    }

    public static EconomyRankingSavedData get(MinecraftServer server) {
        ServerLevel level = server.overworld();
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public Optional<Snapshot> latest() {
        if (latestJson == null || latestJson.isBlank()) {
            return Optional.empty();
        }
        try {
            RawSnapshot raw = GSON.fromJson(latestJson, RawSnapshot.class);
            if (raw == null || raw.records == null) {
                return Optional.empty();
            }
            List<RankingRecord> records = new ArrayList<>();
            for (RawRecord r : raw.records) {
                records.add(new RankingRecord(
                        r.playerUuid, r.username, r.playTime, r.balance, r.bankBalance, r.totalMoney,
                        r.totalEarnings, r.totalDebt, r.totalLost, r.travelDistance, r.blocksBroken, r.deaths,
                        r.playerKills, r.mobKills, r.harvests, r.potionsBrewed, r.fishCaught,
                        r.etfBuyAmount, r.etfShortAmount, r.etfProfitAmount, r.totalTradeCount
                ));
            }
            return Optional.of(new Snapshot(raw.createdAtEpochSec, records));
        } catch (Exception e) {
            EconomyCommon.LOGGER.error("Failed to parse ranking snapshot", e);
            return Optional.empty();
        }
    }

    public void setLatest(Snapshot snapshot) {
        RawSnapshot raw = new RawSnapshot();
        raw.createdAtEpochSec = snapshot.createdAtEpochSec();
        raw.records = new ArrayList<>();
        for (RankingRecord record : snapshot.records()) {
            RawRecord r = new RawRecord();
            r.playerUuid = record.playerUuid();
            r.username = record.username();
            r.playTime = record.playTime();
            r.balance = record.balance();
            r.bankBalance = record.bankBalance();
            r.totalMoney = record.totalMoney();
            r.totalEarnings = record.totalEarnings();
            r.totalDebt = record.totalDebt();
            r.totalLost = record.totalLost();
            r.travelDistance = record.travelDistance();
            r.blocksBroken = record.blocksBroken();
            r.deaths = record.deaths();
            r.playerKills = record.playerKills();
            r.mobKills = record.mobKills();
            r.harvests = record.harvests();
            r.potionsBrewed = record.potionsBrewed();
            r.fishCaught = record.fishCaught();
            r.etfBuyAmount = record.etfBuyAmount();
            r.etfShortAmount = record.etfShortAmount();
            r.etfProfitAmount = record.etfProfitAmount();
            r.totalTradeCount = record.totalTradeCount();
            raw.records.add(r);
        }
        latestJson = GSON.toJson(raw);
        setDirty();
    }

    public void clear() {
        latestJson = "";
        setDirty();
    }

    private static final class RawSnapshot {
        long createdAtEpochSec;
        List<RawRecord> records;
    }

    private static final class RawRecord {
        String playerUuid;
        String username;
        int playTime;
        int balance;
        int bankBalance;
        int totalMoney;
        int totalEarnings;
        int totalDebt;
        int totalLost;
        double travelDistance;
        int blocksBroken;
        int deaths;
        int playerKills;
        int mobKills;
        int harvests;
        int potionsBrewed;
        int fishCaught;
        int etfBuyAmount;
        int etfShortAmount;
        int etfProfitAmount;
        int totalTradeCount;
    }
}
