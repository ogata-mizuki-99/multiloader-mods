package com.ogatamizuki.economy.backend.local;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ogatamizuki.economy.data.EconomyWorldSavedData;
import com.ogatamizuki.economy.master.EconomyMasterData;

/** ショップ取得・購入・売却のローカル処理。 */
public final class EconomyLocalShopService {
    /** Minecraft の UTF-8 文字列上限 (32767) より余裕を持たせる。 */
    private static final int MAX_PACKET_CHARS = 28_000;

    private EconomyLocalShopService() {
    }

    public static String fetchShopDetails(int shopId, UUID playerUuid) {
        List<String> chunks = fetchShopDetailsChunks(shopId, playerUuid);
        if (chunks.isEmpty()) {
            return null;
        }
        if (chunks.size() == 1) {
            return chunks.getFirst();
        }
        return mergeShopDetailChunks(chunks);
    }

    public static List<String> fetchShopDetailsChunks(int shopId, UUID playerUuid) {
        EconomyMasterData master = EconomyMasterData.get();
        var shopOpt = master.shop(shopId);
        if (shopOpt.isEmpty()) {
            return List.of();
        }
        EconomyMasterData.ShopDef shop = shopOpt.get();
        EconomyWorldSavedData data = worldData();
        if (data == null) {
            return List.of();
        }

        List<JsonObject> itemObjects = new ArrayList<>();
        if ("BUYER".equalsIgnoreCase(shop.npcType())) {
            for (EconomyMasterData.ItemDef item : master.allEnabledItems()) {
                if (item.sellPrice() != null && item.sellPrice() > 0) {
                    itemObjects.add(buildBuyerItemJson(item));
                }
            }
        } else {
            for (EconomyMasterData.ShopItemDef shopItem : master.shopItemsForShop(shopId)) {
                master.item(shopItem.itemId()).ifPresent(item ->
                        itemObjects.add(buildSellerItemJson(data, playerUuid, shopItem, item)));
            }
        }

        JsonObject header = new JsonObject();
        header.addProperty("shopId", shop.id());
        header.addProperty("shopName", shop.shopName());
        header.addProperty("npcType", shop.npcType());
        header.addProperty("npcModel", shop.npcModel());

        List<String> chunks = new ArrayList<>();
        JsonArray batch = new JsonArray();
        for (JsonObject itemObject : itemObjects) {
            batch.add(itemObject);
            JsonObject probe = header.deepCopy();
            probe.add("items", batch);
            if (probe.toString().length() > MAX_PACKET_CHARS && batch.size() > 1) {
                batch.remove(batch.size() - 1);
                chunks.add(buildShopDetailsChunk(header, batch));
                batch = new JsonArray();
                batch.add(itemObject);
            }
        }
        if (!batch.isEmpty()) {
            chunks.add(buildShopDetailsChunk(header, batch));
        }
        if (chunks.isEmpty()) {
            chunks.add(buildShopDetailsChunk(header, new JsonArray()));
        }
        return chunks;
    }

    private static String buildShopDetailsChunk(JsonObject header, JsonArray items) {
        JsonObject root = header.deepCopy();
        root.add("items", items);
        return root.toString();
    }

    static String mergeShopDetailChunks(List<String> chunks) {
        JsonObject merged = JsonParser.parseString(chunks.getFirst()).getAsJsonObject();
        JsonArray mergedItems = merged.getAsJsonArray("items");
        for (int i = 1; i < chunks.size(); i++) {
            JsonArray partItems = JsonParser.parseString(chunks.get(i)).getAsJsonObject().getAsJsonArray("items");
            partItems.forEach(mergedItems::add);
        }
        return merged.toString();
    }

    public static JsonObject buy(UUID playerUuid, int shopItemId, int quantity) {
        try {
            return buyInternal(playerUuid, shopItemId, quantity);
        } catch (Exception e) {
            com.ogatamizuki.economy.EconomyCommon.LOGGER.error(
                    "Shop buy threw for player={} shopItemId={} qty={}", playerUuid, shopItemId, quantity, e);
            return error("購入処理中に内部エラーが発生しました。");
        }
    }

    private static JsonObject buyInternal(UUID playerUuid, int shopItemId, int quantity) {
        if (quantity <= 0) {
            return error("Invalid quantity");
        }
        EconomyMasterData master = EconomyMasterData.get();
        var shopItemOpt = master.shopItem(shopItemId);
        if (shopItemOpt.isEmpty()) {
            return error("Shop item not found");
        }
        EconomyMasterData.ShopItemDef shopItem = shopItemOpt.get();
        var itemOpt = master.item(shopItem.itemId());
        if (itemOpt.isEmpty()) {
            return error("Item not found");
        }
        EconomyMasterData.ItemDef item = itemOpt.get();
        if (item.buyPrice() == null || item.buyPrice() <= 0) {
            return error("Item is not for sale");
        }

        EconomyWorldSavedData data = worldData();
        if (data == null) {
            return error("World data unavailable");
        }

        int userBought = data.getUserBoughtToday(playerUuid, shopItemId);
        int globalBought = data.getGlobalBoughtToday(shopItemId);
        if (shopItem.userLimit() != null && userBought + quantity > shopItem.userLimit()) {
            return error("User purchase limit exceeded");
        }
        if (shopItem.dailyLimit() != null && globalBought + quantity > shopItem.dailyLimit()) {
            return error("Daily purchase limit exceeded");
        }

        int totalPrice = item.buyPrice() * quantity;
        EconomyWorldSavedData.PlayerRecord current = data.getOrCreate(playerUuid, "");
        if (current.balance() < totalPrice) {
            return error("Insufficient balance (have=" + current.balance() + ", need=" + totalPrice + ")");
        }

        EconomyWorldSavedData.PlayerRecord updated = new EconomyWorldSavedData.PlayerRecord(
                current.username(),
                current.balance() - totalPrice,
                current.bankBalance(),
                current.debt(),
                current.totalEarnings(),
                current.totalLost(),
                current.etfBuyAmount(),
                current.etfShortAmount(),
                current.etfProfitAmount(),
                current.totalTradeCount()
        );
        // 購入数更新を先に（失敗時に残高だけ減るのを防ぐ）
        data.addShopPurchase(playerUuid, shopItemId, quantity);
        data.putPlayer(playerUuid, updated);
        EconomyLocalEtfService.updatePricesFromShopTrade(item.itemKey(), quantity, "buy");

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("itemName", item.name());
        result.addProperty("itemKey", item.itemKey());
        result.addProperty("quantity", quantity);
        result.addProperty("totalPrice", totalPrice);
        result.addProperty("newBalance", updated.balance());
        result.addProperty("userBoughtToday", data.getUserBoughtToday(playerUuid, shopItemId));
        return result;
    }

    public static JsonObject sell(UUID playerUuid, int itemId, int quantity) {
        if (quantity <= 0) {
            return error("Invalid quantity");
        }
        EconomyMasterData master = EconomyMasterData.get();
        var itemOpt = master.item(itemId);
        if (itemOpt.isEmpty()) {
            return error("Item not found or selling is disabled");
        }
        EconomyMasterData.ItemDef item = itemOpt.get();
        if (item.sellPrice() == null || item.sellPrice() <= 0) {
            return error("This item cannot be sold");
        }

        EconomyWorldSavedData data = worldData();
        if (data == null) {
            return error("World data unavailable");
        }

        int totalGain = item.sellPrice() * quantity;
        EconomyWorldSavedData.PlayerRecord current = data.getOrCreate(playerUuid, "");
        EconomyWorldSavedData.PlayerRecord updated = new EconomyWorldSavedData.PlayerRecord(
                current.username(),
                current.balance() + totalGain,
                current.bankBalance(),
                current.debt(),
                current.totalEarnings() + totalGain,
                current.totalLost(),
                current.etfBuyAmount(),
                current.etfShortAmount(),
                current.etfProfitAmount(),
                current.totalTradeCount()
        );
        data.putPlayer(playerUuid, updated);
        EconomyLocalEtfService.updatePricesFromShopTrade(item.itemKey(), quantity, "sell");

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("itemId", item.id());
        result.addProperty("itemName", item.name());
        result.addProperty("itemKey", item.itemKey());
        if (item.matchPotion() != null) {
            result.addProperty("matchPotion", item.matchPotion());
        }
        if (item.matchEnchantment() != null) {
            result.addProperty("matchEnchantment", item.matchEnchantment());
        }
        if (item.matchEnchantmentLevel() != null) {
            result.addProperty("matchEnchantmentLevel", item.matchEnchantmentLevel());
        }
        result.addProperty("quantity", quantity);
        result.addProperty("totalGain", totalGain);
        result.addProperty("newBalance", updated.balance());
        return result;
    }

    private static JsonObject buildBuyerItemJson(EconomyMasterData.ItemDef item) {
        JsonObject obj = new JsonObject();
        obj.addProperty("order_no", item.id());
        obj.addProperty("item_id", item.id());
        obj.addProperty("item_name", item.name());
        obj.addProperty("item_key", item.itemKey());
        obj.addProperty("sell_price", item.sellPrice());
        if (item.matchPotion() != null) {
            obj.addProperty("match_potion", item.matchPotion());
        }
        if (item.matchEnchantment() != null) {
            obj.addProperty("match_enchantment", item.matchEnchantment());
        }
        if (item.matchEnchantmentLevel() != null) {
            obj.addProperty("match_enchantment_level", item.matchEnchantmentLevel());
        }
        return obj;
    }

    private static JsonObject buildSellerItemJson(
            EconomyWorldSavedData data,
            UUID playerUuid,
            EconomyMasterData.ShopItemDef shopItem,
            EconomyMasterData.ItemDef item
    ) {
        int userBought = data.getUserBoughtToday(playerUuid, shopItem.id());
        int globalBought = data.getGlobalBoughtToday(shopItem.id());
        Integer remainingUser = shopItem.userLimit() != null ? Math.max(0, shopItem.userLimit() - userBought) : null;
        Integer remainingDaily = shopItem.dailyLimit() != null ? Math.max(0, shopItem.dailyLimit() - globalBought) : null;

        JsonObject obj = new JsonObject();
        obj.addProperty("shop_item_id", shopItem.id());
        obj.addProperty("order_no", shopItem.orderNo());
        obj.addProperty("item_id", item.id());
        obj.addProperty("item_name", item.name());
        obj.addProperty("item_unit", item.unit());
        obj.addProperty("item_key", item.itemKey());
        addNullablePrice(obj, "buy_price", item.buyPrice());
        addNullablePrice(obj, "sell_price", item.sellPrice());
        addNullableInt(obj, "daily_limit", shopItem.dailyLimit());
        addNullableInt(obj, "user_limit", shopItem.userLimit());
        obj.addProperty("user_bought_today", userBought);
        obj.addProperty("global_bought_today", globalBought);
        addNullableInt(obj, "remaining_user_limit", remainingUser);
        addNullableInt(obj, "remaining_daily_limit", remainingDaily);
        return obj;
    }

    private static void addNullablePrice(JsonObject obj, String key, Integer value) {
        if (value == null) {
            obj.add(key, JsonNull.INSTANCE);
        } else {
            obj.addProperty(key, value);
        }
    }

    private static void addNullableInt(JsonObject obj, String key, Integer value) {
        if (value == null) {
            obj.add(key, JsonNull.INSTANCE);
        } else {
            obj.addProperty(key, value);
        }
    }

    private static JsonObject error(String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("success", false);
        obj.addProperty("error", message);
        return obj;
    }

    private static EconomyWorldSavedData worldData() {
        return EconomyLocalPlayerService.worldData();
    }
}
