package com.ogatamizuki.economy.master;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ogatamizuki.economy.EconomyMod;
import java.util.HashMap;
import java.util.Map;

/** 物理クライアント: MOD 同梱マスタの読み取り（ソロ時フォールバック用）。 */
public final class EconomyMasterClientData {
    private EconomyMasterClientData() {
    }

    public static String fetchRewardsJson() {
        return fetchSection("actionRewards", true);
    }

    public static String fetchItemsJson() {
        return fetchSection("items", false);
    }

    public static String fetchShopsJson() {
        return fetchSection("shops", false, true);
    }

    public static String fetchShopItemsJson() {
        try (InputStream in = EconomyMod.class.getResourceAsStream("/data/economy/master/economy_master.json")) {
            if (in == null) {
                return null;
            }
            JsonObject manifest = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<Integer, String> itemNames = buildEnabledItemNames(manifest);
            Map<Integer, String> itemKeys = buildEnabledItemKeys(manifest);
            Map<Integer, ShopInfo> shops = buildEnabledShops(manifest);
            JsonArray source = manifest.getAsJsonArray("shopItems");
            if (source == null) {
                return emptyResult();
            }
            JsonArray entries = new JsonArray();
            for (var element : source) {
                JsonObject row = element.getAsJsonObject();
                if (row.has("enabled") && !row.get("enabled").getAsBoolean()) {
                    continue;
                }
                JsonObject out = new JsonObject();
                int id = row.get("id").getAsInt();
                int shopId = row.get("shopId").getAsInt();
                int itemId = row.get("itemId").getAsInt();
                out.addProperty("id", id);
                out.addProperty("shopId", shopId);
                out.addProperty("itemId", itemId);
                out.addProperty("orderNo", row.get("orderNo").getAsInt());
                String itemName = itemNames.get(itemId);
                if (itemName != null) {
                    out.addProperty("itemName", itemName);
                }
                String itemKey = itemKeys.get(itemId);
                if (itemKey != null) {
                    out.addProperty("itemKey", itemKey);
                }
                ShopInfo shop = shops.get(shopId);
                if (shop != null) {
                    out.addProperty("shopName", shop.shopName());
                    out.addProperty("npcType", shop.npcType());
                }
                if (row.has("dailyLimit") && !row.get("dailyLimit").isJsonNull()) {
                    out.addProperty("dailyLimit", row.get("dailyLimit").getAsInt());
                }
                if (row.has("userLimit") && !row.get("userLimit").isJsonNull()) {
                    out.addProperty("userLimit", row.get("userLimit").getAsInt());
                }
                entries.add(out);
            }
            JsonObject root = new JsonObject();
            root.addProperty("source", "client-bundled");
            root.addProperty("sourceHint", "MOD同梱マスタ（ローカル読込）");
            root.addProperty("total", entries.size());
            root.add("entries", entries);
            return root.toString();
        } catch (Exception e) {
            EconomyMod.LOGGER.error("Failed to read bundled shop items on client", e);
            return null;
        }
    }

    public static String fetchEtfItemsJson() {
        try (InputStream in = EconomyMod.class.getResourceAsStream("/data/economy/master/economy_master.json")) {
            if (in == null) {
                return null;
            }
            JsonObject manifest = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, String> itemNamesByKey = buildEnabledItemNamesByKey(manifest);
            JsonArray source = manifest.getAsJsonArray("etfItems");
            if (source == null) {
                return emptyResult();
            }
            JsonArray entries = new JsonArray();
            for (var element : source) {
                JsonObject row = element.getAsJsonObject();
                if (row.has("enabled") && !row.get("enabled").getAsBoolean()) {
                    continue;
                }
                String itemKey = row.get("itemKey").getAsString();
                JsonObject out = new JsonObject();
                out.addProperty("etfCode", row.get("etfCode").getAsString());
                out.addProperty("itemKey", itemKey);
                out.addProperty("influenceWeight", row.get("influenceWeight").getAsDouble());
                String itemName = itemNamesByKey.get(itemKey);
                if (itemName != null) {
                    out.addProperty("itemName", itemName);
                }
                entries.add(out);
            }
            JsonObject root = new JsonObject();
            root.addProperty("source", "client-bundled");
            root.addProperty("sourceHint", "MOD同梱マスタ（ローカル読込）");
            root.addProperty("total", entries.size());
            root.add("entries", entries);
            return root.toString();
        } catch (Exception e) {
            EconomyMod.LOGGER.error("Failed to read bundled etf items on client", e);
            return null;
        }
    }

    public static String fetchConfigJson() {
        try (InputStream in = EconomyMod.class.getResourceAsStream("/data/economy/master/economy_master.json")) {
            if (in == null) {
                return null;
            }
            JsonObject manifest = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            double deathPenalty = EconomyBalanceDefaults.DEATH_PENALTY_RATE;
            double shortSell = EconomyBalanceDefaults.SHORT_SELL_LIMIT_RATE;
            int etfInterval = EconomyBalanceDefaults.ETF_RANDOM_WALK_INTERVAL_MINUTES;
            int loanMax = EconomyBalanceDefaults.LOAN_MAX_AMOUNT;
            double loanMultiplier = EconomyBalanceDefaults.LOAN_ASSET_MULTIPLIER;
            JsonArray config = manifest.getAsJsonArray("config");
            if (config != null) {
                for (var element : config) {
                    JsonObject entry = element.getAsJsonObject();
                    if (entry.has("enabled") && !entry.get("enabled").getAsBoolean()) {
                        continue;
                    }
                    String actionType = entry.get("actionType").getAsString();
                    JsonObject value = entry.getAsJsonObject("value");
                    if (value == null) {
                        continue;
                    }
                    switch (actionType) {
                        case "DEATH_PENALTY_RATE" -> {
                            if (value.has("money")) {
                                deathPenalty = Math.abs(value.get("money").getAsDouble());
                            }
                        }
                        case "SHORT_SELL_LIMIT_RATE" -> {
                            if (value.has("rate")) {
                                shortSell = value.get("rate").getAsDouble();
                            }
                        }
                        case "ETF_RANDOM_WALK_INTERVAL" -> {
                            if (value.has("minutes")) {
                                etfInterval = value.get("minutes").getAsInt();
                            }
                        }
                        case "LOAN_DEBT_LIMIT" -> {
                            if (value.has("max_amount")) {
                                loanMax = value.get("max_amount").getAsInt();
                            }
                            if (value.has("asset_multiplier")) {
                                loanMultiplier = value.get("asset_multiplier").getAsDouble();
                            }
                        }
                        default -> {
                        }
                    }
                }
            }
            JsonObject root = new JsonObject();
            root.addProperty("source", "client-bundled");
            root.addProperty("customOverride", false);
            root.addProperty("sourceHint", "MOD同梱マスタ（ローカル読込）");
            root.addProperty("deathPenaltyRate", deathPenalty);
            root.addProperty("shortSellLimitRate", shortSell);
            root.addProperty("etfIntervalMinutes", etfInterval);
            root.addProperty("loanMaxAmount", loanMax);
            root.addProperty("loanAssetMultiplier", loanMultiplier);
            return root.toString();
        } catch (Exception e) {
            EconomyMod.LOGGER.error("Failed to read bundled master config on client", e);
            return null;
        }
    }

    private static String fetchSection(String section, boolean rewards) {
        return fetchSection(section, rewards, false);
    }

    private static String fetchSection(String section, boolean rewards, boolean shops) {
        return fetchSection(section, rewards, shops, false, false);
    }

    private static String fetchSection(String section, boolean rewards, boolean shops, boolean shopItems, boolean etfItems) {
        try (InputStream in = EconomyMod.class.getResourceAsStream("/data/economy/master/economy_master.json")) {
            if (in == null) {
                return null;
            }
            JsonObject manifest = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray source = manifest.getAsJsonArray(section);
            if (source == null) {
                return emptyResult();
            }
            JsonArray entries = new JsonArray();
            for (var element : source) {
                JsonObject row = element.getAsJsonObject();
                if (row.has("enabled") && !row.get("enabled").getAsBoolean()) {
                    continue;
                }
                JsonObject out = new JsonObject();
                if (shops) {
                    out.addProperty("id", row.get("id").getAsInt());
                    out.addProperty("shopName", row.get("shopName").getAsString());
                    out.addProperty("npcType", row.get("npcType").getAsString());
                    out.addProperty("npcModel", row.get("npcModel").getAsString());
                } else if (shopItems) {
                    out.addProperty("id", row.get("id").getAsInt());
                    out.addProperty("shopId", row.get("shopId").getAsInt());
                    out.addProperty("itemId", row.get("itemId").getAsInt());
                    out.addProperty("orderNo", row.get("orderNo").getAsInt());
                    if (row.has("dailyLimit") && !row.get("dailyLimit").isJsonNull()) {
                        out.addProperty("dailyLimit", row.get("dailyLimit").getAsInt());
                    }
                    if (row.has("userLimit") && !row.get("userLimit").isJsonNull()) {
                        out.addProperty("userLimit", row.get("userLimit").getAsInt());
                    }
                } else if (etfItems) {
                    out.addProperty("etfCode", row.get("etfCode").getAsString());
                    out.addProperty("itemKey", row.get("itemKey").getAsString());
                    out.addProperty("influenceWeight", row.get("influenceWeight").getAsDouble());
                } else if (rewards) {
                    out.addProperty("actionType", row.get("actionType").getAsString());
                    out.addProperty("displayName", row.get("displayName").getAsString());
                    out.addProperty("rewardAmount", row.get("rewardAmount").getAsInt());
                } else {
                    out.addProperty("id", row.get("id").getAsInt());
                    out.addProperty("name", row.get("name").getAsString());
                    out.addProperty("itemKey", row.get("itemKey").getAsString());
                    if (row.has("matchPotion") && !row.get("matchPotion").isJsonNull()) {
                        out.addProperty("matchPotion", row.get("matchPotion").getAsString());
                    }
                    if (row.has("matchEnchantment") && !row.get("matchEnchantment").isJsonNull()) {
                        out.addProperty("matchEnchantment", row.get("matchEnchantment").getAsString());
                    }
                    if (row.has("matchEnchantmentLevel") && !row.get("matchEnchantmentLevel").isJsonNull()) {
                        out.addProperty("matchEnchantmentLevel", row.get("matchEnchantmentLevel").getAsInt());
                    }
                    if (row.has("buyPrice") && !row.get("buyPrice").isJsonNull()) {
                        out.addProperty("buyPrice", row.get("buyPrice").getAsInt());
                    }
                    if (row.has("sellPrice") && !row.get("sellPrice").isJsonNull()) {
                        out.addProperty("sellPrice", row.get("sellPrice").getAsInt());
                    }
                }
                entries.add(out);
            }
            JsonObject root = new JsonObject();
            root.addProperty("source", "client-bundled");
            root.addProperty("sourceHint", "MOD同梱マスタ（ローカル読込）");
            root.addProperty("total", entries.size());
            root.add("entries", entries);
            return root.toString();
        } catch (Exception e) {
            EconomyMod.LOGGER.error("Failed to read bundled master on client", e);
            return null;
        }
    }

    private record ShopInfo(String shopName, String npcType) {
    }

    private static Map<Integer, String> buildEnabledItemNames(JsonObject manifest) {
        Map<Integer, String> names = new HashMap<>();
        JsonArray items = manifest.getAsJsonArray("items");
        if (items == null) {
            return names;
        }
        for (var element : items) {
            JsonObject row = element.getAsJsonObject();
            if (row.has("enabled") && !row.get("enabled").getAsBoolean()) {
                continue;
            }
            names.put(row.get("id").getAsInt(), row.get("name").getAsString());
        }
        return names;
    }

    private static Map<Integer, String> buildEnabledItemKeys(JsonObject manifest) {
        Map<Integer, String> keys = new HashMap<>();
        JsonArray items = manifest.getAsJsonArray("items");
        if (items == null) {
            return keys;
        }
        for (var element : items) {
            JsonObject row = element.getAsJsonObject();
            if (row.has("enabled") && !row.get("enabled").getAsBoolean()) {
                continue;
            }
            keys.put(row.get("id").getAsInt(), row.get("itemKey").getAsString());
        }
        return keys;
    }

    private static Map<String, String> buildEnabledItemNamesByKey(JsonObject manifest) {
        Map<String, String> names = new HashMap<>();
        JsonArray items = manifest.getAsJsonArray("items");
        if (items == null) {
            return names;
        }
        for (var element : items) {
            JsonObject row = element.getAsJsonObject();
            if (row.has("enabled") && !row.get("enabled").getAsBoolean()) {
                continue;
            }
            names.put(row.get("itemKey").getAsString(), row.get("name").getAsString());
        }
        return names;
    }

    private static Map<Integer, ShopInfo> buildEnabledShops(JsonObject manifest) {
        Map<Integer, ShopInfo> shops = new HashMap<>();
        JsonArray source = manifest.getAsJsonArray("shops");
        if (source == null) {
            return shops;
        }
        for (var element : source) {
            JsonObject row = element.getAsJsonObject();
            if (row.has("enabled") && !row.get("enabled").getAsBoolean()) {
                continue;
            }
            shops.put(row.get("id").getAsInt(), new ShopInfo(
                    row.get("shopName").getAsString(),
                    row.get("npcType").getAsString()
            ));
        }
        return shops;
    }

    private static String emptyResult() {
        JsonObject root = new JsonObject();
        root.addProperty("total", 0);
        root.add("entries", new JsonArray());
        return root.toString();
    }
}
