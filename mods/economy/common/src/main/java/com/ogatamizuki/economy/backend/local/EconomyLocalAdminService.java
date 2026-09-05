package com.ogatamizuki.economy.backend.local;

import com.ogatamizuki.economy.EconomyCommon;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ogatamizuki.economy.EconomyNicknameBridge;
import com.ogatamizuki.economy.EconomyNpcSpawnService;
import com.ogatamizuki.economy.backend.EconomyEtfPriceScheduler;
import com.ogatamizuki.economy.data.EconomyEtfWorldSavedData;
import com.ogatamizuki.economy.data.EconomyFleaMarketSavedData;
import com.ogatamizuki.economy.data.EconomyRankingSavedData;
import com.ogatamizuki.economy.master.EconomyMasterData;
import com.ogatamizuki.economy.data.EconomyWorldSavedData;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 管理ブロック向けのワールドデータ操作。 */
public final class EconomyLocalAdminService {
    public record ResetOptions(
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
            boolean resetFishCaught
    ) {
        public boolean hasAny() {
            return resetBalances || resetRankingMetrics || resetPortfolios || resetShopLimits
                    || resetFleaMarket || resetRankingSnapshots || resetEtfPrices
                    || resetPlayTime || resetTravelDistance || resetBlocksBroken || resetDeaths
                    || resetPlayerKills || resetMobKills || resetHarvests || resetPotionsBrewed
                    || resetFishCaught;
        }

        public static ResetOptions fullReset() {
            return new ResetOptions(true, true, true, true, true, true, true,
                    true, true, true, true, true, true, true, true, true);
        }

        public EconomyLocalPlayerStatsReset.StatsResetOptions statsResetOptions() {
            return new EconomyLocalPlayerStatsReset.StatsResetOptions(
                    resetPlayTime, resetTravelDistance, resetBlocksBroken, resetDeaths,
                    resetPlayerKills, resetMobKills, resetHarvests, resetPotionsBrewed, resetFishCaught
            );
        }
    }

    private EconomyLocalAdminService() {
    }

    public static String fetchPlayerBalances() {
        MinecraftServer server = EconomyLocalPlayerService.server();
        if (server == null) {
            return null;
        }
        EconomyWorldSavedData data = EconomyWorldSavedData.get(server);
        Map<String, String> onlineNames = new HashMap<>();
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            onlineNames.put(online.getUUID().toString(), EconomyNicknameBridge.resolvePlayerName(online));
        }

        List<Map.Entry<UUID, EconomyWorldSavedData.PlayerRecord>> entries = new ArrayList<>(data.allPlayerMap().entrySet());
        entries.sort(Comparator
                .comparing((Map.Entry<UUID, EconomyWorldSavedData.PlayerRecord> e) ->
                        !EconomyCommon.isEconomyReady(e.getKey()))
                .thenComparing(e -> e.getValue().username(), String.CASE_INSENSITIVE_ORDER));

        JsonArray players = new JsonArray();
        for (Map.Entry<UUID, EconomyWorldSavedData.PlayerRecord> entry : entries) {
            UUID uuid = entry.getKey();
            EconomyWorldSavedData.PlayerRecord record = entry.getValue();
            String username = EconomyNicknameBridge.resolveUsernameForRanking(uuid.toString(), onlineNames);
            if ("Unknown".equals(username) && record.username() != null && !record.username().isBlank()) {
                username = record.username();
            }
            if ("Unknown".equals(username)) {
                username = uuid.toString().substring(0, 8);
            }
            JsonObject player = new JsonObject();
            player.addProperty("playerUuid", uuid.toString());
            player.addProperty("username", username);
            player.addProperty("balance", record.balance());
            player.addProperty("bankBalance", record.bankBalance());
            player.addProperty("debt", record.debt());
            player.addProperty("active", EconomyCommon.isEconomyReady(uuid));
            players.add(player);
        }

        JsonObject root = new JsonObject();
        root.add("players", players);
        return root.toString();
    }

    public static JsonObject reset(ResetOptions options) {
        if (!options.hasAny()) {
            return error("At least one reset option must be enabled");
        }
        MinecraftServer server = EconomyLocalPlayerService.server();
        if (server == null) {
            return error("World data unavailable");
        }

        int playersUpdated = 0;
        if (options.resetBalances() || options.resetRankingMetrics()) {
            playersUpdated = EconomyWorldSavedData.get(server).resetPlayers(options.resetBalances(), options.resetRankingMetrics());
        }
        if (options.resetShopLimits()) {
            EconomyWorldSavedData.get(server).clearShopLimits();
        }
        if (options.resetPortfolios()) {
            EconomyEtfWorldSavedData.get(server).clearPortfolios();
        }
        if (options.resetEtfPrices()) {
            EconomyEtfWorldSavedData.get(server).resetPricesToSeed();
        }
        if (options.resetFleaMarket()) {
            EconomyFleaMarketSavedData.get(server).clearAll();
        }
        if (options.resetRankingSnapshots()) {
            EconomyRankingSavedData.get(server).clear();
        }

        int statsFilesUpdated = 0;
        if (options.statsResetOptions().hasAny()) {
            statsFilesUpdated = EconomyLocalPlayerStatsReset.reset(server, options.statsResetOptions());
        }

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("message", "Economy data reset successfully");
        result.addProperty("playersUpdated", playersUpdated);
        result.addProperty("statsFilesUpdated", statsFilesUpdated);
        return result;
    }

    public static JsonObject reloadMaster() {
        MinecraftServer server = EconomyLocalPlayerService.server();
        if (server == null) {
            return error("World data unavailable");
        }
        String source = EconomyMasterData.reload(server);
        EconomyEtfPriceScheduler.stop();
        EconomyEtfPriceScheduler.start(server);
        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("source", source);
        result.addProperty("message", "bundled".equals(source)
                ? "MOD同梱の economy_master.json を再読込しました"
                : "カスタムマスタを再読込しました: " + source);
        return result;
    }

    public static String fetchMasterConfig(net.minecraft.server.MinecraftServer server) {
        if (server == null) {
            return null;
        }
        EconomyMasterData.reload(server);
        EconomyMasterData.MasterConfigValues values = EconomyMasterData.currentConfigValues();
        JsonObject root = new JsonObject();
        root.addProperty("customOverride", EconomyMasterData.hasOverride(server));
        root.addProperty("sourceHint", "編集: config/economy/economy_master.json");
        root.addProperty("deathPenaltyRate", values.deathPenaltyRate());
        root.addProperty("shortSellLimitRate", values.shortSellLimitRate());
        root.addProperty("etfIntervalMinutes", values.etfIntervalMinutes());
        root.addProperty("loanMaxAmount", values.loanMaxAmount());
        root.addProperty("loanAssetMultiplier", values.loanAssetMultiplier());
        return root.toString();
    }

    public static String fetchMasterRewards(net.minecraft.server.MinecraftServer server) {
        if (server == null) {
            return null;
        }
        EconomyMasterData.reload(server);
        JsonObject root = EconomyMasterData.fetchAllActionRewards();
        root.addProperty("sourceHint", server.isSingleplayer() ? "ローカルワールド" : "サーバー");
        return root.toString();
    }

    /** Minecraft UTF-8 文字列上限 (32767) を超えないよう 1 ページあたりの件数を抑える。 */
    private static final int MASTER_ITEMS_PAGE_SIZE = 40;

    public static String fetchMasterItems(net.minecraft.server.MinecraftServer server) {
        return fetchMasterItems(server, 0);
    }

    public static String fetchMasterItems(net.minecraft.server.MinecraftServer server, int page) {
        if (server == null) {
            return null;
        }
        EconomyMasterData.reload(server);
        JsonObject root = EconomyMasterData.fetchItemsPage(page, MASTER_ITEMS_PAGE_SIZE);
        root.addProperty("sourceHint", server.isSingleplayer() ? "ローカルワールド" : "サーバー");
        return root.toString();
    }

    public static String fetchMasterShops(net.minecraft.server.MinecraftServer server) {
        if (server == null) {
            return null;
        }
        EconomyMasterData.reload(server);
        JsonObject root = EconomyMasterData.fetchAllShops();
        root.addProperty("sourceHint", server.isSingleplayer() ? "ローカルワールド" : "サーバー");
        return root.toString();
    }

    public static String fetchMasterShopItems(net.minecraft.server.MinecraftServer server) {
        if (server == null) {
            return null;
        }
        EconomyMasterData.reload(server);
        JsonObject root = EconomyMasterData.fetchAllShopItems();
        root.addProperty("sourceHint", server.isSingleplayer() ? "ローカルワールド" : "サーバー");
        return root.toString();
    }

    public static String fetchMasterEtfItems(net.minecraft.server.MinecraftServer server) {
        if (server == null) {
            return null;
        }
        EconomyMasterData.reload(server);
        JsonObject root = EconomyMasterData.fetchAllEtfItems();
        root.addProperty("sourceHint", server.isSingleplayer() ? "ローカルワールド" : "サーバー");
        return root.toString();
    }

    public static JsonObject giveSpawnEgg(ServerPlayer player, int shopId) {
        try {
            String message = EconomyNpcSpawnService.giveForShop(player, shopId);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", message);
            return result;
        } catch (Exception e) {
            EconomyCommon.LOGGER.error("Failed to give spawn egg", e);
            return error("スポナーエッグの付与に失敗しました: " + e.getMessage());
        }
    }

    public static JsonObject giveAllSpawnEggs(ServerPlayer player) {
        try {
            String message = EconomyNpcSpawnService.giveAllForEnabledShops(player);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", message);
            return result;
        } catch (Exception e) {
            EconomyCommon.LOGGER.error("Failed to give all spawn eggs", e);
            return error("スポナーエッグの一括付与に失敗しました: " + e.getMessage());
        }
    }

    public static JsonObject saveMasterRewardEdits(String jsonBody) {
        MinecraftServer server = EconomyLocalPlayerService.server();
        if (server == null) {
            return error("World data unavailable");
        }
        try {
            EconomyMasterData.applyActionRewardEdits(server, com.google.gson.JsonParser.parseString(jsonBody).getAsJsonArray());
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "報酬設定を保存し、反映しました。");
            return result;
        } catch (Exception e) {
            EconomyCommon.LOGGER.error("Failed to save action reward edits", e);
            return error("報酬設定の保存に失敗しました: " + e.getMessage());
        }
    }

    public static JsonObject saveMasterItemEdits(String jsonBody) {
        MinecraftServer server = EconomyLocalPlayerService.server();
        if (server == null) {
            return error("World data unavailable");
        }
        try {
            EconomyMasterData.applyItemPriceEdits(server, com.google.gson.JsonParser.parseString(jsonBody).getAsJsonArray());
            EconomyEtfPriceScheduler.stop();
            EconomyEtfPriceScheduler.start(server);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "ショップ価格を保存し、反映しました。");
            return result;
        } catch (Exception e) {
            EconomyCommon.LOGGER.error("Failed to save item price edits", e);
            return error("ショップ価格の保存に失敗しました: " + e.getMessage());
        }
    }

    public static JsonObject saveMasterShopItemEdits(String jsonBody) {
        MinecraftServer server = EconomyLocalPlayerService.server();
        if (server == null) {
            return error("World data unavailable");
        }
        try {
            EconomyMasterData.applyShopItemEdits(server, com.google.gson.JsonParser.parseString(jsonBody).getAsJsonArray());
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "ショップ割当を保存し、反映しました。");
            return result;
        } catch (Exception e) {
            EconomyCommon.LOGGER.error("Failed to save shop item edits", e);
            return error("ショップ割当の保存に失敗しました: " + e.getMessage());
        }
    }

    public static JsonObject saveMasterEtfItemEdits(String jsonBody) {
        MinecraftServer server = EconomyLocalPlayerService.server();
        if (server == null) {
            return error("World data unavailable");
        }
        try {
            EconomyMasterData.applyEtfItemEdits(server, com.google.gson.JsonParser.parseString(jsonBody).getAsJsonArray());
            EconomyEtfPriceScheduler.stop();
            EconomyEtfPriceScheduler.start(server);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("message", "ETF構成比を保存し、反映しました。");
            return result;
        } catch (Exception e) {
            EconomyCommon.LOGGER.error("Failed to save etf item edits", e);
            return error("ETF構成比の保存に失敗しました: " + e.getMessage());
        }
    }

    public static JsonObject saveMasterConfig(EconomyMasterData.MasterConfigValues values) {
        MinecraftServer server = EconomyLocalPlayerService.server();
        if (server == null) {
            return error("World data unavailable");
        }
        try {
            String source = EconomyMasterData.saveConfigOverride(server, values);
            EconomyEtfPriceScheduler.stop();
            EconomyEtfPriceScheduler.start(server);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("source", source);
            result.addProperty("message", "設定を保存し、マスタを反映しました。");
            return result;
        } catch (Exception e) {
            EconomyCommon.LOGGER.error("Failed to save economy master config", e);
            return error("マスタ設定の保存に失敗しました: " + e.getMessage());
        }
    }

    public static JsonObject resetMasterConfig() {
        MinecraftServer server = EconomyLocalPlayerService.server();
        if (server == null) {
            return error("World data unavailable");
        }
        try {
            String source = EconomyMasterData.resetConfigOverride(server);
            EconomyEtfPriceScheduler.stop();
            EconomyEtfPriceScheduler.start(server);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("source", source);
            result.addProperty("message", "同梱の初期内容に書き戻し、反映しました。");
            return result;
        } catch (Exception e) {
            EconomyCommon.LOGGER.error("Failed to reset economy master config", e);
            return error("マスタ設定の復帰に失敗しました: " + e.getMessage());
        }
    }

    private static JsonObject error(String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("error", message);
        return obj;
    }
}
