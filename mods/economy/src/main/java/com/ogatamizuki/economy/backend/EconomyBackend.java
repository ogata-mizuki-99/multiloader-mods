package com.ogatamizuki.economy.backend;

import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Economy データ操作の抽象化レイヤ。
 * 実装は {@link EconomyLocalBackend}（ワールド内 SavedData）に固定。外部 API は不要。
 */
public interface EconomyBackend {

    CompletableFuture<Void> joinPlayer(String uuid, String username, ServerPlayer syncTarget);

    CompletableFuture<Void> leavePlayer(String uuid, String username);

    CompletableFuture<Void> depositBank(String uuid, int amount);

    CompletableFuture<Void> withdrawBank(String uuid, int amount);

    CompletableFuture<Void> rewardPlayer(Player player, String actionType, double ratio);

    CompletableFuture<Void> deathPlayer(Player player);

    CompletableFuture<String> fetchShopDetails(int shopId, String playerUuid);

    CompletableFuture<JsonObject> buyShopItem(String playerUuid, int shopItemId, int quantity);

    CompletableFuture<JsonObject> sellShopItem(String playerUuid, int itemId, int quantity);

    CompletableFuture<String> fetchStocks(String playerUuid);

    CompletableFuture<JsonObject> tradeStock(String playerUuid, String stockCategoryId, String tradeType, int quantity);

    CompletableFuture<String> fetchStockHistory(String stockCategoryId, int limit);

    CompletableFuture<String> fetchStockComponents(String stockCategoryId);

    CompletableFuture<String> syncRanking(String jsonPayload);

    CompletableFuture<String> fetchLatestRanking();

    CompletableFuture<JsonObject> fetchLoanLimit(String uuid);

    CompletableFuture<JsonObject> borrowLoan(String uuid, int amount);

    CompletableFuture<JsonObject> repayLoan(String uuid, int amount);

    CompletableFuture<String> fetchFleaMarketListings();

    CompletableFuture<JsonObject> listFleaMarketItem(
            String sellerUuid,
            String itemKey,
            String itemName,
            int price,
            int quantity,
            net.minecraft.world.item.ItemStack itemStack);

    CompletableFuture<JsonObject> buyFleaMarketItem(String buyerUuid, String listingId, int quantity);

    CompletableFuture<JsonObject> cancelFleaMarketListing(String sellerUuid, String listingId);
}
