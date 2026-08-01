package com.ogatamizuki.economy;

import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ogatamizuki.economy.backend.EconomyBackends;
import com.ogatamizuki.economy.backend.EconomyBalanceSync;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Economy 機能の公開 API。実装は {@link com.ogatamizuki.economy.backend.EconomyBackend} に委譲する。
 * 既存コードとの互換のため static メソッドを維持する。
 */
public final class EconomyService {
    private EconomyService() {
    }

    public static void syncBalanceToClient(ServerPlayer player, int balance, int bankBalance, int debt) {
        EconomyBalanceSync.syncBalanceToClient(player, balance, bankBalance, debt);
    }

    public static void applyBalanceAndSync(Player player, int balance, int bankBalance, int debt) {
        EconomyBalanceSync.applyBalanceAndSync(player, balance, bankBalance, debt);
    }

    public static CompletableFuture<Void> joinPlayer(String uuid, String username) {
        return joinPlayer(uuid, username, null);
    }

    public static CompletableFuture<Void> joinPlayer(String uuid, String username, ServerPlayer syncTarget) {
        return EconomyBackends.get().joinPlayer(uuid, username, syncTarget);
    }

    public static CompletableFuture<Void> leavePlayer(String uuid, String username) {
        return EconomyBackends.get().leavePlayer(uuid, username);
    }

    public static CompletableFuture<Void> depositBank(String uuid, int amount) {
        if (isRemoteGameClient()) {
            return com.ogatamizuki.economy.ClientAccess.requestDepositBank(amount);
        }
        return EconomyBackends.get().depositBank(uuid, amount);
    }

    public static CompletableFuture<Void> withdrawBank(String uuid, int amount) {
        if (isRemoteGameClient()) {
            return com.ogatamizuki.economy.ClientAccess.requestWithdrawBank(amount);
        }
        return EconomyBackends.get().withdrawBank(uuid, amount);
    }

    public static CompletableFuture<Void> rewardPlayer(Player player, String actionType) {
        return rewardPlayer(player, actionType, 1.0);
    }

    public static CompletableFuture<Void> rewardPlayer(Player player, String actionType, double ratio) {
        if (!EconomyFeatures.isActionRewardsEnabled()) {
            return CompletableFuture.completedFuture(null);
        }
        return EconomyBackends.get().rewardPlayer(player, actionType, ratio);
    }

    public static CompletableFuture<Void> deathPlayer(Player player) {
        if (!EconomyFeatures.isActionRewardsEnabled()) {
            return CompletableFuture.completedFuture(null);
        }
        return EconomyBackends.get().deathPlayer(player);
    }

    public static CompletableFuture<String> fetchShopDetails(int shopId, String playerUuid) {
        if (isRemoteGameClient()) {
            return com.ogatamizuki.economy.ClientAccess.requestShopDetails(shopId);
        }
        return EconomyBackends.get().fetchShopDetails(shopId, playerUuid);
    }

    public static CompletableFuture<JsonObject> buyShopItem(String playerUuid, int shopItemId, int quantity) {
        return EconomyBackends.get().buyShopItem(playerUuid, shopItemId, quantity);
    }

    public static CompletableFuture<JsonObject> sellShopItem(String playerUuid, int itemId, int quantity) {
        return EconomyBackends.get().sellShopItem(playerUuid, itemId, quantity);
    }

    public static CompletableFuture<String> fetchStocks(String playerUuid) {
        if (isRemoteGameClient()) {
            return com.ogatamizuki.economy.ClientAccess.requestQuery("STOCKS", playerUuid, 0);
        }
        return EconomyBackends.get().fetchStocks(playerUuid);
    }

    public static CompletableFuture<JsonObject> tradeStock(String playerUuid, String stockCategoryId, String tradeType, int quantity) {
        return EconomyBackends.get().tradeStock(playerUuid, stockCategoryId, tradeType, quantity);
    }

    public static CompletableFuture<String> fetchStockHistory(String stockCategoryId, int limit) {
        if (isRemoteGameClient()) {
            return com.ogatamizuki.economy.ClientAccess.requestQuery("STOCK_HISTORY", stockCategoryId, limit);
        }
        return EconomyBackends.get().fetchStockHistory(stockCategoryId, limit);
    }

    public static CompletableFuture<String> fetchStockComponents(String stockCategoryId) {
        if (isRemoteGameClient()) {
            return com.ogatamizuki.economy.ClientAccess.requestQuery("STOCK_COMPONENTS", stockCategoryId, 0);
        }
        return EconomyBackends.get().fetchStockComponents(stockCategoryId);
    }

    public static CompletableFuture<String> syncRanking(String jsonPayload) {
        return EconomyBackends.get().syncRanking(jsonPayload);
    }

    public static CompletableFuture<String> fetchLatestRanking() {
        if (isRemoteGameClient()) {
            return com.ogatamizuki.economy.ClientAccess.requestQuery("RANKING_LATEST", "", 0);
        }
        return EconomyBackends.get().fetchLatestRanking();
    }

    public static CompletableFuture<JsonObject> fetchLoanLimit(String uuid) {
        if (isRemoteGameClient()) {
            return com.ogatamizuki.economy.ClientAccess.requestQuery("LOAN_LIMIT", uuid, 0)
                    .thenApply(json -> json != null ? JsonParser.parseString(json).getAsJsonObject() : null);
        }
        return EconomyBackends.get().fetchLoanLimit(uuid);
    }

    public static CompletableFuture<JsonObject> borrowLoan(String uuid, int amount) {
        return EconomyBackends.get().borrowLoan(uuid, amount);
    }

    public static CompletableFuture<JsonObject> repayLoan(String uuid, int amount) {
        return EconomyBackends.get().repayLoan(uuid, amount);
    }

    public static CompletableFuture<String> fetchFleaMarketListings() {
        if (isRemoteGameClient()) {
            return com.ogatamizuki.economy.ClientAccess.requestQuery("FLEA_LISTINGS", "", 0);
        }
        return EconomyBackends.get().fetchFleaMarketListings();
    }

    public static CompletableFuture<JsonObject> listFleaMarketItem(
            String sellerUuid,
            String itemKey,
            String itemName,
            int price,
            int quantity,
            net.minecraft.world.item.ItemStack itemStack) {
        return EconomyBackends.get().listFleaMarketItem(sellerUuid, itemKey, itemName, price, quantity, itemStack);
    }

    public static CompletableFuture<JsonObject> buyFleaMarketItem(String buyerUuid, String listingId, int quantity) {
        return EconomyBackends.get().buyFleaMarketItem(buyerUuid, listingId, quantity);
    }

    public static CompletableFuture<JsonObject> cancelFleaMarketListing(String sellerUuid, String listingId) {
        return EconomyBackends.get().cancelFleaMarketListing(sellerUuid, listingId);
    }

    /** 物理クライアントがリモートの Minecraft サーバーに接続中か（統合サーバーでは false）。 */
    private static boolean isRemoteGameClient() {
        return FMLEnvironment.getDist() == Dist.CLIENT && ServerLifecycleHooks.getCurrentServer() == null;
    }
}
