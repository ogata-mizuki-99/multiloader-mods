package com.ogatamizuki.economy.backend.local;

import com.ogatamizuki.economy.EconomyCommon;

import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ogatamizuki.economy.EconomyFeatures;
import com.ogatamizuki.economy.data.EconomyEtfWorldSavedData;
import com.ogatamizuki.economy.data.EconomyPersist;
import com.ogatamizuki.economy.data.EconomyWorldSavedData;
import com.ogatamizuki.economy.master.EconomyMasterData;

import net.minecraft.server.MinecraftServer;

/** ETF 取引・株価更新のローカル処理。 */
public final class EconomyLocalEtfService {
    public static final int ETF_MIN_PRICE = 10;
    public static final double ETF_TRADE_IMPACT = 1.0;

    private EconomyLocalEtfService() {
    }

    public static String fetchStocks(UUID playerUuid) {
        MinecraftServer server = EconomyLocalPlayerService.server();
        if (server == null) {
            return null;
        }
        EconomyEtfWorldSavedData etfData = EconomyEtfWorldSavedData.get(server);
        EconomyWorldSavedData worldData = EconomyWorldSavedData.get(server);
        EconomyWorldSavedData.PlayerRecord player = worldData.getOrCreate(playerUuid, "");

        int shortSellAvailable = calculateShortSellAvailable(server, playerUuid, player);

        JsonArray array = new JsonArray();
        for (EconomyEtfWorldSavedData.CategoryState category : etfData.enabledCategories()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", category.id());
            obj.addProperty("code", category.code());
            obj.addProperty("name", category.name());
            if (category.description() != null) {
                obj.addProperty("description", category.description());
            }
            obj.addProperty("currentPrice", category.currentPrice());
            obj.addProperty("enabled", category.enabled());
            obj.addProperty("portfolioQuantity", etfData.portfolioQuantity(playerUuid, category.code()));
            obj.addProperty("shortSellAvailable", shortSellAvailable);
            array.add(obj);
        }
        return array.toString();
    }

    public static String fetchHistory(String stockCategoryId, int limit) {
        MinecraftServer server = EconomyLocalPlayerService.server();
        if (server == null) {
            return null;
        }
        EconomyEtfWorldSavedData etfData = EconomyEtfWorldSavedData.get(server);
        int capped = Math.min(Math.max(limit, 1), 500);
        JsonArray array = new JsonArray();
        for (EconomyEtfWorldSavedData.HistoryEntry entry : etfData.priceHistory(stockCategoryId, capped)) {
            JsonObject obj = new JsonObject();
            obj.addProperty("price", entry.price());
            obj.addProperty("source", entry.source());
            array.add(obj);
        }
        return array.toString();
    }

    public static String fetchComponents(String stockCategoryId) {
        EconomyEtfWorldSavedData.CategoryState category = resolveCategory(stockCategoryId);
        if (category == null) {
            return "[]";
        }
        JsonArray array = new JsonArray();
        for (EconomyMasterData.EtfItemDef item : EconomyMasterData.get().etfItemsForCode(category.code())) {
            JsonObject obj = new JsonObject();
            obj.addProperty("item_key", item.itemKey());
            obj.addProperty("influence_weight", item.influenceWeight());
            array.add(obj);
        }
        return array.toString();
    }

    public static JsonObject trade(UUID playerUuid, String stockCategoryId, String tradeType, int quantity) {
        if (quantity <= 0) {
            return error("quantity must be a positive integer");
        }
        MinecraftServer server = EconomyLocalPlayerService.server();
        if (server == null) {
            return error("World data unavailable");
        }
        EconomyEtfWorldSavedData etfData = EconomyEtfWorldSavedData.get(server);
        EconomyEtfWorldSavedData.CategoryState category = resolveCategory(stockCategoryId);
        if (category == null || !category.enabled()) {
            return error("Stock category not found");
        }

        EconomyWorldSavedData worldData = EconomyWorldSavedData.get(server);
        EconomyWorldSavedData.PlayerRecord player = worldData.getOrCreate(playerUuid, "");
        int price = category.currentPrice();
        int tradeAmount = price * quantity;
        int currentQty = etfData.portfolioQuantity(playerUuid, category.code());

        int newQty = currentQty;
        int newBalance = player.balance();
        int etfBuyInc = 0;
        int etfShortInc = 0;
        int etfProfitInc = 0;
        int earningsInc = 0;
        int marketImpact = 0;

        switch (tradeType) {
            case "BUY" -> {
                if (currentQty < 0) {
                    return error("Short position exists. Use BUY_COVER instead.");
                }
                if (player.balance() < tradeAmount) {
                    return error("Insufficient balance");
                }
                newQty = currentQty + quantity;
                newBalance = player.balance() - tradeAmount;
                etfBuyInc = tradeAmount;
                marketImpact = (int) Math.round(Math.sqrt(quantity) * ETF_TRADE_IMPACT);
            }
            case "SELL" -> {
                if (currentQty <= 0) {
                    return error("No shares to sell");
                }
                if (quantity > currentQty) {
                    return error("Cannot sell more than owned shares");
                }
                newQty = currentQty - quantity;
                newBalance = player.balance() + tradeAmount;
                etfProfitInc = tradeAmount;
                earningsInc = tradeAmount;
                marketImpact = -(int) Math.round(Math.sqrt(quantity) * ETF_TRADE_IMPACT);
            }
            case "SELL_SHORT" -> {
                int shortExposure = calculateShortExposure(server, playerUuid);
                double limitRate = EconomyMasterData.get().shortSellLimitRate();
                int maxShort = (int) Math.floor((player.balance() + player.bankBalance()) * limitRate);
                if (shortExposure + tradeAmount > maxShort) {
                    return error(String.format(
                            "Short selling limit exceeded. (Limit: ¥%d, Current exposure: ¥%d, Attempted: ¥%d)",
                            maxShort, shortExposure, tradeAmount));
                }
                newQty = currentQty - quantity;
                newBalance = player.balance() + tradeAmount;
                etfShortInc = tradeAmount;
                marketImpact = -(int) Math.round(Math.sqrt(quantity) * ETF_TRADE_IMPACT);
            }
            case "BUY_COVER" -> {
                if (currentQty >= 0) {
                    return error("No short position to cover");
                }
                if (quantity > Math.abs(currentQty)) {
                    return error("Cannot cover more than short position");
                }
                if (player.balance() < tradeAmount) {
                    return error("Insufficient balance");
                }
                newQty = currentQty + quantity;
                newBalance = player.balance() - tradeAmount;
                marketImpact = (int) Math.round(Math.sqrt(quantity) * ETF_TRADE_IMPACT);
            }
            default -> {
                return error("Invalid trade type");
            }
        }

        EconomyWorldSavedData.PlayerRecord updatedPlayer = new EconomyWorldSavedData.PlayerRecord(
                player.username(),
                newBalance,
                player.bankBalance(),
                player.debt(),
                player.totalEarnings() + earningsInc,
                player.totalLost(),
                player.etfBuyAmount() + etfBuyInc,
                player.etfShortAmount() + etfShortInc,
                player.etfProfitAmount() + etfProfitInc,
                player.totalTradeCount() + 1
        );
        worldData.putPlayer(playerUuid, updatedPlayer);
        etfData.setPortfolioQuantity(playerUuid, category.code(), newQty);

        int finalPrice = price;
        if (marketImpact != 0) {
            int newPrice = Math.max(ETF_MIN_PRICE, price + marketImpact);
            if (newPrice != price) {
                String source = marketImpact > 0 ? "SHOP_BUY" : "SHOP_SELL";
                etfData.updateCategoryPrice(category.code(), newPrice, source);
                finalPrice = newPrice;
            }
        }

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("newBalance", newBalance);
        result.addProperty("currentPrice", finalPrice);
        result.addProperty("portfolioQuantity", newQty);
        return result;
    }

    public static void updatePricesFromShopTrade(String itemKey, int quantity, String direction) {
        if (!EconomyFeatures.isEtfUpdatesEnabled()) {
            return;
        }
        MinecraftServer server = EconomyLocalPlayerService.server();
        if (server == null || quantity <= 0) {
            return;
        }
        EconomyEtfWorldSavedData etfData = EconomyEtfWorldSavedData.get(server);
        String source = "buy".equals(direction) ? "SHOP_BUY" : "SHOP_SELL";
        for (EconomyMasterData.EtfItemDef etfItem : EconomyMasterData.get().etfItemsForItemKey(itemKey)) {
            int delta = (int) Math.round(Math.sqrt(quantity) * etfItem.influenceWeight() * 10);
            if (delta == 0) {
                continue;
            }
            EconomyEtfWorldSavedData.CategoryState category = etfData.category(etfItem.etfCode());
            if (category == null) {
                continue;
            }
            int change = "buy".equals(direction) ? delta : -delta;
            int newPrice = Math.max(ETF_MIN_PRICE, category.currentPrice() + change);
            if (newPrice != category.currentPrice()) {
                etfData.updateCategoryPrice(category.code(), newPrice, source);
            }
        }
    }

    public static void applyRandomWalk() {
        if (!EconomyFeatures.isEtfUpdatesEnabled()) {
            return;
        }
        MinecraftServer server = EconomyLocalPlayerService.server();
        if (server == null) {
            return;
        }
        EconomyEtfWorldSavedData etfData = EconomyEtfWorldSavedData.get(server);
        for (EconomyEtfWorldSavedData.CategoryState category : etfData.enabledCategories()) {
            double ratio = (double) category.currentPrice() / category.seedPrice();
            double reversionBias = 0;
            if (ratio > 3.0) {
                reversionBias = -0.01;
            } else if (ratio < 0.33) {
                reversionBias = 0.01;
            }
            int sign = Math.random() < 0.5 ? -1 : 1;
            double magnitude = 0.01 + Math.random() * 0.04;
            double rate = sign * magnitude + reversionBias;
            int newPrice = Math.max(ETF_MIN_PRICE, (int) Math.round(category.currentPrice() * (1 + rate)));
            if (newPrice != category.currentPrice()) {
                etfData.updateCategoryPrice(category.code(), newPrice, "RANDOM_WALK");
            }
        }
        EconomyPersist.saveAll(server);
        EconomyCommon.LOGGER.debug("ETF random walk applied");
    }

    /** 空売り建玉の時価評価額合計（|口数| × 現在株価）。ランキングでは借金扱い。 */
    public static int calculateShortExposure(MinecraftServer server, UUID playerUuid) {
        if (server == null || playerUuid == null) {
            return 0;
        }
        EconomyEtfWorldSavedData etfData = EconomyEtfWorldSavedData.get(server);
        int exposure = 0;
        for (Map.Entry<String, Integer> entry : etfData.portfolio(playerUuid).entrySet()) {
            if (entry.getValue() < 0) {
                EconomyEtfWorldSavedData.CategoryState category = etfData.category(entry.getKey());
                if (category != null) {
                    exposure += Math.abs(entry.getValue()) * category.currentPrice();
                }
            }
        }
        return exposure;
    }

    private static int calculateShortSellAvailable(MinecraftServer server, UUID playerUuid, EconomyWorldSavedData.PlayerRecord player) {
        double limitRate = EconomyMasterData.get().shortSellLimitRate();
        int maxShort = (int) Math.floor((player.balance() + player.bankBalance()) * limitRate);
        return Math.max(0, maxShort - calculateShortExposure(server, playerUuid));
    }

    private static EconomyEtfWorldSavedData.CategoryState resolveCategory(String stockCategoryId) {
        MinecraftServer server = EconomyLocalPlayerService.server();
        if (server == null) {
            return null;
        }
        EconomyEtfWorldSavedData etfData = EconomyEtfWorldSavedData.get(server);
        EconomyEtfWorldSavedData.CategoryState category = etfData.category(stockCategoryId);
        if (category != null) {
            return category;
        }
        for (EconomyEtfWorldSavedData.CategoryState candidate : etfData.enabledCategories()) {
            if (candidate.id().equals(stockCategoryId)) {
                return candidate;
            }
        }
        return null;
    }

    private static JsonObject error(String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("error", message);
        return obj;
    }
}
