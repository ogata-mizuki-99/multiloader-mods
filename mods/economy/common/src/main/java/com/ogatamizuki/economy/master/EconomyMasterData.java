package com.ogatamizuki.economy.master;

import com.ogatamizuki.economy.EconomyCommon;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ogatamizuki.economy.EconomyItemMatcher;

/** ビルド同梱の `data/economy/master/economy_master.json` を SSOT とするマスタ。 */
public final class EconomyMasterData {
    private static final Gson GSON = new Gson();
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static EconomyMasterData instance;

    public record MasterConfigValues(
            double deathPenaltyRate,
            double shortSellLimitRate,
            int etfIntervalMinutes,
            int loanMaxAmount,
            double loanAssetMultiplier
    ) {
    }

    public static java.nio.file.Path overridePath(net.minecraft.server.MinecraftServer server) {
        return server.getServerDirectory().resolve("config/economy/economy_master.json");
    }

    public static boolean hasOverride(net.minecraft.server.MinecraftServer server) {
        return java.nio.file.Files.isRegularFile(overridePath(server));
    }

    public static void load() {
        loadBundled();
    }

    /**
     * サーバー起動時・管理GUIからの再読込。
     * {@code config/economy/economy_master.json} がなければ同梱マスタを初回エクスポートしてから読込。
     */
    public static String reload(net.minecraft.server.MinecraftServer server) {
        java.nio.file.Path override = overridePath(server);
        if (!java.nio.file.Files.isRegularFile(override)) {
            try {
                exportBundledToConfig(server);
            } catch (java.io.IOException e) {
                EconomyCommon.LOGGER.error("Failed to export default economy master", e);
                loadBundled();
                return "bundled";
            }
        }
        loadFromPath(override);
        return override.toAbsolutePath().toString();
    }

    /** 同梱マスタを {@code config/economy/economy_master.json} に書き出す（既存ファイルは上書き）。 */
    public static void exportBundledToConfig(net.minecraft.server.MinecraftServer server) throws java.io.IOException {
        JsonObject manifest = readBundledManifest();
        manifest.remove("source");
        java.nio.file.Path override = overridePath(server);
        java.nio.file.Files.createDirectories(override.getParent());
        java.nio.file.Files.writeString(override, PRETTY_GSON.toJson(manifest), StandardCharsets.UTF_8);
        EconomyCommon.LOGGER.info("Economy master exported to {}", override.toAbsolutePath());
    }

    public static MasterConfigValues currentConfigValues() {
        EconomyMasterData master = get();
        LoanDebtLimitConfig loan = master.loanDebtLimit();
        return new MasterConfigValues(
                master.deathPenaltyRate(),
                master.shortSellLimitRate(),
                master.etfRandomWalkIntervalMinutes(),
                loan.maxAmount(),
                loan.assetMultiplier()
        );
    }

    public static String saveConfigOverride(net.minecraft.server.MinecraftServer server, MasterConfigValues values) throws java.io.IOException {
        JsonObject manifest = readEditableManifest(server);
        applyConfigValues(manifest, values);
        return writeManifest(server, manifest);
    }

    public static String resetConfigOverride(net.minecraft.server.MinecraftServer server) throws java.io.IOException {
        java.nio.file.Path override = overridePath(server);
        if (java.nio.file.Files.isRegularFile(override)) {
            java.nio.file.Files.delete(override);
        }
        exportBundledToConfig(server);
        loadFromPath(override);
        return override.toAbsolutePath().toString();
    }

    public static JsonObject readEditableManifest(net.minecraft.server.MinecraftServer server) throws java.io.IOException {
        java.nio.file.Path override = overridePath(server);
        if (java.nio.file.Files.isRegularFile(override)) {
            return GSON.fromJson(java.nio.file.Files.readString(override, StandardCharsets.UTF_8), JsonObject.class);
        }
        return readBundledManifest();
    }

    public static String writeManifest(net.minecraft.server.MinecraftServer server, JsonObject manifest) throws java.io.IOException {
        manifest.remove("source");
        java.nio.file.Path override = overridePath(server);
        java.nio.file.Files.createDirectories(override.getParent());
        java.nio.file.Files.writeString(override, PRETTY_GSON.toJson(manifest), StandardCharsets.UTF_8);
        return reload(server);
    }

    public static void ensureLoaded(net.minecraft.server.MinecraftServer server) {
        EconomyMasterData data = get();
        if (data.allActionRewards().isEmpty() || data.allEnabledItems().isEmpty()) {
            reload(server);
        }
    }

    public static com.google.gson.JsonObject fetchAllActionRewards() {
        JsonArray entries = new JsonArray();
        for (ActionRewardDef reward : get().allActionRewards()) {
            JsonObject out = new JsonObject();
            out.addProperty("actionType", reward.actionType());
            out.addProperty("displayName", reward.displayName());
            out.addProperty("rewardAmount", reward.rewardAmount());
            entries.add(out);
        }
        JsonObject root = new JsonObject();
        root.addProperty("total", entries.size());
        root.add("entries", entries);
        return root;
    }

    public static com.google.gson.JsonObject fetchAllItems() {
        JsonArray entries = new JsonArray();
        for (ItemDef item : get().allEnabledItems()) {
            entries.add(itemDefToAdminJson(item));
        }
        JsonObject root = new JsonObject();
        root.addProperty("total", entries.size());
        root.add("entries", entries);
        return root;
    }

    public static com.google.gson.JsonObject fetchAllShopItems() {
        EconomyMasterData master = get();
        JsonArray entries = new JsonArray();
        for (ShopItemDef row : master.shopItems.values().stream()
                .sorted(Comparator.comparingInt(ShopItemDef::id))
                .toList()) {
            JsonObject out = new JsonObject();
            out.addProperty("id", row.id());
            out.addProperty("shopId", row.shopId());
            out.addProperty("itemId", row.itemId());
            out.addProperty("orderNo", row.orderNo());
            ItemDef item = master.items.get(row.itemId());
            if (item != null) {
                out.addProperty("itemName", item.name());
                out.addProperty("itemKey", item.itemKey());
            }
            ShopDef shop = master.shops.get(row.shopId());
            if (shop != null) {
                out.addProperty("shopName", shop.shopName());
                out.addProperty("npcType", shop.npcType());
            }
            if (row.dailyLimit() != null) {
                out.addProperty("dailyLimit", row.dailyLimit());
            }
            if (row.userLimit() != null) {
                out.addProperty("userLimit", row.userLimit());
            }
            entries.add(out);
        }
        JsonObject root = new JsonObject();
        root.addProperty("total", entries.size());
        root.add("entries", entries);
        return root;
    }

    public static com.google.gson.JsonObject fetchAllEtfItems() {
        EconomyMasterData master = get();
        JsonArray entries = new JsonArray();
        for (Map.Entry<String, List<EtfItemDef>> entry : master.etfItemsByCode.entrySet()) {
            for (EtfItemDef row : entry.getValue()) {
                JsonObject out = new JsonObject();
                out.addProperty("etfCode", row.etfCode());
                out.addProperty("itemKey", row.itemKey());
                out.addProperty("influenceWeight", row.influenceWeight());
                master.itemByKey(row.itemKey()).ifPresent(item -> out.addProperty("itemName", item.name()));
                entries.add(out);
            }
        }
        JsonObject root = new JsonObject();
        root.addProperty("total", entries.size());
        root.add("entries", entries);
        return root;
    }

    public static com.google.gson.JsonObject fetchActionRewardsPage(int page, int pageSize) {
        JsonArray entries = new JsonArray();
        for (ActionRewardDef reward : get().allActionRewards()) {
            JsonObject out = new JsonObject();
            out.addProperty("actionType", reward.actionType());
            out.addProperty("displayName", reward.displayName());
            out.addProperty("rewardAmount", reward.rewardAmount());
            entries.add(out);
        }
        return buildPageFromArray(entries, page, pageSize);
    }

    public static com.google.gson.JsonObject fetchItemsPage(int page, int pageSize) {
        JsonArray entries = new JsonArray();
        for (ItemDef item : get().allEnabledItems()) {
            entries.add(itemDefToAdminJson(item));
        }
        return buildPageFromArray(entries, page, pageSize);
    }

    private static JsonObject itemDefToAdminJson(ItemDef item) {
        JsonObject out = new JsonObject();
        out.addProperty("id", item.id());
        out.addProperty("name", item.name());
        out.addProperty("itemKey", item.itemKey());
        if (item.matchPotion() != null) {
            out.addProperty("matchPotion", item.matchPotion());
        }
        if (item.matchEnchantment() != null) {
            out.addProperty("matchEnchantment", item.matchEnchantment());
        }
        if (item.matchEnchantmentLevel() != null) {
            out.addProperty("matchEnchantmentLevel", item.matchEnchantmentLevel());
        }
        if (item.buyPrice() != null) {
            out.addProperty("buyPrice", item.buyPrice());
        }
        if (item.sellPrice() != null) {
            out.addProperty("sellPrice", item.sellPrice());
        }
        return out;
    }

    public static String applyActionRewardEdits(net.minecraft.server.MinecraftServer server, JsonArray edits) throws java.io.IOException {
        JsonObject manifest = readEditableManifest(server);
        JsonArray rewards = requireArray(manifest, "actionRewards");
        for (JsonElement editElement : edits) {
            JsonObject edit = editElement.getAsJsonObject();
            if (isCreateEdit(edit)) {
                createActionReward(rewards, edit);
                continue;
            }
            String actionType = edit.get("actionType").getAsString();
            int rewardAmount = edit.get("rewardAmount").getAsInt();
            boolean found = false;
            for (JsonElement element : rewards) {
                JsonObject reward = element.getAsJsonObject();
                if (actionType.equals(reward.get("actionType").getAsString())) {
                    reward.addProperty("rewardAmount", rewardAmount);
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("Unknown action reward: " + actionType);
            }
        }
        return writeManifest(server, manifest);
    }

    public static String applyItemPriceEdits(net.minecraft.server.MinecraftServer server, JsonArray edits) throws java.io.IOException {
        JsonObject manifest = readEditableManifest(server);
        JsonArray items = requireArray(manifest, "items");
        for (JsonElement editElement : edits) {
            JsonObject edit = editElement.getAsJsonObject();
            if (isCreateEdit(edit)) {
                createItem(items, edit);
                continue;
            }
            int itemId = edit.get("id").getAsInt();
            boolean found = false;
            for (JsonElement element : items) {
                JsonObject item = element.getAsJsonObject();
                if (item.get("id").getAsInt() != itemId) {
                    continue;
                }
                applyItemPrices(item, edit);
                found = true;
                break;
            }
            if (!found) {
                throw new IllegalArgumentException("Unknown item id: " + itemId);
            }
        }
        return writeManifest(server, manifest);
    }

    public static String applyShopItemEdits(net.minecraft.server.MinecraftServer server, JsonArray edits) throws java.io.IOException {
        JsonObject manifest = readEditableManifest(server);
        JsonArray shopItems = requireArray(manifest, "shopItems");
        for (JsonElement editElement : edits) {
            JsonObject edit = editElement.getAsJsonObject();
            if (isCreateEdit(edit)) {
                createShopItem(manifest, shopItems, edit);
                continue;
            }
            int id = edit.get("id").getAsInt();
            boolean found = false;
            for (JsonElement element : shopItems) {
                JsonObject row = element.getAsJsonObject();
                if (row.get("id").getAsInt() != id) {
                    continue;
                }
                applyShopItemFields(manifest, row, edit);
                found = true;
                break;
            }
            if (!found) {
                throw new IllegalArgumentException("Unknown shopItem id: " + id);
            }
        }
        return writeManifest(server, manifest);
    }

    public static String applyEtfItemEdits(net.minecraft.server.MinecraftServer server, JsonArray edits) throws java.io.IOException {
        JsonObject manifest = readEditableManifest(server);
        JsonArray etfItems = requireArray(manifest, "etfItems");
        for (JsonElement editElement : edits) {
            JsonObject edit = editElement.getAsJsonObject();
            if (isCreateEdit(edit)) {
                createEtfItem(etfItems, edit);
                continue;
            }
            String etfCode = edit.get("etfCode").getAsString();
            String itemKey = edit.get("itemKey").getAsString();
            boolean found = false;
            for (JsonElement element : etfItems) {
                JsonObject row = element.getAsJsonObject();
                if (!etfCode.equals(row.get("etfCode").getAsString())) {
                    continue;
                }
                if (!itemKey.equals(row.get("itemKey").getAsString())) {
                    continue;
                }
                row.addProperty("influenceWeight", edit.get("influenceWeight").getAsDouble());
                found = true;
                break;
            }
            if (!found) {
                throw new IllegalArgumentException("Unknown etfItem: " + etfCode + "/" + itemKey);
            }
        }
        return writeManifest(server, manifest);
    }

    static void applyManifestEdits(JsonObject manifest, JsonArray edits, String section) {
        switch (section) {
            case "actionRewards" -> {
                JsonArray rewards = requireArray(manifest, "actionRewards");
                for (JsonElement editElement : edits) {
                    JsonObject edit = editElement.getAsJsonObject();
                    if (isCreateEdit(edit)) {
                        createActionReward(rewards, edit);
                    }
                }
            }
            case "items" -> {
                JsonArray items = requireArray(manifest, "items");
                for (JsonElement editElement : edits) {
                    JsonObject edit = editElement.getAsJsonObject();
                    if (isCreateEdit(edit)) {
                        createItem(items, edit);
                    }
                }
            }
            case "shopItems" -> {
                JsonArray shopItems = requireArray(manifest, "shopItems");
                for (JsonElement editElement : edits) {
                    JsonObject edit = editElement.getAsJsonObject();
                    if (isCreateEdit(edit)) {
                        createShopItem(manifest, shopItems, edit);
                    }
                }
            }
            case "etfItems" -> {
                JsonArray etfItems = requireArray(manifest, "etfItems");
                for (JsonElement editElement : edits) {
                    JsonObject edit = editElement.getAsJsonObject();
                    if (isCreateEdit(edit)) {
                        createEtfItem(etfItems, edit);
                    }
                }
            }
            default -> throw new IllegalArgumentException("Unknown section: " + section);
        }
    }

    private static boolean isCreateEdit(JsonObject edit) {
        return edit.has("create") && edit.get("create").getAsBoolean();
    }

    private static JsonArray requireArray(JsonObject manifest, String key) {
        JsonArray array = manifest.getAsJsonArray(key);
        if (array == null) {
            throw new IllegalStateException(key + " missing in economy master");
        }
        return array;
    }

    private static void createActionReward(JsonArray rewards, JsonObject edit) {
        String actionType = requireNonBlank(edit, "actionType");
        if (findActionReward(rewards, actionType) != null) {
            throw new IllegalArgumentException("Duplicate action reward: " + actionType);
        }
        JsonObject row = new JsonObject();
        row.addProperty("actionType", actionType);
        row.addProperty("displayName", requireNonBlank(edit, "displayName"));
        row.addProperty("rewardAmount", edit.get("rewardAmount").getAsInt());
        row.addProperty("enabled", true);
        rewards.add(row);
    }

    private static void createItem(JsonArray items, JsonObject edit) {
        int id = edit.has("id") && !edit.get("id").isJsonNull()
                ? edit.get("id").getAsInt()
                : nextIntId(items, "id");
        if (findByIntId(items, "id", id) != null) {
            throw new IllegalArgumentException("Duplicate item id: " + id);
        }
        String itemKey = requireNonBlank(edit, "itemKey");
        String matchPotion = optionalBlank(edit, "matchPotion");
        String matchEnchantment = optionalBlank(edit, "matchEnchantment");
        Integer matchEnchantmentLevel = edit.has("matchEnchantmentLevel") && !edit.get("matchEnchantmentLevel").isJsonNull()
                ? edit.get("matchEnchantmentLevel").getAsInt()
                : null;
        if (findItemByMatchIdentity(items, itemKey, matchPotion, matchEnchantment, matchEnchantmentLevel) != null) {
            throw new IllegalArgumentException("Duplicate item identity: " + itemKey);
        }
        JsonObject row = new JsonObject();
        row.addProperty("id", id);
        row.addProperty("name", requireNonBlank(edit, "name"));
        row.addProperty("unit", edit.has("unit") && !edit.get("unit").isJsonNull()
                ? edit.get("unit").getAsString()
                : "個");
        row.addProperty("itemKey", itemKey);
        if (matchPotion != null) {
            row.addProperty("matchPotion", matchPotion);
        }
        if (matchEnchantment != null) {
            row.addProperty("matchEnchantment", matchEnchantment);
        }
        if (matchEnchantmentLevel != null) {
            row.addProperty("matchEnchantmentLevel", matchEnchantmentLevel);
        }
        row.addProperty("enabled", true);
        applyItemPrices(row, edit);
        items.add(row);
    }

    private static void createShopItem(JsonObject manifest, JsonArray shopItems, JsonObject edit) {
        int id = edit.has("id") && !edit.get("id").isJsonNull()
                ? edit.get("id").getAsInt()
                : nextIntId(shopItems, "id");
        if (findByIntId(shopItems, "id", id) != null) {
            throw new IllegalArgumentException("Duplicate shopItem id: " + id);
        }
        int shopId = edit.get("shopId").getAsInt();
        if (!isEnabledShop(manifest, shopId)) {
            throw new IllegalArgumentException("Unknown or disabled shop id: " + shopId);
        }
        int itemId = edit.get("itemId").getAsInt();
        if (findByIntId(requireArray(manifest, "items"), "id", itemId) == null) {
            throw new IllegalArgumentException("Unknown item id: " + itemId);
        }
        int orderNo = edit.has("orderNo") && !edit.get("orderNo").isJsonNull()
                ? edit.get("orderNo").getAsInt()
                : nextShopOrderNo(shopItems, shopId);
        JsonObject row = new JsonObject();
        row.addProperty("id", id);
        row.addProperty("shopId", shopId);
        row.addProperty("itemId", itemId);
        row.addProperty("orderNo", orderNo);
        row.addProperty("enabled", true);
        applyShopItemFields(manifest, row, edit);
        shopItems.add(row);
    }

    private static void createEtfItem(JsonArray etfItems, JsonObject edit) {
        String etfCode = requireNonBlank(edit, "etfCode");
        String itemKey = requireNonBlank(edit, "itemKey");
        if (findEtfItem(etfItems, etfCode, itemKey) != null) {
            throw new IllegalArgumentException("Duplicate etfItem: " + etfCode + "/" + itemKey);
        }
        JsonObject row = new JsonObject();
        row.addProperty("etfCode", etfCode);
        row.addProperty("itemKey", itemKey);
        row.addProperty("influenceWeight", edit.get("influenceWeight").getAsDouble());
        row.addProperty("enabled", true);
        etfItems.add(row);
    }

    private static void applyItemPrices(JsonObject item, JsonObject edit) {
        if (edit.has("buyPrice") && !edit.get("buyPrice").isJsonNull()) {
            item.addProperty("buyPrice", edit.get("buyPrice").getAsInt());
        } else {
            item.remove("buyPrice");
        }
        if (edit.has("sellPrice") && !edit.get("sellPrice").isJsonNull()) {
            item.addProperty("sellPrice", edit.get("sellPrice").getAsInt());
        } else {
            item.remove("sellPrice");
        }
    }

    private static void applyShopItemFields(JsonObject manifest, JsonObject row, JsonObject edit) {
        if (edit.has("shopId")) {
            int shopId = edit.get("shopId").getAsInt();
            if (!isEnabledShop(manifest, shopId)) {
                throw new IllegalArgumentException("Unknown or disabled shop id: " + shopId);
            }
            row.addProperty("shopId", shopId);
        }
        if (edit.has("dailyLimit") && !edit.get("dailyLimit").isJsonNull()) {
            row.addProperty("dailyLimit", edit.get("dailyLimit").getAsInt());
        } else if (edit.has("dailyLimit")) {
            row.add("dailyLimit", null);
        }
        if (edit.has("userLimit") && !edit.get("userLimit").isJsonNull()) {
            row.addProperty("userLimit", edit.get("userLimit").getAsInt());
        } else if (edit.has("userLimit")) {
            row.add("userLimit", null);
        }
    }

    private static int nextIntId(JsonArray array, String idField) {
        int max = 0;
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has(idField)) {
                max = Math.max(max, obj.get(idField).getAsInt());
            }
        }
        return max + 1;
    }

    private static int nextShopOrderNo(JsonArray shopItems, int shopId) {
        int max = 0;
        for (JsonElement element : shopItems) {
            JsonObject row = element.getAsJsonObject();
            if (row.get("shopId").getAsInt() == shopId) {
                max = Math.max(max, row.get("orderNo").getAsInt());
            }
        }
        return max + 1;
    }

    private static JsonObject findActionReward(JsonArray rewards, String actionType) {
        for (JsonElement element : rewards) {
            JsonObject reward = element.getAsJsonObject();
            if (actionType.equals(reward.get("actionType").getAsString())) {
                return reward;
            }
        }
        return null;
    }

    private static JsonObject findByIntId(JsonArray array, String idField, int id) {
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has(idField) && obj.get(idField).getAsInt() == id) {
                return obj;
            }
        }
        return null;
    }


    private static JsonObject findItemByMatchIdentity(
            JsonArray items,
            String itemKey,
            String matchPotion,
            String matchEnchantment,
            Integer matchEnchantmentLevel
    ) {
        String target = EconomyItemMatcher.matchIdentityKey(itemKey, matchPotion, matchEnchantment, matchEnchantmentLevel);
        for (JsonElement element : items) {
            JsonObject item = element.getAsJsonObject();
            String potion = optionalBlank(item, "matchPotion");
            String enchant = optionalBlank(item, "matchEnchantment");
            Integer level = item.has("matchEnchantmentLevel") && !item.get("matchEnchantmentLevel").isJsonNull()
                    ? item.get("matchEnchantmentLevel").getAsInt()
                    : null;
            String identity = EconomyItemMatcher.matchIdentityKey(
                    item.get("itemKey").getAsString(), potion, enchant, level);
            if (target.equals(identity)) {
                return item;
            }
        }
        return null;
    }


    private static String optionalBlank(JsonObject obj, String field) {
        if (!obj.has(field) || obj.get(field).isJsonNull()) {
            return null;
        }
        String value = obj.get(field).getAsString();
        return value == null || value.isBlank() ? null : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static JsonObject findEtfItem(JsonArray etfItems, String etfCode, String itemKey) {
        for (JsonElement element : etfItems) {
            JsonObject row = element.getAsJsonObject();
            if (etfCode.equals(row.get("etfCode").getAsString())
                    && itemKey.equals(row.get("itemKey").getAsString())) {
                return row;
            }
        }
        return null;
    }

    private static String requireNonBlank(JsonObject edit, String field) {
        if (!edit.has(field) || edit.get(field).isJsonNull()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String value = edit.get(field).getAsString();
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static boolean isEnabledShop(JsonObject manifest, int shopId) {
        JsonArray shops = manifest.getAsJsonArray("shops");
        if (shops == null) {
            return false;
        }
        for (JsonElement element : shops) {
            JsonObject shop = element.getAsJsonObject();
            if (shop.get("id").getAsInt() == shopId) {
                return !shop.has("enabled") || shop.get("enabled").getAsBoolean();
            }
        }
        return false;
    }

    private static JsonObject buildPageFromArray(JsonArray enabled, int page, int pageSize) {
        int total = enabled.size();
        int pageCount = Math.max(1, (total + pageSize - 1) / pageSize);
        int requested = Math.max(0, page);
        JsonArray entries = new JsonArray();
        if (requested < pageCount && total > 0) {
            int start = requested * pageSize;
            int end = Math.min(start + pageSize, total);
            for (int i = start; i < end; i++) {
                entries.add(enabled.get(i));
            }
        }
        JsonObject root = new JsonObject();
        root.addProperty("page", requested);
        root.addProperty("pageCount", pageCount);
        root.addProperty("total", total);
        root.add("entries", entries);
        return root;
    }

    private static JsonObject readBundledManifest() throws java.io.IOException {
        try (InputStream in = EconomyCommon.class.getResourceAsStream("/data/economy/master/economy_master.json")) {
            if (in == null) {
                throw new IllegalStateException("economy_master.json not found in mod resources");
            }
            return GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
        }
    }

    private static void applyConfigValues(JsonObject manifest, MasterConfigValues values) {
        JsonArray config = manifest.getAsJsonArray("config");
        if (config == null) {
            config = new JsonArray();
            manifest.add("config", config);
        }
        upsertConfigEntry(config, 1, "DEATH_PENALTY_RATE", deathPenaltyValue(values.deathPenaltyRate()));
        upsertConfigEntry(config, 2, "SHORT_SELL_LIMIT_RATE", shortSellValue(values.shortSellLimitRate()));
        upsertConfigEntry(config, 3, "ETF_RANDOM_WALK_INTERVAL", etfIntervalValue(values.etfIntervalMinutes()));
        upsertConfigEntry(config, 4, "LOAN_DEBT_LIMIT", loanLimitValue(values.loanMaxAmount(), values.loanAssetMultiplier()));
    }

    private static void upsertConfigEntry(JsonArray config, int id, String actionType, JsonObject value) {
        for (int i = 0; i < config.size(); i++) {
            JsonObject entry = config.get(i).getAsJsonObject();
            if (actionType.equals(entry.get("actionType").getAsString())) {
                entry.add("value", value);
                entry.addProperty("enabled", true);
                return;
            }
        }
        JsonObject entry = new JsonObject();
        entry.addProperty("id", id);
        entry.addProperty("actionType", actionType);
        entry.addProperty("enabled", true);
        entry.add("value", value);
        config.add(entry);
    }

    private static JsonObject deathPenaltyValue(double rate) {
        JsonObject value = new JsonObject();
        value.addProperty("money", -Math.abs(rate));
        return value;
    }

    private static JsonObject shortSellValue(double rate) {
        JsonObject value = new JsonObject();
        value.addProperty("rate", rate);
        return value;
    }

    private static JsonObject etfIntervalValue(int minutes) {
        JsonObject value = new JsonObject();
        value.addProperty("minutes", minutes);
        return value;
    }

    private static JsonObject loanLimitValue(int maxAmount, double assetMultiplier) {
        JsonObject value = new JsonObject();
        value.addProperty("max_amount", maxAmount);
        value.addProperty("asset_multiplier", assetMultiplier);
        return value;
    }

    private static void loadBundled() {
        try (InputStream in = EconomyCommon.class.getResourceAsStream("/data/economy/master/economy_master.json")) {
            if (in == null) {
                throw new IllegalStateException("economy_master.json not found in mod resources");
            }
            parseAndApply(GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), RawManifest.class), "bundled");
        } catch (Exception e) {
            instance = new EconomyMasterData(new RawManifest());
            EconomyCommon.LOGGER.error("Failed to load bundled economy master data", e);
        }
    }

    private static void loadFromPath(java.nio.file.Path path) {
        try (InputStream in = java.nio.file.Files.newInputStream(path)) {
            parseAndApply(GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), RawManifest.class), path.toString());
        } catch (Exception e) {
            EconomyCommon.LOGGER.error("Failed to load economy master from {}", path, e);
            loadBundled();
        }
    }

    private static void parseAndApply(RawManifest raw, String sourceLabel) {
        instance = new EconomyMasterData(raw);
        EconomyCommon.LOGGER.info(
                "Economy master loaded from {}: {} shops, {} items, {} shop-items, {} action rewards, {} etf items, {} configs",
                sourceLabel,
                instance.shops.size(),
                instance.items.size(),
                instance.shopItems.size(),
                instance.actionRewards.size(),
                instance.etfItemsByItemKey.values().stream().mapToInt(List::size).sum(),
                instance.configValues.size()
        );
    }

    public static EconomyMasterData get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public record ShopDef(int id, String shopName, String npcType, String npcModel, boolean enabled) {
    }

    public record ItemDef(
            int id,
            String name,
            String unit,
            String itemKey,
            Integer buyPrice,
            Integer sellPrice,
            boolean enabled,
            String matchPotion,
            String matchEnchantment,
            Integer matchEnchantmentLevel
    ) {
        public boolean hasVariantMatch() {
            return (matchPotion != null && !matchPotion.isBlank())
                    || (matchEnchantment != null && !matchEnchantment.isBlank());
        }
    }

    public record ShopItemDef(int id, int shopId, int orderNo, int itemId, Integer dailyLimit, Integer userLimit, boolean enabled) {
    }

    public record ActionRewardDef(String actionType, int rewardAmount, String displayName, boolean enabled) {
    }

    public record EtfItemDef(String etfCode, String itemKey, double influenceWeight) {
    }

    public record LoanDebtLimitConfig(int maxAmount, double assetMultiplier) {
    }

    private final Map<Integer, ShopDef> shops = new HashMap<>();
    private final Map<Integer, ItemDef> items = new HashMap<>();
    private final Map<Integer, ShopItemDef> shopItems = new HashMap<>();
    private final Map<String, ActionRewardDef> actionRewards = new HashMap<>();
    private final Map<Integer, List<ShopItemDef>> shopItemsByShopId = new HashMap<>();
    private final Map<String, List<EtfItemDef>> etfItemsByCode = new HashMap<>();
    private final Map<String, List<EtfItemDef>> etfItemsByItemKey = new HashMap<>();
    private final Map<String, com.google.gson.JsonObject> configValues = new HashMap<>();

    private EconomyMasterData(RawManifest raw) {
        if (raw.shops != null) {
            for (RawShop s : raw.shops) {
                if (isEnabled(s.enabled)) {
                    shops.put(s.id, new ShopDef(s.id, s.shopName, s.npcType, s.npcModel, true));
                }
            }
        }
        if (raw.items != null) {
            for (RawItem i : raw.items) {
                if (isEnabled(i.enabled)) {
                    items.put(i.id, new ItemDef(
                            i.id,
                            i.name,
                            i.unit,
                            i.itemKey,
                            i.buyPrice,
                            i.sellPrice,
                            true,
                            blankToNull(i.matchPotion),
                            blankToNull(i.matchEnchantment),
                            i.matchEnchantmentLevel
                    ));
                }
            }
        }
        if (raw.shopItems != null) {
            for (RawShopItem si : raw.shopItems) {
                if (isEnabled(si.enabled) && items.containsKey(si.itemId) && shops.containsKey(si.shopId)) {
                    ShopItemDef def = new ShopItemDef(si.id, si.shopId, si.orderNo, si.itemId, si.dailyLimit, si.userLimit, true);
                    shopItems.put(si.id, def);
                    shopItemsByShopId.computeIfAbsent(si.shopId, k -> new ArrayList<>()).add(def);
                }
            }
            shopItemsByShopId.values().forEach(list -> list.sort(Comparator.comparingInt(ShopItemDef::orderNo)));
        }
        if (raw.actionRewards != null) {
            for (RawActionReward ar : raw.actionRewards) {
                if (isEnabled(ar.enabled)) {
                    actionRewards.put(ar.actionType, new ActionRewardDef(ar.actionType, ar.rewardAmount, ar.displayName, true));
                }
            }
        }
        if (raw.etfItems != null) {
            for (RawEtfItem ei : raw.etfItems) {
                if (isEnabled(ei.enabled)) {
                    EtfItemDef def = new EtfItemDef(ei.etfCode, ei.itemKey, ei.influenceWeight);
                    etfItemsByCode.computeIfAbsent(ei.etfCode, k -> new ArrayList<>()).add(def);
                    etfItemsByItemKey.computeIfAbsent(ei.itemKey, k -> new ArrayList<>()).add(def);
                }
            }
        }
        if (raw.config != null) {
            for (RawConfig cfg : raw.config) {
                if (isEnabled(cfg.enabled) && cfg.actionType != null && cfg.value != null) {
                    configValues.put(cfg.actionType, cfg.value);
                }
            }
        }
    }

    /** Gson の boolean 欠落は false になるため、未指定は有効扱い。 */
    private static boolean isEnabled(Boolean enabled) {
        return enabled == null || enabled;
    }

    public Optional<ShopDef> shop(int shopId) {
        return Optional.ofNullable(shops.get(shopId));
    }

    public Optional<ItemDef> item(int itemId) {
        return Optional.ofNullable(items.get(itemId));
    }

    public Optional<ItemDef> itemByKey(String itemKey) {
        // match なし行を優先（購入・ETF 等の単純 itemKey 解決用）
        Optional<ItemDef> plain = items.values().stream()
                .filter(i -> i.itemKey().equals(itemKey) && !i.hasVariantMatch())
                .findFirst();
        if (plain.isPresent()) {
            return plain;
        }
        return items.values().stream().filter(i -> i.itemKey().equals(itemKey)).findFirst();
    }

    public Optional<ShopItemDef> shopItem(int shopItemId) {
        return Optional.ofNullable(shopItems.get(shopItemId));
    }

    public List<ItemDef> allEnabledItems() {
        return items.values().stream().sorted(Comparator.comparingInt(ItemDef::id)).toList();
    }

    public List<ActionRewardDef> allActionRewards() {
        return actionRewards.values().stream()
                .sorted(Comparator.comparing(ActionRewardDef::actionType))
                .toList();
    }

    public List<ShopDef> allEnabledShops() {
        return shops.values().stream()
                .filter(ShopDef::enabled)
                .sorted(Comparator.comparingInt(ShopDef::id))
                .toList();
    }

    public static com.google.gson.JsonObject fetchAllShops() {
        JsonArray entries = new JsonArray();
        for (ShopDef shop : get().allEnabledShops()) {
            JsonObject out = new JsonObject();
            out.addProperty("id", shop.id());
            out.addProperty("shopName", shop.shopName());
            out.addProperty("npcType", shop.npcType());
            out.addProperty("npcModel", shop.npcModel());
            entries.add(out);
        }
        JsonObject root = new JsonObject();
        root.addProperty("total", entries.size());
        root.add("entries", entries);
        return root;
    }

    public List<ShopItemDef> shopItemsForShop(int shopId) {
        return List.copyOf(shopItemsByShopId.getOrDefault(shopId, List.of()));
    }

    public Optional<ActionRewardDef> actionReward(String actionType) {
        return Optional.ofNullable(actionRewards.get(actionType));
    }

    public List<EtfItemDef> etfItemsForCode(String etfCode) {
        return List.copyOf(etfItemsByCode.getOrDefault(etfCode, List.of()));
    }

    public List<EtfItemDef> etfItemsForItemKey(String itemKey) {
        return List.copyOf(etfItemsByItemKey.getOrDefault(itemKey, List.of()));
    }

    public double deathPenaltyRate() {
        com.google.gson.JsonObject value = configValues.get("DEATH_PENALTY_RATE");
        if (value != null && value.has("money")) {
            return Math.abs(value.get("money").getAsDouble());
        }
        return EconomyBalanceDefaults.DEATH_PENALTY_RATE;
    }

    public double shortSellLimitRate() {
        com.google.gson.JsonObject value = configValues.get("SHORT_SELL_LIMIT_RATE");
        if (value != null && value.has("rate")) {
            return value.get("rate").getAsDouble();
        }
        return EconomyBalanceDefaults.SHORT_SELL_LIMIT_RATE;
    }

    public int etfRandomWalkIntervalMinutes() {
        com.google.gson.JsonObject value = configValues.get("ETF_RANDOM_WALK_INTERVAL");
        if (value != null && value.has("minutes")) {
            return value.get("minutes").getAsInt();
        }
        return EconomyBalanceDefaults.ETF_RANDOM_WALK_INTERVAL_MINUTES;
    }

    public LoanDebtLimitConfig loanDebtLimit() {
        com.google.gson.JsonObject value = configValues.get("LOAN_DEBT_LIMIT");
        int maxAmount = EconomyBalanceDefaults.LOAN_MAX_AMOUNT;
        double assetMultiplier = EconomyBalanceDefaults.LOAN_ASSET_MULTIPLIER;
        if (value != null) {
            if (value.has("max_amount")) {
                maxAmount = value.get("max_amount").getAsInt();
            }
            if (value.has("asset_multiplier")) {
                assetMultiplier = value.get("asset_multiplier").getAsDouble();
            }
        }
        return new LoanDebtLimitConfig(maxAmount, assetMultiplier);
    }

    private static final class RawManifest {
        List<RawShop> shops;
        List<RawItem> items;
        List<RawShopItem> shopItems;
        List<RawActionReward> actionRewards;
        List<RawEtfItem> etfItems;
        List<RawConfig> config;
    }

    private static final class RawShop {
        int id;
        String shopName;
        String npcType;
        String npcModel;
        Boolean enabled;
    }

    private static final class RawItem {
        int id;
        String name;
        String unit;
        String itemKey;
        Integer buyPrice;
        Integer sellPrice;
        Boolean enabled;
        String matchPotion;
        String matchEnchantment;
        Integer matchEnchantmentLevel;
    }

    private static final class RawShopItem {
        int id;
        int shopId;
        int orderNo;
        int itemId;
        Integer dailyLimit;
        Integer userLimit;
        Boolean enabled;
    }

    private static final class RawActionReward {
        String actionType;
        int rewardAmount;
        String displayName;
        Boolean enabled;
    }

    private static final class RawEtfItem {
        String etfCode;
        String itemKey;
        double influenceWeight;
        Boolean enabled;
    }

    private static final class RawConfig {
        String actionType;
        com.google.gson.JsonObject value;
        Boolean enabled;
    }
}
