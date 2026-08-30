package com.ogatamizuki.economy.backend;

import com.ogatamizuki.economy.EconomyCommon;
import com.ogatamizuki.economy.EconomyPlatform;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.google.gson.JsonObject;
import com.ogatamizuki.economy.backend.local.EconomyLocalEtfService;
import com.ogatamizuki.economy.backend.local.EconomyLocalFleaMarketService;
import com.ogatamizuki.economy.backend.local.EconomyLocalLoanService;
import com.ogatamizuki.economy.backend.local.EconomyLocalPlayerService;
import com.ogatamizuki.economy.backend.local.EconomyLocalRankingService;
import com.ogatamizuki.economy.backend.local.EconomyLocalShopService;
import com.ogatamizuki.economy.data.EconomyWorldSavedData;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** ワールド内 SavedData による economy バックエンド（単体版）。 */
public final class EconomyLocalBackend implements EconomyBackend {
    @Override
    public CompletableFuture<Void> joinPlayer(String uuid, String username, ServerPlayer syncTarget) {
        if (syncTarget == null) {
            EconomyCommon.LOGGER.warn("joinPlayer called without ServerPlayer for {}", username);
            EconomyCommon.setEconomyReady(UUID.fromString(uuid), false);
            return CompletableFuture.completedFuture(null);
        }

        EconomyLocalPlayerService.join(syncTarget);
        EconomyCommon.LOGGER.info("Economy LOCAL join: {}", username);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> leavePlayer(String uuid, String username) {
        EconomyLocalPlayerService.leave(UUID.fromString(uuid));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> depositBank(String uuid, int amount) {
        return runBankOnServer(uuid, amount, true);
    }

    @Override
    public CompletableFuture<Void> withdrawBank(String uuid, int amount) {
        return runBankOnServer(uuid, amount, false);
    }

    @Override
    public CompletableFuture<Void> rewardPlayer(Player player, String actionType, double ratio) {
        if (player == null || player.level().isClientSide()) {
            return CompletableFuture.completedFuture(null);
        }
        MinecraftServer server = resolveServer();
        if (server == null) {
            return CompletableFuture.completedFuture(null);
        }
        return runOnServer(() -> EconomyLocalPlayerService.reward(player, actionType, ratio), server);
    }

    @Override
    public CompletableFuture<Void> deathPlayer(Player player) {
        if (player == null || player.level().isClientSide()) {
            return CompletableFuture.completedFuture(null);
        }
        MinecraftServer server = resolveServer();
        if (server == null) {
            return CompletableFuture.completedFuture(null);
        }
        return runOnServer(() -> EconomyLocalPlayerService.death(player), server);
    }

    @Override
    public CompletableFuture<String> fetchShopDetails(int shopId, String playerUuid) {
        MinecraftServer server = resolveServer();
        if (server == null) {
            return CompletableFuture.completedFuture(null);
        }
        return supplyOnServer(() ->
                EconomyLocalShopService.fetchShopDetails(shopId, UUID.fromString(playerUuid)), server);
    }

    @Override
    public CompletableFuture<JsonObject> buyShopItem(String playerUuid, int shopItemId, int quantity) {
        return supplyOnServer(() -> {
            JsonObject result = EconomyLocalShopService.buy(UUID.fromString(playerUuid), shopItemId, quantity);
            syncPlayerBalance(UUID.fromString(playerUuid), result);
            return result;
        });
    }

    @Override
    public CompletableFuture<JsonObject> sellShopItem(String playerUuid, int itemId, int quantity) {
        return supplyOnServer(() -> {
            JsonObject result = EconomyLocalShopService.sell(UUID.fromString(playerUuid), itemId, quantity);
            syncPlayerBalance(UUID.fromString(playerUuid), result);
            return result;
        });
    }

    @Override
    public CompletableFuture<String> fetchStocks(String playerUuid) {
        return supplyOnServer(() -> EconomyLocalEtfService.fetchStocks(UUID.fromString(playerUuid)));
    }

    @Override
    public CompletableFuture<JsonObject> tradeStock(String playerUuid, String stockCategoryId, String tradeType, int quantity) {
        return supplyOnServer(() -> {
            JsonObject result = EconomyLocalEtfService.trade(UUID.fromString(playerUuid), stockCategoryId, tradeType, quantity);
            syncPlayerBalance(UUID.fromString(playerUuid), result);
            return result;
        });
    }

    @Override
    public CompletableFuture<String> fetchStockHistory(String stockCategoryId, int limit) {
        return supplyOnServer(() -> EconomyLocalEtfService.fetchHistory(stockCategoryId, limit));
    }

    @Override
    public CompletableFuture<String> fetchStockComponents(String stockCategoryId) {
        return supplyOnServer(() -> EconomyLocalEtfService.fetchComponents(stockCategoryId));
    }

    @Override
    public CompletableFuture<String> syncRanking(String jsonPayload) {
        return supplyOnServer(() -> EconomyLocalRankingService.syncRanking(jsonPayload));
    }

    @Override
    public CompletableFuture<String> fetchLatestRanking() {
        return supplyOnServer(EconomyLocalRankingService::fetchLatest);
    }

    @Override
    public CompletableFuture<JsonObject> fetchLoanLimit(String uuid) {
        return supplyOnServer(() -> EconomyLocalLoanService.fetchLimit(UUID.fromString(uuid)));
    }

    @Override
    public CompletableFuture<JsonObject> borrowLoan(String uuid, int amount) {
        return supplyOnServer(() -> {
            JsonObject result = EconomyLocalLoanService.borrow(UUID.fromString(uuid), amount);
            syncPlayerBalanceAndDebt(UUID.fromString(uuid), result);
            return result;
        });
    }

    @Override
    public CompletableFuture<JsonObject> repayLoan(String uuid, int amount) {
        return supplyOnServer(() -> {
            JsonObject result = EconomyLocalLoanService.repay(UUID.fromString(uuid), amount);
            syncPlayerBalanceAndDebt(UUID.fromString(uuid), result);
            return result;
        });
    }

    @Override
    public CompletableFuture<String> fetchFleaMarketListings() {
        return supplyOnServer(EconomyLocalFleaMarketService::fetchListings);
    }

    @Override
    public CompletableFuture<JsonObject> listFleaMarketItem(
            String sellerUuid,
            String itemKey,
            String itemName,
            int price,
            int quantity,
            net.minecraft.world.item.ItemStack itemStack) {
        return supplyOnServer(() -> EconomyLocalFleaMarketService.listItem(
                UUID.fromString(sellerUuid),
                sellerNameOrEmpty(sellerUuid),
                itemKey,
                itemName,
                price,
                quantity,
                itemStack == null ? net.minecraft.world.item.ItemStack.EMPTY : itemStack.copy()));
    }

    @Override
    public CompletableFuture<JsonObject> buyFleaMarketItem(String buyerUuid, String listingId, int quantity) {
        return supplyOnServer(() -> {
            JsonObject result = EconomyLocalFleaMarketService.buy(UUID.fromString(buyerUuid), listingId, quantity);
            syncPlayerBalance(UUID.fromString(buyerUuid), result);
            if (result != null && result.has("sellerUuid") && result.has("sellerNewBalance")) {
                UUID sellerUuid = UUID.fromString(result.get("sellerUuid").getAsString());
                int sellerBalance = result.get("sellerNewBalance").getAsInt();
                syncPlayerBalanceValue(sellerUuid, sellerBalance);
                notifyFleaMarketSeller(result);
                EconomyCommon.LOGGER.info(
                        "Flea market sale: buyer={} seller={} +{} -> sellerBalance={}",
                        buyerUuid,
                        sellerUuid,
                        result.has("totalPrice") ? result.get("totalPrice").getAsInt() : -1,
                        sellerBalance
                );
            }
            return result;
        });
    }

    @Override
    public CompletableFuture<JsonObject> cancelFleaMarketListing(String sellerUuid, String listingId) {
        return supplyOnServer(() -> EconomyLocalFleaMarketService.cancel(UUID.fromString(sellerUuid), listingId));
    }

    private static String sellerNameOrEmpty(String sellerUuid) {
        MinecraftServer server = resolveServer();
        if (server == null) {
            return "";
        }
        ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(sellerUuid));
        return player != null ? player.getName().getString() : "";
    }

    private CompletableFuture<Void> runBankOnServer(String uuid, int amount, boolean deposit) {
        MinecraftServer server = resolveServer();
        if (server == null) {
            return CompletableFuture.completedFuture(null);
        }
        return runOnServer(() -> {
            UUID playerUuid = UUID.fromString(uuid);
            if (deposit) {
                EconomyLocalPlayerService.deposit(playerUuid, amount);
            } else {
                EconomyLocalPlayerService.withdraw(playerUuid, amount);
            }
        }, server);
    }

    private static void syncPlayerBalance(UUID playerUuid, JsonObject result) {
        if (result == null
                || !result.has("success")
                || !result.get("success").getAsBoolean()
                || !result.has("newBalance")) {
            return;
        }
        syncPlayerBalanceValue(playerUuid, result.get("newBalance").getAsInt());
    }

    private static void syncPlayerBalanceValue(UUID playerUuid, int newBalance) {
        MinecraftServer server = resolveServer();
        if (server == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player == null) {
            EconomyCommon.LOGGER.debug("Flea/balance sync skipped (offline): {} -> {}", playerUuid, newBalance);
            return;
        }
        var data = EconomyWorldSavedData.get(server);
        var record = data.getOrCreate(playerUuid, player.getName().getString());
        EconomyBalanceSync.applyBalanceAndSync(player, newBalance, record.bankBalance(), record.debt());
    }

    private static void notifyFleaMarketSeller(JsonObject result) {
        if (result == null || !result.has("sellerUuid") || !result.has("sellerNewBalance")) {
            return;
        }
        MinecraftServer server = resolveServer();
        if (server == null) {
            return;
        }
        UUID sellerUuid = UUID.fromString(result.get("sellerUuid").getAsString());
        ServerPlayer seller = server.getPlayerList().getPlayer(sellerUuid);
        if (seller == null) {
            return;
        }
        String itemKey = result.has("itemKey") ? result.get("itemKey").getAsString() : null;
        String stackNbt = result.has("itemStackNbt") ? result.get("itemStackNbt").getAsString() : "";
        String fallbackName = result.has("itemName") ? result.get("itemName").getAsString() : "アイテム";
        int totalPrice = result.has("totalPrice") ? result.get("totalPrice").getAsInt() : 0;
        int sellerBalance = result.get("sellerNewBalance").getAsInt();
        String valKey = (itemKey != null && !itemKey.isEmpty()) ? itemKey : fallbackName;
        seller.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "economy.chat.flea.sold_notification|" + valKey + "|" + totalPrice + "|" + sellerBalance
        ));
    }

    private static void syncPlayerBalanceAndDebt(UUID playerUuid, JsonObject result) {
        if (result == null || !result.has("success") || !result.get("success").getAsBoolean()) {
            return;
        }
        MinecraftServer server = resolveServer();
        if (server == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player == null) {
            return;
        }
        var data = EconomyWorldSavedData.get(server);
        var record = data.getOrCreate(playerUuid, player.getName().getString());
        int balance = result.has("newBalance") ? result.get("newBalance").getAsInt() : record.balance();
        int debt = result.has("newDebt") ? result.get("newDebt").getAsInt() : record.debt();
        EconomyBalanceSync.applyBalanceAndSync(player, balance, record.bankBalance(), debt);
    }

    private static MinecraftServer resolveServer() {
        return EconomyPlatform.getServer();
    }

    private static CompletableFuture<Void> runOnServer(Runnable action, MinecraftServer server) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        server.execute(() -> {
            try {
                action.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private static <T> CompletableFuture<T> supplyOnServer(Supplier<T> supplier) {
        MinecraftServer server = resolveServer();
        if (server == null) {
            return CompletableFuture.completedFuture(null);
        }
        return supplyOnServer(supplier, server);
    }

    private static <T> CompletableFuture<T> supplyOnServer(Supplier<T> supplier, MinecraftServer server) {
        CompletableFuture<T> future = new CompletableFuture<>();
        server.execute(() -> {
            try {
                future.complete(supplier.get());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}
