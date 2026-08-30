package com.ogatamizuki.economy.backend.local;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ogatamizuki.economy.data.EconomyRankingSavedData;
import com.ogatamizuki.economy.data.EconomyWorldSavedData;

import net.minecraft.server.MinecraftServer;

/** ランキング集計のローカル処理。 */
public final class EconomyLocalRankingService {
    private EconomyLocalRankingService() {
    }

    public static String syncRanking(String jsonPayload) {
        MinecraftServer server = EconomyLocalPlayerService.server();
        if (server == null) {
            return null;
        }
        JsonObject payload = JsonParser.parseString(jsonPayload).getAsJsonObject();
        JsonArray players = payload.getAsJsonArray("players");
        if (players == null) {
            return null;
        }

        EconomyWorldSavedData worldData = EconomyWorldSavedData.get(server);
        List<EconomyRankingSavedData.RankingRecord> records = new ArrayList<>();

        for (var element : players) {
            JsonObject p = element.getAsJsonObject();
            String uuidStr = p.get("playerUuid").getAsString();
            UUID playerUuid = UUID.fromString(uuidStr);
            String username = p.get("username").getAsString();
            EconomyWorldSavedData.PlayerRecord dbPlayer = worldData.getOrCreate(playerUuid, username);

            int playTime = p.has("playTime") ? p.get("playTime").getAsInt() : 0;
            double travelDistance = p.has("travelDistance") ? p.get("travelDistance").getAsDouble() : 0;
            int blocksBroken = p.has("blocksBroken") ? p.get("blocksBroken").getAsInt() : 0;
            int deaths = p.has("deaths") ? p.get("deaths").getAsInt() : 0;
            int playerKills = p.has("playerKills") ? p.get("playerKills").getAsInt() : 0;
            int mobKills = p.has("mobKills") ? p.get("mobKills").getAsInt() : 0;
            int harvests = p.has("harvests") ? p.get("harvests").getAsInt() : 0;
            int potionsBrewed = p.has("potionsBrewed") ? p.get("potionsBrewed").getAsInt() : 0;
            int fishCaught = p.has("fishCaught") ? p.get("fishCaught").getAsInt() : 0;

            int shortLiability = EconomyLocalEtfService.calculateShortExposure(server, playerUuid);
            int totalDebt = dbPlayer.debt() + shortLiability;
            int totalMoney = dbPlayer.balance() + dbPlayer.bankBalance() - totalDebt;
            records.add(new EconomyRankingSavedData.RankingRecord(
                    uuidStr,
                    username,
                    playTime,
                    dbPlayer.balance(),
                    dbPlayer.bankBalance(),
                    totalMoney,
                    dbPlayer.totalEarnings(),
                    totalDebt,
                    dbPlayer.totalLost(),
                    travelDistance,
                    blocksBroken,
                    deaths,
                    playerKills,
                    mobKills,
                    harvests,
                    potionsBrewed,
                    fishCaught,
                    dbPlayer.etfBuyAmount(),
                    dbPlayer.etfShortAmount(),
                    dbPlayer.etfProfitAmount(),
                    dbPlayer.totalTradeCount()
            ));
        }

        if (records.isEmpty()) {
            return null;
        }

        long now = System.currentTimeMillis() / 1000L;
        EconomyRankingSavedData.Snapshot snapshot = new EconomyRankingSavedData.Snapshot(now, records);
        EconomyRankingSavedData.get(server).setLatest(snapshot);

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("message", "Ranking compiled successfully");
        result.addProperty("snapshotId", String.valueOf(now));
        return result.toString();
    }

    public static String fetchLatest() {
        MinecraftServer server = EconomyLocalPlayerService.server();
        if (server == null) {
            return null;
        }
        var snapshotOpt = EconomyRankingSavedData.get(server).latest();
        if (snapshotOpt.isEmpty()) {
            return null;
        }
        EconomyRankingSavedData.Snapshot snapshot = snapshotOpt.get();

        JsonObject root = new JsonObject();
        root.addProperty("id", String.valueOf(snapshot.createdAtEpochSec()));
        root.addProperty("createdAt", snapshot.createdAtEpochSec());
        JsonArray records = new JsonArray();
        for (EconomyRankingSavedData.RankingRecord record : snapshot.records()) {
            records.add(toJson(record));
        }
        root.add("records", records);
        return root.toString();
    }

    private static JsonObject toJson(EconomyRankingSavedData.RankingRecord record) {
        JsonObject obj = new JsonObject();
        obj.addProperty("playerUuid", record.playerUuid());
        obj.addProperty("username", record.username());
        obj.addProperty("playTime", record.playTime());
        obj.addProperty("balance", record.balance());
        obj.addProperty("bankBalance", record.bankBalance());
        obj.addProperty("totalMoney", record.totalMoney());
        obj.addProperty("totalEarnings", record.totalEarnings());
        obj.addProperty("totalDebt", record.totalDebt());
        obj.addProperty("totalLost", record.totalLost());
        obj.addProperty("travelDistance", record.travelDistance());
        obj.addProperty("blocksBroken", record.blocksBroken());
        obj.addProperty("deaths", record.deaths());
        obj.addProperty("playerKills", record.playerKills());
        obj.addProperty("mobKills", record.mobKills());
        obj.addProperty("harvests", record.harvests());
        obj.addProperty("potionsBrewed", record.potionsBrewed());
        obj.addProperty("fishCaught", record.fishCaught());
        obj.addProperty("etfBuyAmount", record.etfBuyAmount());
        obj.addProperty("etfShortAmount", record.etfShortAmount());
        obj.addProperty("etfProfitAmount", record.etfProfitAmount());
        obj.addProperty("totalTradeCount", record.totalTradeCount());
        return obj;
    }
}
