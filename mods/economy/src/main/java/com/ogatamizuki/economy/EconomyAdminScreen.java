package com.ogatamizuki.economy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ogatamizuki.economy.master.EconomyMasterClientData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** 経済管理ブロックGUI（参加ユーザー残高閲覧 + マスタ設定 + 管理操作）。 */
public class EconomyAdminScreen extends Screen {
    private static final NumberFormat YEN_FORMAT = NumberFormat.getNumberInstance(Locale.JAPAN);
    private static final int PLAYERS_PER_PAGE = 3;
    private static final int PANEL_WIDTH = 360;
    private static final int COL_GAP = 8;
    private static final int ROW_HEIGHT = 20;
    private static final int SCREEN_MARGIN = 12;
    private static final int MASTER_ROW_HEIGHT = 22;
    private static final int MASTER_FIELD_COUNT = 5;
    private static final int MASTER_LABEL_WIDTH = 108;
    private static final int MASTER_LIST_ROW_HEIGHT = 20;
    private static final int MASTER_LIST_VISIBLE_ROWS = 5;
    private static final int MASTER_BALANCE_VISIBLE_ROWS = 3;
    private static final int MASTER_SCROLL_STEP = 1;
    private static final int ADMIN_VISIBLE_ROWS = 5;
    private static final int ADMIN_SCROLL_STEP = 1;
    private static final int FOOTER_BOTTOM_MARGIN = 8;
    private static final int FOOTER_CLOSE_HEIGHT = 18;
    private static final int FOOTER_ROW_GAP = 8;
    private static final int FOOTER_ACTION_HEIGHT = 18;
    private static final int MAIN_TAB_TOP_OFFSET = 16;
    private static final int MAIN_TAB_HEIGHT = 18;
    private static final int MASTER_SUB_TAB_TOP_OFFSET = MAIN_TAB_TOP_OFFSET + MAIN_TAB_HEIGHT + 6;
    private static final int MASTER_HINT_TOP_OFFSET = MASTER_SUB_TAB_TOP_OFFSET + MAIN_TAB_HEIGHT + 8;
    private static final int MASTER_CONTENT_TOP_OFFSET = MASTER_HINT_TOP_OFFSET + 14;
    private static final int MASTER_LIST_HEADER_HEIGHT = 12;
    private static final int MASTER_LIST_FIELD_Y_INSET = 2;
    private static final int SHOP_COL_ITEM_MAX_CHARS = 9;
    private static final int SHOP_COL_NPC = 84;
    private static final int SHOP_NPC_ARROW_WIDTH = 14;
    private static final int SHOP_NPC_INNER_WIDTH = 88;
    private static final int SHOP_COL_NPC_LABEL = SHOP_COL_NPC + SHOP_NPC_ARROW_WIDTH + 1;
    private static final int SHOP_COL_NPC_NEXT = SHOP_COL_NPC + SHOP_NPC_ARROW_WIDTH + SHOP_NPC_INNER_WIDTH;
    private static final int SHOP_FIELD_WIDTH = 36;
    private static final int SHOP_COL_DAILY = SHOP_COL_NPC_NEXT + SHOP_NPC_ARROW_WIDTH + 10;
    private static final int SHOP_COL_USER = SHOP_COL_DAILY + SHOP_FIELD_WIDTH + 8;

    private static final int SPAWN_ROW_HEIGHT = 22;
    private static final int SPAWN_VISIBLE_ROWS = 5;
    private static final int SPAWN_HEADER_HEIGHT = 28;
    private static final int SPAWN_GIVE_BUTTON_WIDTH = 52;

    private enum Tab {
        BALANCE("economy.admin.tab.balance"),
        MASTER("economy.admin.tab.master"),
        SPAWN("economy.admin.tab.spawn"),
        ADMIN("economy.admin.tab.admin");

        private final String labelKey;

        Tab(String labelKey) {
            this.labelKey = labelKey;
        }
    }

    private enum MasterSection {
        BALANCE("economy.admin.section.balance"),
        REWARDS("economy.admin.section.rewards"),
        ITEMS("economy.admin.section.items"),
        SHOP_ITEMS("economy.admin.section.shop_items"),
        ETF_ITEMS("economy.admin.section.etf_items");

        private final String labelKey;

        MasterSection(String labelKey) {
            this.labelKey = labelKey;
        }
    }

    private record PlayerBalanceRow(String username, int balance, int bankBalance, int debt, boolean active) {
    }

    private record ToggleOption(String label, BooleanSupplier getter, Consumer<Boolean> setter) {
    }

    private record RewardRow(String actionType, String displayName, int rewardAmount) {
    }

    private record ItemPriceRow(
            int id,
            String name,
            String itemKey,
            String matchPotion,
            String matchEnchantment,
            Integer matchEnchantmentLevel,
            Integer buyPrice,
            Integer sellPrice
    ) {
    }

    private record ShopItemRow(int id, int shopId, int itemId, int orderNo, String itemName, String itemKey, Integer dailyLimit, Integer userLimit) {
    }

    private record EtfItemRow(String etfCode, String itemKey, String itemName, double influenceWeight) {
    }

    private record ShopRow(int id, String shopName, String npcType, String npcModel) {
    }

    private Tab activeTab = Tab.BALANCE;
    private MasterSection masterSection = MasterSection.BALANCE;
    private String statusMessage = "";
    private boolean isProcessing = false;
    private boolean isLoadingBalances = false;
    private boolean isLoadingMasterConfig = false;
    private boolean isLoadingMasterList = false;
    private boolean isLoadingShops = false;
    private int balancePage = 0;
    private int masterScrollOffset = 0;
    private int spawnScrollOffset = 0;
    private int adminScrollOffset = 0;
    private boolean initialDataLoaded = false;
    private final List<PlayerBalanceRow> playerBalances = new ArrayList<>();
    private final List<RewardRow> rewardRows = new ArrayList<>();
    private final List<ItemPriceRow> itemRows = new ArrayList<>();
    private final List<ShopItemRow> shopItemRows = new ArrayList<>();
    private final List<EtfItemRow> etfItemRows = new ArrayList<>();
    private final List<ShopRow> shopRows = new ArrayList<>();

    private String masterSourceHint = "";
    private String deathPenaltyText = "";
    private String shortSellText = "";
    private String etfIntervalText = "";
    private String loanMaxText = "";
    private String loanMultiplierText = "";
    private final List<EditBox> balanceBoxes = new ArrayList<>();
    private final List<String> rewardAmountTexts = new ArrayList<>();
    private final List<String> itemBuyTexts = new ArrayList<>();
    private final List<String> itemSellTexts = new ArrayList<>();
    private final List<String> shopIdTexts = new ArrayList<>();
    private final List<String> shopDailyLimitTexts = new ArrayList<>();
    private final List<String> shopUserLimitTexts = new ArrayList<>();
    private final List<String> etfWeightTexts = new ArrayList<>();
    private final List<EditBox> rewardAmountBoxes = new ArrayList<>();
    private final List<EditBox> itemBuyBoxes = new ArrayList<>();
    private final List<EditBox> itemSellBoxes = new ArrayList<>();
    private final List<EditBox> shopIdBoxes = new ArrayList<>();
    private final List<EditBox> shopDailyLimitBoxes = new ArrayList<>();
    private final List<EditBox> shopUserLimitBoxes = new ArrayList<>();
    private final List<EditBox> etfWeightBoxes = new ArrayList<>();
    private final Set<Integer> masterDraftRows = new HashSet<>();
    private final List<String> rewardActionTexts = new ArrayList<>();
    private final List<String> rewardDisplayTexts = new ArrayList<>();
    private final List<String> itemIdTexts = new ArrayList<>();
    private final List<String> itemNameTexts = new ArrayList<>();
    private final List<String> itemKeyTexts = new ArrayList<>();
    private final List<String> itemUnitTexts = new ArrayList<>();
    private final List<String> shopItemIdTexts = new ArrayList<>();
    private final List<String> shopItemRefTexts = new ArrayList<>();
    private final List<String> shopOrderTexts = new ArrayList<>();
    private final List<String> etfCodeTexts = new ArrayList<>();
    private final List<String> etfItemKeyTexts = new ArrayList<>();
    private final List<EditBox> rewardActionBoxes = new ArrayList<>();
    private final List<EditBox> rewardDisplayBoxes = new ArrayList<>();
    private final List<EditBox> itemIdBoxes = new ArrayList<>();
    private final List<EditBox> itemNameBoxes = new ArrayList<>();
    private final List<EditBox> itemKeyBoxes = new ArrayList<>();
    private final List<EditBox> itemUnitBoxes = new ArrayList<>();
    private final List<EditBox> shopItemIdBoxes = new ArrayList<>();
    private final List<EditBox> shopItemRefBoxes = new ArrayList<>();
    private final List<EditBox> shopOrderBoxes = new ArrayList<>();
    private final List<EditBox> etfCodeBoxes = new ArrayList<>();
    private final List<EditBox> etfItemKeyBoxes = new ArrayList<>();

    private boolean resetBalances = true;
    private boolean resetRankingMetrics = true;
    private boolean resetPortfolios = true;
    private boolean resetShopLimits = true;
    private boolean resetFleaMarket = true;
    private boolean resetRankingSnapshots = true;
    private boolean resetEtfPrices = true;
    private boolean resetPlayTime = true;
    private boolean resetTravelDistance = true;
    private boolean resetBlocksBroken = true;
    private boolean resetDeaths = true;
    private boolean resetPlayerKills = true;
    private boolean resetMobKills = true;
    private boolean resetHarvests = true;
    private boolean resetPotionsBrewed = true;
    private boolean resetFishCaught = true;

    protected EconomyAdminScreen() {
        super(Component.translatable("economy.admin.title"));
    }

    @Override
    protected void init() {
        super.init();
        rebuildAdminWidgets();
        if (!initialDataLoaded) {
            initialDataLoaded = true;
            requestPlayerBalances();
        }
    }

    private void rebuildAdminWidgets() {
        refreshAdminWidgets();
    }

    private void requestPlayerBalances() {
        if (isLoadingBalances) {
            return;
        }
        isLoadingBalances = true;
        statusMessage = admin("status.loading_balances");
        ClientAccess.requestQuery("PLAYER_BALANCES", "", 0).thenAccept(json -> {
            Minecraft.getInstance().execute(() -> {
                isLoadingBalances = false;
                playerBalances.clear();
                if (json != null && !"null".equals(json)) {
                    try {
                        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                        JsonArray players = root.getAsJsonArray("players");
                        if (players != null) {
                            for (JsonElement element : players) {
                                JsonObject player = element.getAsJsonObject();
                                playerBalances.add(new PlayerBalanceRow(
                                        player.get("username").getAsString(),
                                        player.get("balance").getAsInt(),
                                        player.get("bankBalance").getAsInt(),
                                        player.get("debt").getAsInt(),
                                        player.has("active") && player.get("active").getAsBoolean()
                                ));
                            }
                        }
                        statusMessage = playerBalances.isEmpty()
                                ? admin("status.no_players_yet")
                                : admin("status.balances_shown", playerBalances.size());
                    } catch (Exception e) {
                        EconomyMod.LOGGER.error("Failed to parse player balances", e);
                        statusMessage = admin("status.balances_parse_fail");
                    }
                } else {
                    statusMessage = admin("status.balances_fetch_fail");
                }
                balancePage = 0;
                refreshAdminWidgets();
            });
        });
    }

    private void requestMasterConfig() {
        if (!canAdmin() || isLoadingMasterConfig) {
            return;
        }
        isLoadingMasterConfig = true;
        statusMessage = admin("status.loading_master");
        refreshAdminWidgets();
        ClientAccess.requestQuery("MASTER_CONFIG", "", 0).thenAccept(json -> {
            Minecraft.getInstance().execute(() -> {
                isLoadingMasterConfig = false;
                String resolvedJson = resolveMasterConfigJson(json);
                if (resolvedJson != null && !"null".equals(resolvedJson)) {
                    try {
                        JsonObject root = JsonParser.parseString(resolvedJson).getAsJsonObject();
                        masterSourceHint = root.has("sourceHint") ? root.get("sourceHint").getAsString() : "";
                        applyMasterConfigToFields(root);
                        statusMessage = admin("status.editing_master");
                    } catch (Exception e) {
                        EconomyMod.LOGGER.error("Failed to parse master config", e);
                        statusMessage = admin("status.master_fetch_fail");
                    }
                } else {
                    statusMessage = admin("status.master_unavailable");
                }
                refreshAdminWidgets(false);
            });
        });
    }

    private static EconomyAdminActionPayload adminAction(String action, int shopId) {
        return EconomyAdminActionPayload.forAction(action, shopId);
    }

    private void requestMasterList() {
        if (!canAdmin() || isLoadingMasterList || masterSection == MasterSection.BALANCE) {
            return;
        }
        isLoadingMasterList = true;
        rewardRows.clear();
        itemRows.clear();
        shopItemRows.clear();
        etfItemRows.clear();
        rewardAmountTexts.clear();
        itemBuyTexts.clear();
        itemSellTexts.clear();
        shopIdTexts.clear();
        shopDailyLimitTexts.clear();
        shopUserLimitTexts.clear();
        etfWeightTexts.clear();
        clearMasterDraftState();
        masterScrollOffset = 0;
        statusMessage = admin("status.loading_master_list");
        refreshAdminWidgets();
        String queryType = switch (masterSection) {
            case REWARDS -> "MASTER_REWARDS";
            case ITEMS -> "MASTER_ITEMS";
            case SHOP_ITEMS -> "MASTER_SHOP_ITEMS";
            case ETF_ITEMS -> "MASTER_ETF_ITEMS";
            default -> "MASTER_ITEMS";
        };
        java.util.concurrent.CompletableFuture<String> future = masterSection == MasterSection.ITEMS
                ? ClientAccess.requestMasterItemsAll()
                : ClientAccess.requestQuery(queryType, "", 0);
        future.thenAccept(json -> {
            Minecraft.getInstance().execute(() -> {
                isLoadingMasterList = false;
                rewardRows.clear();
                itemRows.clear();
                shopItemRows.clear();
                etfItemRows.clear();
                rewardAmountTexts.clear();
                itemBuyTexts.clear();
                itemSellTexts.clear();
                shopIdTexts.clear();
        shopDailyLimitTexts.clear();
                shopUserLimitTexts.clear();
                etfWeightTexts.clear();
                String resolvedJson = switch (masterSection) {
                    case REWARDS -> resolveMasterRewardsJson(json);
                    case ITEMS -> resolveMasterItemsJson(json);
                    case SHOP_ITEMS -> resolveMasterShopItemsJson(json);
                    case ETF_ITEMS -> resolveMasterEtfItemsJson(json);
                    default -> json;
                };
                if (resolvedJson != null && !"null".equals(resolvedJson)) {
                    try {
                        JsonObject root = JsonParser.parseString(resolvedJson).getAsJsonObject();
                        if (root.has("sourceHint")) {
                            masterSourceHint = root.get("sourceHint").getAsString();
                        }
                        JsonArray entries = root.getAsJsonArray("entries");
                        if (entries != null) {
                            for (JsonElement element : entries) {
                                JsonObject row = element.getAsJsonObject();
                                if (masterSection == MasterSection.REWARDS) {
                                    int amount = row.get("rewardAmount").getAsInt();
                                    rewardRows.add(new RewardRow(
                                            row.get("actionType").getAsString(),
                                            row.get("displayName").getAsString(),
                                            amount
                                    ));
                                    rewardAmountTexts.add(String.valueOf(amount));
                                } else if (masterSection == MasterSection.ITEMS) {
                                    Integer buy = row.has("buyPrice") && !row.get("buyPrice").isJsonNull()
                                            ? row.get("buyPrice").getAsInt() : null;
                                    Integer sell = row.has("sellPrice") && !row.get("sellPrice").isJsonNull()
                                            ? row.get("sellPrice").getAsInt() : null;
                                    Integer matchLevel = row.has("matchEnchantmentLevel")
                                            && !row.get("matchEnchantmentLevel").isJsonNull()
                                            ? row.get("matchEnchantmentLevel").getAsInt() : null;
                                    itemRows.add(new ItemPriceRow(
                                            row.get("id").getAsInt(),
                                            row.get("name").getAsString(),
                                            row.has("itemKey") ? row.get("itemKey").getAsString() : "",
                                            optionalJsonString(row, "matchPotion"),
                                            optionalJsonString(row, "matchEnchantment"),
                                            matchLevel,
                                            buy,
                                            sell
                                    ));
                                    itemBuyTexts.add(buy != null ? String.valueOf(buy) : "");
                                    itemSellTexts.add(sell != null ? String.valueOf(sell) : "");
                                } else if (masterSection == MasterSection.SHOP_ITEMS) {
                                    Integer daily = row.has("dailyLimit") && !row.get("dailyLimit").isJsonNull()
                                            ? row.get("dailyLimit").getAsInt() : null;
                                    Integer user = row.has("userLimit") && !row.get("userLimit").isJsonNull()
                                            ? row.get("userLimit").getAsInt() : null;
                                    int shopId = row.get("shopId").getAsInt();
                                    int itemId = row.get("itemId").getAsInt();
                                    String itemName = row.has("itemName")
                                            ? row.get("itemName").getAsString()
                                            : "item#" + itemId;
                                    String itemKey = row.has("itemKey") ? row.get("itemKey").getAsString() : "";
                                    shopItemRows.add(new ShopItemRow(
                                            row.get("id").getAsInt(),
                                            shopId,
                                            itemId,
                                            row.get("orderNo").getAsInt(),
                                            itemName,
                                            itemKey,
                                            daily,
                                            user
                                    ));
                                    shopIdTexts.add(String.valueOf(shopId));
                                    shopDailyLimitTexts.add(daily != null ? String.valueOf(daily) : "");
                                    shopUserLimitTexts.add(user != null ? String.valueOf(user) : "");
                                } else if (masterSection == MasterSection.ETF_ITEMS) {
                                    double weight = row.get("influenceWeight").getAsDouble();
                                    String itemKey = row.get("itemKey").getAsString();
                                    String itemName = row.has("itemName")
                                            ? row.get("itemName").getAsString()
                                            : itemKey;
                                    etfItemRows.add(new EtfItemRow(
                                            row.get("etfCode").getAsString(),
                                            itemKey,
                                            itemName,
                                            weight
                                    ));
                                    etfWeightTexts.add(String.format(Locale.ROOT, "%.4f", weight));
                                }
                            }
                        }
                        String sectionLabel = EconomyMasterI18n.trs(masterSection.labelKey);
                        int total = root.has("total") ? root.get("total").getAsInt()
                                : rewardRows.size() + itemRows.size() + shopItemRows.size() + etfItemRows.size();
                        statusMessage = admin("status.section_count", sectionLabel, total);
                    } catch (Exception e) {
                        EconomyMod.LOGGER.error("Failed to parse master list", e);
                        statusMessage = admin("status.master_list_fail");
                    }
                } else {
                    statusMessage = admin("status.master_list_unavailable");
                }
                masterScrollOffset = 0;
                refreshAdminWidgets(false);
            });
        });
    }

    private void requestShops() {
        if (!canAdmin() || isLoadingShops) {
            return;
        }
        isLoadingShops = true;
        spawnScrollOffset = 0;
        statusMessage = admin("status.loading_shops");
        refreshAdminWidgets();
        ClientAccess.requestQuery("MASTER_SHOPS", "", 0).thenAccept(json -> {
            Minecraft.getInstance().execute(() -> {
                isLoadingShops = false;
                shopRows.clear();
                String resolvedJson = resolveMasterShopsJson(json);
                if (resolvedJson != null && !"null".equals(resolvedJson)) {
                    try {
                        JsonObject root = JsonParser.parseString(resolvedJson).getAsJsonObject();
                        JsonArray entries = root.getAsJsonArray("entries");
                        if (entries != null) {
                            for (JsonElement element : entries) {
                                JsonObject row = element.getAsJsonObject();
                                shopRows.add(new ShopRow(
                                        row.get("id").getAsInt(),
                                        row.get("shopName").getAsString(),
                                        row.get("npcType").getAsString(),
                                        row.get("npcModel").getAsString()
                                ));
                            }
                        }
                        statusMessage = "";
                    } catch (Exception e) {
                        EconomyMod.LOGGER.error("Failed to parse shop list", e);
                        statusMessage = admin("status.shops_fail");
                    }
                } else {
                    statusMessage = admin("status.shops_unavailable");
                }
                spawnScrollOffset = 0;
                refreshAdminWidgets();
            });
        });
    }

    private void applyMasterConfigToFields(JsonObject root) {
        deathPenaltyText = formatRate(root, "deathPenaltyRate");
        shortSellText = formatRate(root, "shortSellLimitRate");
        etfIntervalText = root.has("etfIntervalMinutes") ? String.valueOf(root.get("etfIntervalMinutes").getAsInt()) : "";
        loanMaxText = root.has("loanMaxAmount") ? String.valueOf(root.get("loanMaxAmount").getAsInt()) : "";
        loanMultiplierText = root.has("loanAssetMultiplier") ? String.valueOf(root.get("loanAssetMultiplier").getAsDouble()) : "";
    }

    private static String formatRate(JsonObject root, String key) {
        if (!root.has(key)) {
            return "";
        }
        double value = root.get(key).getAsDouble();
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static boolean isSingleplayerClient() {
        return Minecraft.getInstance().hasSingleplayerServer();
    }

    private String resolveMasterConfigJson(String json) {
        if (!needsMasterConfigFallback(json)) {
            return json;
        }
        String local = EconomyMasterClientData.fetchConfigJson();
        if (local != null) {
            return local;
        }
        return json;
    }

    private String resolveMasterRewardsJson(String json) {
        if (!needsMasterListFallback(json)) {
            return json;
        }
        if (isSingleplayerClient()) {
            String local = EconomyMasterClientData.fetchRewardsJson();
            if (local != null) {
                return local;
            }
        }
        return json;
    }

    private String resolveMasterItemsJson(String json) {
        if (!needsMasterListFallback(json)) {
            return json;
        }
        if (isSingleplayerClient()) {
            String local = EconomyMasterClientData.fetchItemsJson();
            if (local != null) {
                return local;
            }
        }
        return json;
    }

    private String resolveMasterShopsJson(String json) {
        if (!needsMasterListFallback(json)) {
            return json;
        }
        if (isSingleplayerClient()) {
            String local = EconomyMasterClientData.fetchShopsJson();
            if (local != null) {
                return local;
            }
        }
        return json;
    }

    private String resolveMasterShopItemsJson(String json) {
        if (!needsMasterListFallback(json)) {
            return json;
        }
        if (isSingleplayerClient()) {
            String local = EconomyMasterClientData.fetchShopItemsJson();
            if (local != null) {
                return local;
            }
        }
        return json;
    }

    private String resolveMasterEtfItemsJson(String json) {
        if (!needsMasterListFallback(json)) {
            return json;
        }
        if (isSingleplayerClient()) {
            String local = EconomyMasterClientData.fetchEtfItemsJson();
            if (local != null) {
                return local;
            }
        }
        return json;
    }

    private static boolean needsMasterConfigFallback(String json) {
        if (json == null || "null".equals(json)) {
            return true;
        }
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            return !root.has("deathPenaltyRate");
        } catch (Exception e) {
            return true;
        }
    }

    private static boolean needsMasterListFallback(String json) {
        if (json == null || "null".equals(json)) {
            return true;
        }
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            return !root.has("total") || root.get("total").getAsInt() == 0;
        } catch (Exception e) {
            return true;
        }
    }

    private int mainTabTop() {
        return panelTop() + MAIN_TAB_TOP_OFFSET;
    }

    private int masterSubTabTop() {
        return panelTop() + MASTER_SUB_TAB_TOP_OFFSET;
    }

    private int masterHintTop() {
        return panelTop() + MASTER_HINT_TOP_OFFSET;
    }

    private int masterContentTop() {
        return panelTop() + MASTER_CONTENT_TOP_OFFSET;
    }

    private int panelBottom() {
        return panelTop() + panelHeight();
    }

    private int closeButtonTop() {
        return panelBottom() - FOOTER_BOTTOM_MARGIN - FOOTER_CLOSE_HEIGHT;
    }

    private int masterActionRowTop() {
        return closeButtonTop() - FOOTER_ROW_GAP - FOOTER_ACTION_HEIGHT;
    }

    private int masterDraftActionTop() {
        return masterActionRowTop() - FOOTER_ROW_GAP - FOOTER_ACTION_HEIGHT;
    }

    private boolean masterSectionSupportsDraftRows() {
        return masterSection == MasterSection.REWARDS
                || masterSection == MasterSection.ITEMS
                || masterSection == MasterSection.SHOP_ITEMS
                || masterSection == MasterSection.ETF_ITEMS;
    }

    private int masterListScrollBottom() {
        if (activeTab == Tab.MASTER && masterSectionSupportsDraftRows()) {
            return masterDraftActionTop() - 8;
        }
        return masterActionRowTop() - 8;
    }

    private int masterListContentTop() {
        return (masterSection == MasterSection.ITEMS
                || masterSection == MasterSection.SHOP_ITEMS
                || masterSection == MasterSection.ETF_ITEMS)
                ? masterContentTop() + MASTER_LIST_HEADER_HEIGHT
                : masterContentTop();
    }

    private int masterFieldsTop() {
        return masterContentTop() + 4;
    }

    private int masterBalanceRowY(int visibleRowIndex) {
        return masterFieldsTop() + visibleRowIndex * MASTER_ROW_HEIGHT;
    }

    private int masterVisibleBalanceRows() {
        int available = masterListScrollBottom() - masterFieldsTop();
        return Math.max(1, available / MASTER_ROW_HEIGHT);
    }

    private int masterVisibleListRows() {
        int available = masterListScrollBottom() - masterListContentTop();
        return Math.max(1, available / MASTER_LIST_ROW_HEIGHT);
    }

    private int masterListRowY(int visibleRowIndex) {
        return masterListContentTop() + visibleRowIndex * MASTER_LIST_ROW_HEIGHT;
    }

    private int maxMasterScrollOffset() {
        return switch (masterSection) {
            case BALANCE -> Math.max(0, MASTER_FIELD_COUNT - masterVisibleBalanceRows());
            case REWARDS, ITEMS, SHOP_ITEMS, ETF_ITEMS -> Math.max(0, masterListSize() - masterVisibleListRows());
        };
    }

    private void clampMasterScrollOffset() {
        masterScrollOffset = Math.max(0, Math.min(masterScrollOffset, maxMasterScrollOffset()));
    }

    private String rewardAmountTextAt(int index) {
        return index >= 0 && index < rewardAmountTexts.size() ? rewardAmountTexts.get(index) : "";
    }

    private String itemBuyTextAt(int index) {
        return index >= 0 && index < itemBuyTexts.size() ? itemBuyTexts.get(index) : "";
    }

    private String itemSellTextAt(int index) {
        return index >= 0 && index < itemSellTexts.size() ? itemSellTexts.get(index) : "";
    }

    private String shopIdTextAt(int index) {
        return index >= 0 && index < shopIdTexts.size() ? shopIdTexts.get(index) : "";
    }

    private String shopDailyTextAt(int index) {
        return index >= 0 && index < shopDailyLimitTexts.size() ? shopDailyLimitTexts.get(index) : "";
    }

    private String shopUserTextAt(int index) {
        return index >= 0 && index < shopUserLimitTexts.size() ? shopUserLimitTexts.get(index) : "";
    }

    private String etfWeightTextAt(int index) {
        return index >= 0 && index < etfWeightTexts.size() ? etfWeightTexts.get(index) : "";
    }

    private boolean isMasterDraftRow(int index) {
        return masterDraftRows.contains(index);
    }

    private void clearMasterDraftState() {
        masterDraftRows.clear();
        rewardActionTexts.clear();
        rewardDisplayTexts.clear();
        itemIdTexts.clear();
        itemNameTexts.clear();
        itemKeyTexts.clear();
        itemUnitTexts.clear();
        shopItemIdTexts.clear();
        shopItemRefTexts.clear();
        shopOrderTexts.clear();
        etfCodeTexts.clear();
        etfItemKeyTexts.clear();
    }

    private String draftTextAt(List<String> texts, int index) {
        return index >= 0 && index < texts.size() ? texts.get(index) : "";
    }

    private void addMasterDraftRow() {
        if (!masterSectionSupportsDraftRows() || isProcessing || isLoadingMasterList) {
            return;
        }
        captureVisibleListEdits();
        switch (masterSection) {
            case REWARDS -> {
                rewardRows.add(new RewardRow("", "", 0));
                rewardAmountTexts.add("1");
                rewardActionTexts.add("MINE_STONE");
                rewardDisplayTexts.add("採掘");
                masterDraftRows.add(rewardRows.size() - 1);
            }
            case ITEMS -> {
                itemRows.add(new ItemPriceRow(-1, "", "", null, null, null, null, null));
                itemBuyTexts.add("");
                itemSellTexts.add("");
                itemIdTexts.add("");
                itemNameTexts.add("");
                itemKeyTexts.add("minecraft:stone");
                itemUnitTexts.add("個");
                masterDraftRows.add(itemRows.size() - 1);
            }
            case SHOP_ITEMS -> {
                shopItemRows.add(new ShopItemRow(-1, 1, 1, 1, "", "", null, null));
                shopIdTexts.add("1");
                shopDailyLimitTexts.add("");
                shopUserLimitTexts.add("");
                shopItemIdTexts.add("");
                shopItemRefTexts.add("1");
                shopOrderTexts.add("");
                masterDraftRows.add(shopItemRows.size() - 1);
            }
            case ETF_ITEMS -> {
                etfItemRows.add(new EtfItemRow("狩猟開拓", "minecraft:bone", "", 0.01));
                etfWeightTexts.add("0.0100");
                etfCodeTexts.add("狩猟開拓");
                etfItemKeyTexts.add("minecraft:bone");
                masterDraftRows.add(etfItemRows.size() - 1);
            }
            default -> {
                return;
            }
        }
        masterScrollOffset = maxMasterScrollOffset();
        statusMessage = admin("status.draft_added");
        refreshAdminWidgets(false);
    }

    private void removeMasterDraftRows() {
        if (masterDraftRows.isEmpty()) {
            return;
        }
        captureVisibleListEdits();
        List<Integer> indices = masterDraftRows.stream().sorted((a, b) -> Integer.compare(b, a)).toList();
        for (int index : indices) {
            removeMasterRowAt(index);
        }
        masterDraftRows.clear();
        clampMasterScrollOffset();
        statusMessage = admin("status.draft_removed");
        refreshAdminWidgets(false);
    }

    private void removeMasterRowAt(int index) {
        switch (masterSection) {
            case REWARDS -> {
                if (index < rewardRows.size()) {
                    rewardRows.remove(index);
                }
                removeAt(rewardAmountTexts, index);
                removeAt(rewardActionTexts, index);
                removeAt(rewardDisplayTexts, index);
            }
            case ITEMS -> {
                if (index < itemRows.size()) {
                    itemRows.remove(index);
                }
                removeAt(itemBuyTexts, index);
                removeAt(itemSellTexts, index);
                removeAt(itemIdTexts, index);
                removeAt(itemNameTexts, index);
                removeAt(itemKeyTexts, index);
                removeAt(itemUnitTexts, index);
            }
            case SHOP_ITEMS -> {
                if (index < shopItemRows.size()) {
                    shopItemRows.remove(index);
                }
                removeAt(shopIdTexts, index);
                removeAt(shopDailyLimitTexts, index);
                removeAt(shopUserLimitTexts, index);
                removeAt(shopItemIdTexts, index);
                removeAt(shopItemRefTexts, index);
                removeAt(shopOrderTexts, index);
            }
            case ETF_ITEMS -> {
                if (index < etfItemRows.size()) {
                    etfItemRows.remove(index);
                }
                removeAt(etfWeightTexts, index);
                removeAt(etfCodeTexts, index);
                removeAt(etfItemKeyTexts, index);
            }
            default -> {
            }
        }
    }

    private static void removeAt(List<String> list, int index) {
        if (index >= 0 && index < list.size()) {
            list.remove(index);
        }
    }


    private int masterListSize() {
        return switch (masterSection) {
            case REWARDS -> rewardRows.size();
            case ITEMS -> itemRows.size();
            case SHOP_ITEMS -> shopItemRows.size();
            case ETF_ITEMS -> etfItemRows.size();
            default -> 0;
        };
    }

    private int spawnListTop() {
        return contentTop() + SPAWN_HEADER_HEIGHT;
    }

    private int spawnListScrollBottom() {
        return footerTop() - 22;
    }

    private int spawnScrollIndicatorY() {
        return footerTop() - 14;
    }

    private int spawnVisibleRows() {
        int available = spawnListScrollBottom() - spawnListTop();
        return Math.max(1, available / SPAWN_ROW_HEIGHT);
    }

    private int spawnListRowY(int visibleRowIndex) {
        return spawnListTop() + visibleRowIndex * SPAWN_ROW_HEIGHT;
    }

    private int maxSpawnScrollOffset() {
        return Math.max(0, shopRows.size() - spawnVisibleRows());
    }

    private void clampSpawnScrollOffset() {
        spawnScrollOffset = Math.max(0, Math.min(spawnScrollOffset, maxSpawnScrollOffset()));
    }

    private boolean isKnownShopId(int shopId) {
        for (ShopRow shop : shopRows) {
            if (shop.id() == shopId) {
                return true;
            }
        }
        return false;
    }

    private String shopDisplayNameFitting(int shopId) {
        for (ShopRow shop : shopRows) {
            if (shop.id() == shopId) {
                String suffix = npcTypeLabel(shop.npcType());
                String name = EconomyMasterI18n.shopName(shop.id(), shop.shopName());
                String label = name + suffix;
                int maxWidth = SHOP_NPC_INNER_WIDTH - 4;
                while (name.length() > 0 && font.width(label) > maxWidth) {
                    name = name.substring(0, name.length() - 1);
                    label = name + suffix;
                }
                return name.isEmpty() ? truncate(suffix, 2) : label;
            }
        }
        return "#" + shopId;
    }

    private void cycleShopAssignment(int index, int delta) {
        if (shopRows.isEmpty() || index < 0 || index >= shopIdTexts.size()) {
            return;
        }
        Integer current = parseInt(shopIdTextAt(index));
        if (current == null) {
            current = shopItemRows.get(index).shopId();
        }
        List<Integer> shopIds = shopRows.stream().map(ShopRow::id).toList();
        int position = shopIds.indexOf(current);
        if (position < 0) {
            position = 0;
        }
        int next = (position + delta + shopIds.size()) % shopIds.size();
        shopIdTexts.set(index, String.valueOf(shopIds.get(next)));
        refreshAdminWidgets();
    }

    private static String npcTypeLabel(String npcType) {
        return switch (npcType == null ? "" : npcType.toUpperCase(Locale.ROOT)) {
            case "BUYER" -> admin("npc.buyer");
            case "STOCK_TRADER" -> admin("npc.stock");
            case "FLEA_MARKET" -> admin("npc.flea");
            case "LOAN" -> admin("npc.loan");
            default -> admin("npc.seller");
        };
    }

    private int masterFieldBoxWidth() {
        return PANEL_WIDTH - 32 - MASTER_LABEL_WIDTH - 4;
    }

    private boolean canAdmin() {
        return EconomyAdminAuth.canPerformAdminActionsClient();
    }

    private int masterRequiredPanelHeight() {
        int footerBlock = FOOTER_BOTTOM_MARGIN + FOOTER_CLOSE_HEIGHT + FOOTER_ROW_GAP + FOOTER_ACTION_HEIGHT;
        int draftRowBlock = FOOTER_ROW_GAP + FOOTER_ACTION_HEIGHT;
        int headerBlock = MASTER_CONTENT_TOP_OFFSET + 8;
        int balanceContent = MASTER_BALANCE_VISIBLE_ROWS * MASTER_ROW_HEIGHT + 12;
        int listContent = MASTER_LIST_HEADER_HEIGHT + MASTER_LIST_VISIBLE_ROWS * MASTER_LIST_ROW_HEIGHT + 8 + draftRowBlock;
        return Math.max(headerBlock + balanceContent + footerBlock, headerBlock + listContent + footerBlock);
    }

    private int panelHeight() {
        int available = height - SCREEN_MARGIN * 2;
        int desired = switch (activeTab) {
            case BALANCE -> {
                int visibleRows = playerBalances.isEmpty() ? 1 : Math.min(PLAYERS_PER_PAGE, playerBalances.size());
                yield MAIN_TAB_TOP_OFFSET + 22 + visibleRows * 20 + 54 + 28;
            }
            case MASTER -> canAdmin() ? masterRequiredPanelHeight() : 34 + 40 + 28;
            case SPAWN -> canAdmin() ? MAIN_TAB_TOP_OFFSET + 22 + SPAWN_HEADER_HEIGHT + SPAWN_VISIBLE_ROWS * SPAWN_ROW_HEIGHT + 54 + 28 : 34 + 40 + 28;
            case ADMIN -> canAdmin()
                    ? MAIN_TAB_TOP_OFFSET + 22 + ADMIN_VISIBLE_ROWS * ROW_HEIGHT + 8
                            + FOOTER_ACTION_HEIGHT + FOOTER_ROW_GAP + FOOTER_CLOSE_HEIGHT + FOOTER_BOTTOM_MARGIN
                    : 34 + 40 + 28;
        };
        return Math.min(Math.max(desired, 150), available);
    }

    private int panelTop() {
        int h = panelHeight();
        return Math.max(SCREEN_MARGIN, (height - h) / 2);
    }

    private int panelLeft() {
        return width / 2 - PANEL_WIDTH / 2;
    }

    private int contentTop() {
        return panelTop() + MAIN_TAB_TOP_OFFSET + 22;
    }

    private int footerTop() {
        return panelTop() + panelHeight() - 48;
    }

    private int contentLeft() {
        return panelLeft() + 16;
    }

    private int columnWidth() {
        return (PANEL_WIDTH - 32 - COL_GAP) / 2;
    }

    private void captureMasterTexts() {
        if (masterSection != MasterSection.BALANCE || isLoadingMasterConfig || balanceBoxes.isEmpty()) {
            return;
        }
        int start = masterScrollOffset;
        String[] values = {deathPenaltyText, shortSellText, etfIntervalText, loanMaxText, loanMultiplierText};
        for (int i = 0; i < balanceBoxes.size(); i++) {
            int index = start + i;
            if (index >= 0 && index < values.length) {
                values[index] = balanceBoxes.get(i).getValue();
            }
        }
        deathPenaltyText = values[0];
        shortSellText = values[1];
        etfIntervalText = values[2];
        loanMaxText = values[3];
        loanMultiplierText = values[4];
    }

    private void captureVisibleListEdits() {
        if (masterSection == MasterSection.REWARDS) {
            int start = masterScrollOffset;
            int actionBoxIndex = 0;
            int displayBoxIndex = 0;
            for (int i = 0; i < rewardAmountBoxes.size(); i++) {
                int index = start + i;
                if (index < rewardAmountTexts.size()) {
                    rewardAmountTexts.set(index, rewardAmountBoxes.get(i).getValue());
                }
                if (index < rewardRows.size() && isMasterDraftRow(index)) {
                    if (actionBoxIndex < rewardActionBoxes.size() && index < rewardActionTexts.size()) {
                        rewardActionTexts.set(index, rewardActionBoxes.get(actionBoxIndex).getValue());
                        actionBoxIndex++;
                    }
                    if (displayBoxIndex < rewardDisplayBoxes.size() && index < rewardDisplayTexts.size()) {
                        rewardDisplayTexts.set(index, rewardDisplayBoxes.get(displayBoxIndex).getValue());
                        displayBoxIndex++;
                    }
                }
            }
            return;
        }
        if (masterSection == MasterSection.ITEMS) {
            int start = masterScrollOffset;
            int nameBoxIndex = 0;
            int keyBoxIndex = 0;
            for (int i = 0; i < itemBuyBoxes.size(); i++) {
                int index = start + i;
                if (index < itemBuyTexts.size()) {
                    itemBuyTexts.set(index, itemBuyBoxes.get(i).getValue());
                }
                if (index < itemSellTexts.size()) {
                    itemSellTexts.set(index, itemSellBoxes.get(i).getValue());
                }
                if (index < itemRows.size() && isMasterDraftRow(index)) {
                    if (nameBoxIndex < itemNameBoxes.size() && index < itemNameTexts.size()) {
                        itemNameTexts.set(index, itemNameBoxes.get(nameBoxIndex).getValue());
                        nameBoxIndex++;
                    }
                    if (keyBoxIndex < itemKeyBoxes.size() && index < itemKeyTexts.size()) {
                        itemKeyTexts.set(index, itemKeyBoxes.get(keyBoxIndex).getValue());
                        keyBoxIndex++;
                    }
                }
            }
            return;
        }
        if (masterSection == MasterSection.SHOP_ITEMS) {
            int start = masterScrollOffset;
            int itemRefBoxIndex = 0;
            for (int i = 0; i < shopDailyLimitBoxes.size(); i++) {
                int index = start + i;
                if (i < shopIdBoxes.size() && index < shopIdTexts.size()) {
                    shopIdTexts.set(index, shopIdBoxes.get(i).getValue());
                }
                if (index < shopDailyLimitTexts.size()) {
                    shopDailyLimitTexts.set(index, shopDailyLimitBoxes.get(i).getValue());
                }
                if (index < shopUserLimitTexts.size()) {
                    shopUserLimitTexts.set(index, shopUserLimitBoxes.get(i).getValue());
                }
                if (index < shopItemRows.size() && isMasterDraftRow(index)) {
                    if (itemRefBoxIndex < shopItemRefBoxes.size() && index < shopItemRefTexts.size()) {
                        shopItemRefTexts.set(index, shopItemRefBoxes.get(itemRefBoxIndex).getValue());
                        itemRefBoxIndex++;
                    }
                }
            }
            return;
        }
        if (masterSection == MasterSection.ETF_ITEMS) {
            int start = masterScrollOffset;
            int codeBoxIndex = 0;
            int keyBoxIndex = 0;
            for (int i = 0; i < etfWeightBoxes.size(); i++) {
                int index = start + i;
                if (index < etfWeightTexts.size()) {
                    etfWeightTexts.set(index, etfWeightBoxes.get(i).getValue());
                }
                if (index < etfItemRows.size() && isMasterDraftRow(index)) {
                    if (codeBoxIndex < etfCodeBoxes.size() && index < etfCodeTexts.size()) {
                        etfCodeTexts.set(index, etfCodeBoxes.get(codeBoxIndex).getValue());
                        codeBoxIndex++;
                    }
                    if (keyBoxIndex < etfItemKeyBoxes.size() && index < etfItemKeyTexts.size()) {
                        etfItemKeyTexts.set(index, etfItemKeyBoxes.get(keyBoxIndex).getValue());
                        keyBoxIndex++;
                    }
                }
            }
        }
    }

    private void refreshAdminWidgets() {
        refreshAdminWidgets(true);
    }

    private void refreshAdminWidgets(boolean persistEdits) {
        if (persistEdits && activeTab == Tab.MASTER) {
            if (masterSection == MasterSection.BALANCE) {
                captureMasterTexts();
            } else {
                captureVisibleListEdits();
            }
        }
        clearWidgets();
        balanceBoxes.clear();
        rewardAmountBoxes.clear();
        itemBuyBoxes.clear();
        itemSellBoxes.clear();
        shopIdBoxes.clear();
        shopDailyLimitBoxes.clear();
        shopUserLimitBoxes.clear();
        etfWeightBoxes.clear();
        rewardActionBoxes.clear();
        rewardDisplayBoxes.clear();
        itemIdBoxes.clear();
        itemNameBoxes.clear();
        itemKeyBoxes.clear();
        itemUnitBoxes.clear();
        shopItemIdBoxes.clear();
        shopItemRefBoxes.clear();
        shopOrderBoxes.clear();
        etfCodeBoxes.clear();
        etfItemKeyBoxes.clear();

        int tabY = mainTabTop();
        int tabWidth = 52;
        int tabGap = 4;
        int totalTabWidth = tabWidth * 4 + tabGap * 3;
        int tabStart = width / 2 - totalTabWidth / 2;

        addRenderableWidget(Button.builder(Component.literal(tabLabel(Tab.BALANCE)), b -> switchTab(Tab.BALANCE))
                .bounds(tabStart, tabY, tabWidth, 18).build());
        addRenderableWidget(Button.builder(Component.literal(tabLabel(Tab.MASTER)), b -> switchTab(Tab.MASTER))
                .bounds(tabStart + tabWidth + tabGap, tabY, tabWidth, 18).build());
        addRenderableWidget(Button.builder(Component.literal(tabLabel(Tab.SPAWN)), b -> switchTab(Tab.SPAWN))
                .bounds(tabStart + (tabWidth + tabGap) * 2, tabY, tabWidth, 18).build());
        addRenderableWidget(Button.builder(Component.literal(tabLabel(Tab.ADMIN)), b -> switchTab(Tab.ADMIN))
                .bounds(tabStart + (tabWidth + tabGap) * 3, tabY, tabWidth, 18).build());

        switch (activeTab) {
            case BALANCE -> buildBalanceTab();
            case MASTER -> buildMasterTab();
            case SPAWN -> buildSpawnTab();
            case ADMIN -> buildAdminTab();
        }

        addRenderableWidget(Button.builder(EconomyMasterI18n.tr("economy.ui.close"), b -> onClose())
                .bounds(width / 2 - 36, closeButtonTop(), 72, FOOTER_CLOSE_HEIGHT).build());
    }

    private static String admin(String key) {
        return EconomyMasterI18n.trs("economy.admin." + key);
    }

    private static String admin(String key, Object... args) {
        return EconomyMasterI18n.tr("economy.admin." + key, args).getString();
    }

    private String tabLabel(Tab tab) {
        String prefix = activeTab == tab ? "§e" : "§7";
        return prefix + EconomyMasterI18n.trs(tab.labelKey);
    }

    private String masterSectionLabel(MasterSection section) {
        String prefix = masterSection == section ? "§e" : "§7";
        return prefix + EconomyMasterI18n.trs(section.labelKey);
    }

    private void switchTab(Tab tab) {
        if ((tab == Tab.ADMIN || tab == Tab.MASTER || tab == Tab.SPAWN) && !canAdmin()) {
            statusMessage = admin("status.op_required");
            activeTab = Tab.BALANCE;
        } else {
            activeTab = tab;
            if (tab == Tab.ADMIN) {
                adminScrollOffset = 0;
            }
            if (statusMessage.equals(admin("status.op_required"))) {
                statusMessage = "";
            }
            if (tab == Tab.MASTER) {
                loadMasterSectionData();
            } else if (tab == Tab.SPAWN) {
                requestShops();
            }
        }
        refreshAdminWidgets();
    }

    private void switchMasterSection(MasterSection section) {
        if (masterSection != section) {
            if (masterSection == MasterSection.BALANCE) {
                captureMasterTexts();
            } else {
                captureVisibleListEdits();
            }
        }
        masterSection = section;
        masterScrollOffset = 0;
        refreshAdminWidgets(false);
        loadMasterSectionData();
    }

    private void loadMasterSectionData() {
        if (masterSection == MasterSection.BALANCE) {
            requestMasterConfig();
        } else if (masterSection == MasterSection.SHOP_ITEMS) {
            requestShopsForMasterAssignment();
        } else {
            requestMasterList();
        }
    }

    private void requestShopsForMasterAssignment() {
        if (!canAdmin() || isLoadingMasterList) {
            return;
        }
        ClientAccess.requestQuery("MASTER_SHOPS", "", 0).thenAccept(json -> {
            Minecraft.getInstance().execute(() -> {
                shopRows.clear();
                String resolvedJson = resolveMasterShopsJson(json);
                if (resolvedJson != null && !"null".equals(resolvedJson)) {
                    try {
                        JsonObject root = JsonParser.parseString(resolvedJson).getAsJsonObject();
                        JsonArray entries = root.getAsJsonArray("entries");
                        if (entries != null) {
                            for (JsonElement element : entries) {
                                JsonObject row = element.getAsJsonObject();
                                shopRows.add(new ShopRow(
                                        row.get("id").getAsInt(),
                                        row.get("shopName").getAsString(),
                                        row.get("npcType").getAsString(),
                                        row.get("npcModel").getAsString()
                                ));
                            }
                        }
                    } catch (Exception e) {
                        EconomyMod.LOGGER.error("Failed to parse shops for assignment", e);
                    }
                }
                requestMasterList();
            });
        });
    }

    private void buildBalanceTab() {
        int footerY = footerTop();
        int panelLeft = panelLeft();
        int arrowWidth = 22;
        int arrowGap = 8;
        int updateWidth = 72;
        int groupWidth = arrowWidth * 2 + arrowGap * 2 + updateWidth;
        int groupLeft = panelLeft + (PANEL_WIDTH - groupWidth) / 2;
        int updateX = groupLeft + arrowWidth + arrowGap;
        int rightArrowX = updateX + updateWidth + arrowGap;

        addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
            if (balancePage > 0) {
                balancePage--;
                refreshAdminWidgets();
            }
        }).bounds(groupLeft, footerY, arrowWidth, 18).build()).active = balancePage > 0;

        int maxPage = Math.max(0, (playerBalances.size() - 1) / PLAYERS_PER_PAGE);
        addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
            if (balancePage < maxPage) {
                balancePage++;
                refreshAdminWidgets();
            }
        }).bounds(rightArrowX, footerY, arrowWidth, 18).build()).active = balancePage < maxPage;

        addRenderableWidget(Button.builder(Component.literal(admin("btn.refresh")), b -> requestPlayerBalances())
                .bounds(updateX, footerY, updateWidth, 18).build()).active = !isLoadingBalances;
    }

    private void buildSpawnTab() {
        if (!canAdmin()) {
            statusMessage = admin("status.op_required");
            return;
        }
        clampSpawnScrollOffset();
        int leftX = contentLeft();
        int buttonX = panelLeft() + PANEL_WIDTH - 16 - SPAWN_GIVE_BUTTON_WIDTH;
        int start = spawnScrollOffset;
        int end = Math.min(shopRows.size(), start + spawnVisibleRows());
        for (int i = start; i < end; i++) {
            ShopRow shop = shopRows.get(i);
            int rowY = spawnListRowY(i - start);
            int finalShopId = shop.id();
            addRenderableWidget(Button.builder(Component.literal(admin("btn.give")), b -> sendGiveSpawnEgg(finalShopId))
                    .bounds(buttonX, rowY + 2, SPAWN_GIVE_BUTTON_WIDTH, 18).build())
                    .active = !isProcessing && !isLoadingShops;
        }
        addRenderableWidget(Button.builder(Component.literal(admin("btn.give_all")), b -> sendGiveAllSpawnEggs())
                .bounds(leftX, footerTop(), PANEL_WIDTH - 32, 18).build())
                .active = !isProcessing && !isLoadingShops && !shopRows.isEmpty();
    }

    private void scrollSpawnList(int delta) {
        if (delta == 0) {
            return;
        }
        int next = spawnScrollOffset + delta;
        if (next < 0 || next > maxSpawnScrollOffset()) {
            return;
        }
        spawnScrollOffset = next;
        refreshAdminWidgets();
    }

    private void buildMasterTab() {
        if (!canAdmin()) {
            statusMessage = admin("status.op_required");
            return;
        }
        clampMasterScrollOffset();

        int leftX = contentLeft();
        int subTabWidth = 64;
        int subTabGap = 4;
        int subTabStart = panelLeft() + (PANEL_WIDTH - (subTabWidth * 5 + subTabGap * 4)) / 2;
        int subY = masterSubTabTop();
        addRenderableWidget(Button.builder(Component.literal(masterSectionLabel(MasterSection.BALANCE)),
                b -> switchMasterSection(MasterSection.BALANCE)).bounds(subTabStart, subY, subTabWidth, 18).build());
        addRenderableWidget(Button.builder(Component.literal(masterSectionLabel(MasterSection.REWARDS)),
                b -> switchMasterSection(MasterSection.REWARDS)).bounds(subTabStart + subTabWidth + subTabGap, subY, subTabWidth, 18).build());
        addRenderableWidget(Button.builder(Component.literal(masterSectionLabel(MasterSection.ITEMS)),
                b -> switchMasterSection(MasterSection.ITEMS)).bounds(subTabStart + (subTabWidth + subTabGap) * 2, subY, subTabWidth, 18).build());
        addRenderableWidget(Button.builder(Component.literal(masterSectionLabel(MasterSection.SHOP_ITEMS)),
                b -> switchMasterSection(MasterSection.SHOP_ITEMS)).bounds(subTabStart + (subTabWidth + subTabGap) * 3, subY, subTabWidth, 18).build());
        addRenderableWidget(Button.builder(Component.literal(masterSectionLabel(MasterSection.ETF_ITEMS)),
                b -> switchMasterSection(MasterSection.ETF_ITEMS)).bounds(subTabStart + (subTabWidth + subTabGap) * 4, subY, subTabWidth, 18).build());

        switch (masterSection) {
            case BALANCE -> buildMasterBalanceSection(leftX);
            case REWARDS -> buildMasterRewardsSection(leftX);
            case ITEMS -> buildMasterItemsSection(leftX);
            case SHOP_ITEMS -> buildMasterShopItemsSection(leftX);
            case ETF_ITEMS -> buildMasterEtfItemsSection(leftX);
        }

        buildMasterFooterActions(leftX);
    }

    private void scrollMasterList(int delta) {
        if (delta == 0) {
            return;
        }
        if (masterSection == MasterSection.BALANCE) {
            captureMasterTexts();
        } else {
            captureVisibleListEdits();
        }
        int next = masterScrollOffset + delta;
        if (next < 0 || next > maxMasterScrollOffset()) {
            return;
        }
        masterScrollOffset = next;
        setFocused(null);
        refreshAdminWidgets(false);
    }

    private void buildMasterBalanceSection(int leftX) {
        int boxWidth = masterFieldBoxWidth();
        int boxX = leftX + MASTER_LABEL_WIDTH + 4;
        String[] values = {deathPenaltyText, shortSellText, etfIntervalText, loanMaxText, loanMultiplierText};
        int start = masterScrollOffset;
        int end = Math.min(MASTER_FIELD_COUNT, start + masterVisibleBalanceRows());
        for (int i = start; i < end; i++) {
            int visibleRow = i - start;
            int y = masterBalanceRowY(visibleRow) + MASTER_LIST_FIELD_Y_INSET;
            balanceBoxes.add(addMasterField(boxX, y, boxWidth, values[i]));
        }
    }

    private void buildMasterRewardsSection(int leftX) {
        int actionX = leftX + 28;
        int actionWidth = 72;
        int displayX = leftX + 104;
        int displayWidth = 80;
        int amountX = leftX + 188;
        int amountWidth = PANEL_WIDTH - 32 - 188;
        int start = masterScrollOffset;
        int end = Math.min(rewardRows.size(), start + masterVisibleListRows());
        for (int i = start; i < end; i++) {
            int visibleRow = i - start;
            int rowY = masterListRowY(visibleRow) + MASTER_LIST_FIELD_Y_INSET;
            if (isMasterDraftRow(i)) {
                rewardActionBoxes.add(addMasterField(actionX, rowY, actionWidth, draftTextAt(rewardActionTexts, i), 32));
                rewardDisplayBoxes.add(addMasterField(displayX, rowY, displayWidth, draftTextAt(rewardDisplayTexts, i), 24));
            }
            rewardAmountBoxes.add(addMasterField(amountX, rowY, amountWidth, rewardAmountTextAt(i)));
        }
    }

    private void buildMasterItemsSection(int leftX) {
        int nameX = leftX;
        int nameWidth = 58;
        int keyX = leftX + 62;
        int keyWidth = 118;
        int buyX = leftX + 188;
        int sellX = buyX + 58;
        int boxWidth = 52;
        int start = masterScrollOffset;
        int end = Math.min(itemRows.size(), start + masterVisibleListRows());
        for (int i = start; i < end; i++) {
            int visibleRow = i - start;
            int rowY = masterListRowY(visibleRow) + MASTER_LIST_FIELD_Y_INSET;
            if (isMasterDraftRow(i)) {
                itemNameBoxes.add(addMasterField(nameX, rowY, nameWidth, draftTextAt(itemNameTexts, i), 24));
                itemKeyBoxes.add(addMasterField(keyX, rowY, keyWidth, draftTextAt(itemKeyTexts, i), 64));
            }
            itemBuyBoxes.add(addMasterField(buyX, rowY, boxWidth, itemBuyTextAt(i)));
            itemSellBoxes.add(addMasterField(sellX, rowY, boxWidth, itemSellTextAt(i)));
        }
    }

    private void buildMasterShopItemsSection(int leftX) {
        int itemRefX = leftX;
        int itemRefWidth = 76;
        int shopControlX = leftX + SHOP_COL_NPC;
        int dailyX = leftX + SHOP_COL_DAILY;
        int userX = leftX + SHOP_COL_USER;
        int boxWidth = SHOP_FIELD_WIDTH;
        boolean canEdit = !isProcessing && !isLoadingMasterConfig && !isLoadingMasterList;
        int start = masterScrollOffset;
        int end = Math.min(shopItemRows.size(), start + masterVisibleListRows());
        for (int i = start; i < end; i++) {
            int visibleRow = i - start;
            int rowY = masterListRowY(visibleRow) + MASTER_LIST_FIELD_Y_INSET;
            if (isMasterDraftRow(i)) {
                shopItemRefBoxes.add(addMasterField(itemRefX, rowY, itemRefWidth, draftTextAt(shopItemRefTexts, i)));
            }
            if (!shopRows.isEmpty()) {
                int rowIndex = i;
                addRenderableWidget(Button.builder(Component.literal("◀"), b -> cycleShopAssignment(rowIndex, -1))
                        .bounds(shopControlX, rowY, SHOP_NPC_ARROW_WIDTH, 18).build()).active = canEdit;
                addRenderableWidget(Button.builder(Component.literal("▶"), b -> cycleShopAssignment(rowIndex, 1))
                        .bounds(shopControlX + SHOP_NPC_ARROW_WIDTH + SHOP_NPC_INNER_WIDTH, rowY, SHOP_NPC_ARROW_WIDTH, 18).build()).active = canEdit;
            } else {
                shopIdBoxes.add(addMasterField(shopControlX, rowY, 28, shopIdTextAt(i)));
            }
            shopDailyLimitBoxes.add(addMasterField(dailyX, rowY, boxWidth, shopDailyTextAt(i)));
            shopUserLimitBoxes.add(addMasterField(userX, rowY, boxWidth, shopUserTextAt(i)));
        }
    }

    private void buildMasterEtfItemsSection(int leftX) {
        int codeX = leftX;
        int codeWidth = 70;
        int keyX = leftX + 74;
        int keyWidth = 118;
        int weightX = leftX + 200;
        int boxWidth = 70;
        int start = masterScrollOffset;
        int end = Math.min(etfItemRows.size(), start + masterVisibleListRows());
        for (int i = start; i < end; i++) {
            int visibleRow = i - start;
            int rowY = masterListRowY(visibleRow) + MASTER_LIST_FIELD_Y_INSET;
            if (isMasterDraftRow(i)) {
                etfCodeBoxes.add(addMasterField(codeX, rowY, codeWidth, draftTextAt(etfCodeTexts, i), 24));
                etfItemKeyBoxes.add(addMasterField(keyX, rowY, keyWidth, draftTextAt(etfItemKeyTexts, i), 64));
            }
            etfWeightBoxes.add(addMasterField(weightX, rowY, boxWidth, etfWeightTextAt(i)));
        }
    }

    private void buildMasterFooterActions(int leftX) {
        int fieldWidth = PANEL_WIDTH - 32;
        int halfWidth = (fieldWidth - COL_GAP) / 2;
        int actionY = masterActionRowTop();
        boolean canEdit = !isProcessing && !isLoadingMasterConfig && !isLoadingMasterList;

        if (masterSectionSupportsDraftRows()) {
            int draftY = masterDraftActionTop();
            addRenderableWidget(Button.builder(Component.literal(admin("btn.add_row")), b -> addMasterDraftRow())
                    .bounds(leftX, draftY, halfWidth, FOOTER_ACTION_HEIGHT).build()).active = canEdit;
            addRenderableWidget(Button.builder(Component.literal(admin("btn.remove_draft")), b -> removeMasterDraftRows())
                    .bounds(leftX + halfWidth + COL_GAP, draftY, halfWidth, FOOTER_ACTION_HEIGHT).build())
                    .active = canEdit && !masterDraftRows.isEmpty();
        }

        addRenderableWidget(Button.builder(Component.literal(admin("btn.save")), b -> sendSaveMaster())
                .bounds(leftX, actionY, halfWidth, FOOTER_ACTION_HEIGHT).build()).active = canEdit;
        addRenderableWidget(Button.builder(Component.literal(admin("btn.reset_bundled")), b -> sendResetMasterConfig())
                .bounds(leftX + halfWidth + COL_GAP, actionY, halfWidth, FOOTER_ACTION_HEIGHT).build())
                .active = canEdit;
    }

    private EditBox addMasterField(int boxX, int y, int boxWidth, String value) {
        return addMasterField(boxX, y, boxWidth, value, 16);
    }

    private EditBox addMasterField(int boxX, int y, int boxWidth, String value, int maxLength) {
        EditBox box = new EditBox(font, boxX, y, boxWidth, 18, Component.literal(""));
        box.setMaxLength(maxLength);
        box.setValue(value == null ? "" : value);
        box.setEditable(!isProcessing && !isLoadingMasterConfig && !isLoadingMasterList);
        addRenderableWidget(box);
        return box;
    }

    private void buildAdminTab() {
        if (!canAdmin()) {
            statusMessage = admin("status.op_required");
            return;
        }

        List<ToggleOption> options = List.of(
                new ToggleOption(admin("toggle.balances"), () -> resetBalances, v -> resetBalances = v),
                new ToggleOption(admin("toggle.ranking_metrics"), () -> resetRankingMetrics, v -> resetRankingMetrics = v),
                new ToggleOption(admin("toggle.portfolios"), () -> resetPortfolios, v -> resetPortfolios = v),
                new ToggleOption(admin("toggle.shop_limits"), () -> resetShopLimits, v -> resetShopLimits = v),
                new ToggleOption(admin("toggle.flea_market"), () -> resetFleaMarket, v -> resetFleaMarket = v),
                new ToggleOption(admin("toggle.ranking_snapshots"), () -> resetRankingSnapshots, v -> resetRankingSnapshots = v),
                new ToggleOption(admin("toggle.etf_prices"), () -> resetEtfPrices, v -> resetEtfPrices = v),
                new ToggleOption(admin("toggle.play_time"), () -> resetPlayTime, v -> resetPlayTime = v),
                new ToggleOption(admin("toggle.travel_distance"), () -> resetTravelDistance, v -> resetTravelDistance = v),
                new ToggleOption(admin("toggle.blocks_broken"), () -> resetBlocksBroken, v -> resetBlocksBroken = v),
                new ToggleOption(admin("toggle.deaths"), () -> resetDeaths, v -> resetDeaths = v),
                new ToggleOption(admin("toggle.player_kills"), () -> resetPlayerKills, v -> resetPlayerKills = v),
                new ToggleOption(admin("toggle.mob_kills"), () -> resetMobKills, v -> resetMobKills = v),
                new ToggleOption(admin("toggle.harvests"), () -> resetHarvests, v -> resetHarvests = v),
                new ToggleOption(admin("toggle.potions_brewed"), () -> resetPotionsBrewed, v -> resetPotionsBrewed = v),
                new ToggleOption(admin("toggle.fish_caught"), () -> resetFishCaught, v -> resetFishCaught = v)
        );

        int leftX = contentLeft();
        int rightX = leftX + columnWidth() + COL_GAP;
        int colWidth = columnWidth();
        int start = adminScrollOffset;
        int end = Math.min(options.size(), start + ADMIN_VISIBLE_ROWS * 2);
        int y = contentTop();

        for (int i = start; i < end; i += 2) {
            addToggleButton(leftX, y, colWidth, options.get(i));
            if (i + 1 < options.size()) {
                addToggleButton(rightX, y, colWidth, options.get(i + 1));
            }
            y += ROW_HEIGHT;
        }

        y = adminActionRowTop();
        addRenderableWidget(Button.builder(Component.literal(admin("btn.reset_selected")), b -> sendReset())
                .bounds(leftX, y, colWidth, 18).build()).active = !isProcessing;
        addRenderableWidget(Button.builder(Component.literal(admin("btn.compile_ranking")), b -> sendCompileRanking())
                .bounds(rightX, y, colWidth, 18).build()).active = !isProcessing;
    }

    private int adminListTop() {
        return contentTop();
    }

    private int adminListScrollBottom() {
        return adminActionRowTop() - 8;
    }

    private int adminActionRowTop() {
        return closeButtonTop() - FOOTER_ROW_GAP - FOOTER_ACTION_HEIGHT;
    }

    private int maxAdminScrollOffset() {
        int optionRows = (16 + 1) / 2;
        return Math.max(0, optionRows - ADMIN_VISIBLE_ROWS);
    }

    private void scrollAdminList(int delta) {
        if (delta == 0) {
            return;
        }
        int next = adminScrollOffset + delta;
        if (next < 0 || next > maxAdminScrollOffset()) {
            return;
        }
        adminScrollOffset = next;
        refreshAdminWidgets();
    }

    private void addToggleButton(int x, int y, int width, ToggleOption option) {
        boolean value = option.getter().getAsBoolean();
        Button button = Button.builder(Component.literal((value ? "§a[ON] " : "§7[OFF] ") + option.label()), b -> {
            option.setter().accept(!value);
            refreshAdminWidgets();
        }).bounds(x, y, width, 18).build();
        button.active = !isProcessing;
        addRenderableWidget(button);
    }

    private void sendReset() {
        if (isProcessing || !canAdmin()) {
            return;
        }
        isProcessing = true;
        statusMessage = admin("status.resetting");
        refreshAdminWidgets();
        Minecraft.getInstance().getConnection().send(new EconomyAdminActionPayload(
                "RESET",
                resetBalances,
                resetRankingMetrics,
                resetPortfolios,
                resetShopLimits,
                resetFleaMarket,
                resetRankingSnapshots,
                resetEtfPrices,
                resetPlayTime,
                resetTravelDistance,
                resetBlocksBroken,
                resetDeaths,
                resetPlayerKills,
                resetMobKills,
                resetHarvests,
                resetPotionsBrewed,
                resetFishCaught,
                0
        ));
    }

    private void sendCompileRanking() {
        if (isProcessing || !canAdmin()) {
            return;
        }
        isProcessing = true;
        statusMessage = admin("status.compiling_ranking");
        refreshAdminWidgets();
        Minecraft.getInstance().getConnection().send(adminAction("COMPILE_RANKING", 0));
    }

    private void sendGiveSpawnEgg(int shopId) {
        if (isProcessing || !canAdmin()) {
            return;
        }
        isProcessing = true;
        statusMessage = admin("status.giving_egg");
        refreshAdminWidgets();
        Minecraft.getInstance().getConnection().send(adminAction("GIVE_SPAWN_EGG", shopId));
    }

    private void sendGiveAllSpawnEggs() {
        if (isProcessing || !canAdmin()) {
            return;
        }
        isProcessing = true;
        statusMessage = admin("status.giving_eggs_all");
        refreshAdminWidgets();
        Minecraft.getInstance().getConnection().send(adminAction("GIVE_ALL_SPAWN_EGGS", 0));
    }

    private void sendSaveMaster() {
        if (isProcessing || !canAdmin()) {
            return;
        }
        switch (masterSection) {
            case BALANCE -> sendSaveMasterConfig();
            case REWARDS -> sendSaveMasterRewards();
            case ITEMS -> sendSaveMasterItems();
            case SHOP_ITEMS -> sendSaveMasterShopItems();
            case ETF_ITEMS -> sendSaveMasterEtfItems();
        }
    }

    private void sendSaveMasterConfig() {
        captureMasterTexts();
        Double deathPenalty = parseDouble(deathPenaltyText);
        Double shortSell = parseDouble(shortSellText);
        Integer etfInterval = parseInt(etfIntervalText);
        Integer loanMax = parseInt(loanMaxText);
        Double loanMultiplier = parseDouble(loanMultiplierText);
        if (deathPenalty == null || shortSell == null || etfInterval == null || loanMax == null || loanMultiplier == null) {
            statusMessage = admin("status.invalid_number");
            return;
        }

        isProcessing = true;
        statusMessage = admin("status.saving_master");
        refreshAdminWidgets();
        Minecraft.getInstance().getConnection().send(new EconomyMasterConfigPayload(
                "SAVE",
                deathPenalty,
                shortSell,
                etfInterval,
                loanMax,
                loanMultiplier
        ));
    }

    private void sendSaveMasterRewards() {
        captureVisibleListEdits();
        if (rewardRows.isEmpty() || rewardRows.size() != rewardAmountTexts.size()) {
            return;
        }
        JsonArray edits = new JsonArray();
        for (int i = 0; i < rewardRows.size(); i++) {
            Integer amount = parseInt(rewardAmountTexts.get(i));
            if (amount == null) {
                statusMessage = admin("status.invalid_reward_amount");
                return;
            }
            JsonObject edit = new JsonObject();
            if (isMasterDraftRow(i)) {
                String actionType = draftTextAt(rewardActionTexts, i).trim();
                String displayName = draftTextAt(rewardDisplayTexts, i).trim();
                if (actionType.isEmpty()) {
                    statusMessage = admin("status.need_action_type");
                    return;
                }
                if (displayName.isEmpty()) {
                    statusMessage = admin("status.need_display_name");
                    return;
                }
                edit.addProperty("create", true);
                edit.addProperty("actionType", actionType);
                edit.addProperty("displayName", displayName);
                edit.addProperty("rewardAmount", amount);
            } else {
                edit.addProperty("actionType", rewardRows.get(i).actionType());
                edit.addProperty("rewardAmount", amount);
            }
            edits.add(edit);
        }
        isProcessing = true;
        statusMessage = admin("status.saving_rewards");
        refreshAdminWidgets();
        Minecraft.getInstance().getConnection().send(new EconomyMasterEditPayload("SAVE_REWARDS", edits.toString()));
    }

    private void sendSaveMasterItems() {
        captureVisibleListEdits();
        if (itemRows.isEmpty() || itemRows.size() != itemBuyTexts.size() || itemRows.size() != itemSellTexts.size()) {
            return;
        }
        JsonArray edits = new JsonArray();
        for (int i = 0; i < itemRows.size(); i++) {
            Integer buy = parseOptionalInt(itemBuyTexts.get(i));
            Integer sell = parseOptionalInt(itemSellTexts.get(i));
            if (buy == null && sell == null) {
                statusMessage = admin("status.need_buy_or_sell");
                return;
            }
            if (buy != null && buy < 0) {
                statusMessage = admin("status.invalid_buy");
                return;
            }
            if (sell != null && sell < 0) {
                statusMessage = admin("status.invalid_sell");
                return;
            }
            JsonObject edit = new JsonObject();
            if (isMasterDraftRow(i)) {
                String name = draftTextAt(itemNameTexts, i).trim();
                String itemKey = draftTextAt(itemKeyTexts, i).trim();
                if (name.isEmpty()) {
                    statusMessage = admin("status.need_item_name");
                    return;
                }
                if (itemKey.isEmpty()) {
                    statusMessage = admin("status.need_item_key");
                    return;
                }
                Integer optionalId = parseOptionalInt(draftTextAt(itemIdTexts, i));
                if (optionalId != null) {
                    edit.addProperty("id", optionalId);
                }
                edit.addProperty("create", true);
                edit.addProperty("name", name);
                edit.addProperty("itemKey", itemKey);
                String unit = draftTextAt(itemUnitTexts, i).trim();
                if (!unit.isEmpty()) {
                    edit.addProperty("unit", unit);
                }
            } else {
                edit.addProperty("id", itemRows.get(i).id());
            }
            if (buy != null) {
                edit.addProperty("buyPrice", buy);
            } else {
                edit.add("buyPrice", null);
            }
            if (sell != null) {
                edit.addProperty("sellPrice", sell);
            } else {
                edit.add("sellPrice", null);
            }
            edits.add(edit);
        }
        isProcessing = true;
        statusMessage = admin("status.saving_prices");
        refreshAdminWidgets();
        Minecraft.getInstance().getConnection().send(new EconomyMasterEditPayload("SAVE_ITEMS", edits.toString()));
    }

    private void sendSaveMasterShopItems() {
        captureVisibleListEdits();
        if (shopItemRows.isEmpty()
                || shopItemRows.size() != shopIdTexts.size()
                || shopItemRows.size() != shopDailyLimitTexts.size()
                || shopItemRows.size() != shopUserLimitTexts.size()) {
            return;
        }
        JsonArray edits = new JsonArray();
        for (int i = 0; i < shopItemRows.size(); i++) {
            Integer shopId = parseInt(shopIdTexts.get(i));
            Integer daily = parseOptionalInt(shopDailyLimitTexts.get(i));
            Integer user = parseOptionalInt(shopUserLimitTexts.get(i));
            if (shopId == null) {
                statusMessage = admin("status.need_shop_id");
                return;
            }
            if (!shopRows.isEmpty() && !isKnownShopId(shopId)) {
                statusMessage = admin("status.unknown_shop_id", shopId);
                return;
            }
            if (daily != null && daily < 0) {
                statusMessage = admin("status.invalid_daily");
                return;
            }
            if (user != null && user < 0) {
                statusMessage = admin("status.invalid_user_limit");
                return;
            }
            JsonObject edit = new JsonObject();
            if (isMasterDraftRow(i)) {
                Integer itemId = parseInt(draftTextAt(shopItemRefTexts, i));
                if (itemId == null) {
                    statusMessage = admin("status.need_item_id");
                    return;
                }
                Integer optionalId = parseOptionalInt(draftTextAt(shopItemIdTexts, i));
                if (optionalId != null) {
                    edit.addProperty("id", optionalId);
                }
                Integer orderNo = parseOptionalInt(draftTextAt(shopOrderTexts, i));
                if (orderNo != null) {
                    edit.addProperty("orderNo", orderNo);
                }
                edit.addProperty("create", true);
                edit.addProperty("shopId", shopId);
                edit.addProperty("itemId", itemId);
            } else {
                edit.addProperty("id", shopItemRows.get(i).id());
                if (shopId != shopItemRows.get(i).shopId()) {
                    edit.addProperty("shopId", shopId);
                }
            }
            if (daily != null) {
                edit.addProperty("dailyLimit", daily);
            } else {
                edit.add("dailyLimit", null);
            }
            if (user != null) {
                edit.addProperty("userLimit", user);
            } else {
                edit.add("userLimit", null);
            }
            edits.add(edit);
        }
        isProcessing = true;
        statusMessage = admin("status.saving_shop_items");
        refreshAdminWidgets();
        Minecraft.getInstance().getConnection().send(new EconomyMasterEditPayload("SAVE_SHOP_ITEMS", edits.toString()));
    }

    private void sendSaveMasterEtfItems() {
        captureVisibleListEdits();
        if (etfItemRows.isEmpty() || etfItemRows.size() != etfWeightTexts.size()) {
            return;
        }
        JsonArray edits = new JsonArray();
        for (int i = 0; i < etfItemRows.size(); i++) {
            Double weight = parseDouble(etfWeightTexts.get(i));
            if (weight == null || weight < 0.0) {
                statusMessage = admin("status.invalid_etf_weight");
                return;
            }
            JsonObject edit = new JsonObject();
            if (isMasterDraftRow(i)) {
                String etfCode = draftTextAt(etfCodeTexts, i).trim();
                String itemKey = draftTextAt(etfItemKeyTexts, i).trim();
                if (etfCode.isEmpty()) {
                    statusMessage = admin("status.need_etf_code");
                    return;
                }
                if (itemKey.isEmpty()) {
                    statusMessage = admin("status.need_item_key");
                    return;
                }
                edit.addProperty("create", true);
                edit.addProperty("etfCode", etfCode);
                edit.addProperty("itemKey", itemKey);
                edit.addProperty("influenceWeight", weight);
            } else {
                edit.addProperty("etfCode", etfItemRows.get(i).etfCode());
                edit.addProperty("itemKey", etfItemRows.get(i).itemKey());
                edit.addProperty("influenceWeight", weight);
            }
            edits.add(edit);
        }
        isProcessing = true;
        statusMessage = admin("status.saving_etf");
        refreshAdminWidgets();
        Minecraft.getInstance().getConnection().send(new EconomyMasterEditPayload("SAVE_ETF_ITEMS", edits.toString()));
    }

    private void sendResetMasterConfig() {
        if (isProcessing || !canAdmin()) {
            return;
        }
        isProcessing = true;
        statusMessage = admin("status.resetting_bundled");
        refreshAdminWidgets();
        Minecraft.getInstance().getConnection().send(new EconomyMasterConfigPayload(
                "RESET_OVERRIDE", 0, 0, 0, 0, 0));
    }

    private static Double parseDouble(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseOptionalInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parseInt(raw);
    }

    public void onActionResult(boolean success, String message) {
        isProcessing = false;
        statusMessage = message;
        if (success) {
            if (activeTab == Tab.BALANCE) {
                requestPlayerBalances();
            } else if (activeTab == Tab.MASTER) {
                if (masterSection == MasterSection.BALANCE) {
                    requestMasterConfig();
                } else {
                    requestMasterList();
                }
            } else {
                refreshAdminWidgets();
            }
        } else {
            refreshAdminWidgets();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractTransparentBackground(graphics);

        int panelTop = panelTop();
        int panelLeft = panelLeft();
        int panelRight = panelLeft + PANEL_WIDTH;
        int panelBottom = panelTop + panelHeight();

        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xCC101418);
        graphics.fill(panelLeft, panelTop, panelRight, panelTop + 1, 0xFFDFB323);
        graphics.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, 0xFF3A3A3A);
        graphics.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, 0xFF3A3A3A);
        graphics.fill(panelRight - 1, panelTop, panelRight, panelBottom, 0xFF3A3A3A);

        graphics.centeredText(font, title, width / 2, panelTop + 4, 0xFFFFFF);

        if (activeTab == Tab.BALANCE) {
            renderBalancePanel(graphics, panelLeft);
        } else if (activeTab == Tab.MASTER) {
            renderMasterPanel(graphics, panelLeft);
        } else if (activeTab == Tab.SPAWN) {
            renderSpawnPanel(graphics, panelLeft);
        }

        if (!statusMessage.isEmpty() && activeTab != Tab.MASTER && activeTab != Tab.SPAWN) {
            graphics.centeredText(font, statusMessage, width / 2, footerTop() - 12, 0xCCCCCC);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderBalancePanel(GuiGraphicsExtractor graphics, int panelLeft) {
        int textLeft = panelLeft + 16;
        int top = contentTop();
        graphics.text(font, "§f" + admin("header.balance_list"), textLeft, top, 0xFFFFFFFF, true);

        if (isLoadingBalances) {
            graphics.text(font, admin("status.loading"), textLeft, top + 14, 0xFFAAAAAA, true);
            return;
        }

        if (playerBalances.isEmpty()) {
            graphics.text(font, admin("empty.no_users"), textLeft, top + 14, 0xFFAAAAAA, true);
            return;
        }

        int start = balancePage * PLAYERS_PER_PAGE;
        int end = Math.min(start + PLAYERS_PER_PAGE, playerBalances.size());
        int y = top + 14;
        for (int i = start; i < end; i++) {
            PlayerBalanceRow row = playerBalances.get(i);
            String status = row.active() ? admin("status.online") : admin("status.offline");
            String name = row.username() == null || row.username().isBlank() ? "-" : row.username();
            graphics.text(font, status + " §f" + name, textLeft, y, 0xFFFFFFFF, true);
            String detail = admin("detail.balance",
                    YEN_FORMAT.format(row.balance()),
                    YEN_FORMAT.format(row.bankBalance()),
                    YEN_FORMAT.format(row.debt()));
            graphics.text(font, "§7  " + detail, textLeft, y + 9, 0xFFCCCCCC, true);
            y += 20;
        }

        int maxPage = Math.max(0, (playerBalances.size() - 1) / PLAYERS_PER_PAGE);
        graphics.centeredText(font, (balancePage + 1) + " / " + (maxPage + 1), panelLeft + PANEL_WIDTH / 2, footerTop() - 10, 0xFFAAAAAA);
    }

    private void renderSpawnPanel(GuiGraphicsExtractor graphics, int panelLeft) {
        int textLeft = panelLeft + 16;
        int top = contentTop();
        graphics.text(font, "§f" + admin("header.spawn_eggs"), textLeft, top, 0xFFFFFFFF, true);
        if (!statusMessage.isEmpty() && statusMessage.startsWith("§c")) {
            graphics.text(font, statusMessage, textLeft, top + 12, 0xFFFF5555, true);
        } else if (isLoadingShops) {
            graphics.text(font, admin("status.loading"), textLeft, top + 12, 0xFFAAAAAA, true);
        } else if (!shopRows.isEmpty()) {
            graphics.text(font, admin("hint.spawn_count", shopRows.size()), textLeft, top + 12, 0xFFAAAAAA, true);
        } else {
            graphics.text(font, admin("hint.spawn_help"), textLeft, top + 12, 0xFFAAAAAA, true);
        }

        if (isLoadingShops) {
            return;
        }
        if (shopRows.isEmpty()) {
            graphics.text(font, admin("empty.no_shops"), textLeft, spawnListTop() + 6, 0xFFAAAAAA, true);
            return;
        }

        int start = spawnScrollOffset;
        int end = Math.min(shopRows.size(), start + spawnVisibleRows());
        for (int i = start; i < end; i++) {
            ShopRow shop = shopRows.get(i);
            int rowY = spawnListRowY(i - start);
            String localizedShop = EconomyMasterI18n.shopName(shop.id(), shop.shopName());
            String line = "§f" + truncate(localizedShop, 10) + " §7[" + npcTypeLabel(shop.npcType()) + "] §8#" + shop.id();
            graphics.text(font, line, textLeft, rowY + 6, 0xFFFFFFFF, true);
        }
        if (maxSpawnScrollOffset() > 0) {
            graphics.centeredText(font, "§8" + (start + 1) + "-" + end + " / " + shopRows.size(),
                    panelLeft + PANEL_WIDTH / 2, spawnScrollIndicatorY(), 0xFFAAAAAA);
        } else if (!isProcessing && statusMessage.startsWith("§a")) {
            graphics.centeredText(font, statusMessage, panelLeft + PANEL_WIDTH / 2, spawnScrollIndicatorY(), 0xFFAAAAAA);
        }
    }

    private void renderMasterPanel(GuiGraphicsExtractor graphics, int panelLeft) {
        int textLeft = panelLeft + 16;

        String hintText = !statusMessage.isEmpty()
                ? statusMessage
                : isLoadingMasterConfig || isLoadingMasterList
                ? admin("status.reading")
                : !masterSourceHint.isEmpty()
                ? "§7" + masterSourceHint
                : "§7config/economy/economy_master.json";
        graphics.text(font, hintText, textLeft, masterHintTop(), 0xFFAAAAAA, true);

        switch (masterSection) {
            case BALANCE -> renderMasterBalanceLabels(graphics, textLeft);
            case REWARDS -> renderMasterRewardRows(graphics, textLeft);
            case ITEMS -> renderMasterItemRows(graphics, textLeft);
            case SHOP_ITEMS -> renderMasterShopItemRows(graphics, textLeft);
            case ETF_ITEMS -> renderMasterEtfItemRows(graphics, textLeft);
        }
    }

    private void renderMasterBalanceLabels(GuiGraphicsExtractor graphics, int textLeft) {
        String[] labels = {
                admin("label.death_penalty"),
                admin("label.short_sell"),
                admin("label.etf_interval"),
                admin("label.loan_max"),
                admin("label.loan_multiplier")
        };
        int start = masterScrollOffset;
        int end = Math.min(MASTER_FIELD_COUNT, start + masterVisibleBalanceRows());
        for (int i = start; i < end; i++) {
            int visibleRow = i - start;
            renderMasterLabel(graphics, textLeft, masterBalanceRowY(visibleRow), labels[i]);
        }
    }

    private void renderMasterRewardRows(GuiGraphicsExtractor graphics, int textLeft) {
        int y = masterListContentTop();
        if (isLoadingMasterList) {
            graphics.text(font, admin("status.loading"), textLeft, y + 6, 0xFFAAAAAA, true);
            return;
        }
        if (rewardRows.isEmpty() && masterDraftRows.isEmpty()) {
            graphics.text(font, admin("empty.no_rewards"), textLeft, y + 6, 0xFFAAAAAA, true);
            return;
        }
        int start = masterScrollOffset;
        int end = Math.min(rewardRows.size(), start + masterVisibleListRows());
        for (int i = start; i < end; i++) {
            int visibleRow = i - start;
            RewardRow row = rewardRows.get(i);
            int rowY = masterListRowY(visibleRow);
            if (isMasterDraftRow(i)) {
                graphics.text(font, admin("status.draft_new"), textLeft, rowY + 6, 0xFFFFFF55, true);
            } else {
                graphics.text(font, "§f" + truncate(
                        EconomyMasterI18n.rewardName(row.actionType(), row.displayName()), 18),
                        textLeft, rowY + 6, 0xFFFFFFFF, true);
                graphics.text(font, "§7" + admin("header.amount"), textLeft + 152, rowY + 6, 0xFFAAAAAA, true);
            }
        }
    }

    private void renderMasterItemRows(GuiGraphicsExtractor graphics, int textLeft) {
        int headerY = masterListContentTop() - 10;
        int y = masterListContentTop();
        if (isLoadingMasterList) {
            graphics.text(font, admin("status.loading"), textLeft, y + 6, 0xFFAAAAAA, true);
            return;
        }
        if (itemRows.isEmpty() && masterDraftRows.isEmpty()) {
            graphics.text(font, admin("empty.no_items"), textLeft, y + 6, 0xFFAAAAAA, true);
            return;
        }
        graphics.text(font, "§7" + admin("header.item"), textLeft, headerY, 0xFFAAAAAA, true);
        graphics.text(font, "§7" + admin("header.buy"), textLeft + 188, headerY, 0xFFAAAAAA, true);
        graphics.text(font, "§7" + admin("header.sell"), textLeft + 246, headerY, 0xFFAAAAAA, true);
        int start = masterScrollOffset;
        int end = Math.min(itemRows.size(), start + masterVisibleListRows());
        for (int i = start; i < end; i++) {
            int visibleRow = i - start;
            ItemPriceRow row = itemRows.get(i);
            int rowY = masterListRowY(visibleRow) + 6;
            if (isMasterDraftRow(i)) {
                graphics.text(font, admin("status.draft_new"), textLeft, rowY, 0xFFFFFF55, true);
            } else {
                graphics.text(font, "§f" + truncate(
                        EconomyMasterI18n.itemName(
                                row.itemKey(), row.name(), row.matchPotion(), row.matchEnchantment()), 18),
                        textLeft, rowY, 0xFFFFFFFF, true);
            }
        }
    }

    private void renderMasterShopItemRows(GuiGraphicsExtractor graphics, int textLeft) {
        int headerY = masterListContentTop() - 10;
        int y = masterListContentTop();
        if (isLoadingMasterList) {
            graphics.text(font, admin("status.loading"), textLeft, y + 6, 0xFFAAAAAA, true);
            return;
        }
        if (shopItemRows.isEmpty() && masterDraftRows.isEmpty()) {
            graphics.text(font, admin("empty.no_shop_items"), textLeft, y + 6, 0xFFAAAAAA, true);
            return;
        }
        graphics.text(font, "§7" + admin("header.item"), textLeft, headerY, 0xFFAAAAAA, true);
        graphics.text(font, "§7" + admin("header.npc_shop"), textLeft + SHOP_COL_NPC, headerY, 0xFFAAAAAA, true);
        graphics.text(font, "§7" + admin("header.daily"), textLeft + SHOP_COL_DAILY, headerY, 0xFFAAAAAA, true);
        graphics.text(font, "§7" + admin("header.user"), textLeft + SHOP_COL_USER, headerY, 0xFFAAAAAA, true);
        int start = masterScrollOffset;
        int end = Math.min(shopItemRows.size(), start + masterVisibleListRows());
        for (int i = start; i < end; i++) {
            int visibleRow = i - start;
            ShopItemRow row = shopItemRows.get(i);
            int rowY = masterListRowY(visibleRow) + 6;
            if (isMasterDraftRow(i)) {
                graphics.text(font, admin("status.draft_new"), textLeft, rowY, 0xFFFFFF55, true);
            } else {
                graphics.text(font, "§f" + truncate(
                        EconomyMasterI18n.itemName(row.itemKey(), row.itemName()), SHOP_COL_ITEM_MAX_CHARS),
                        textLeft, rowY, 0xFFFFFFFF, true);
            }
            Integer shopId = parseInt(shopIdTextAt(i));
            int resolvedShopId = shopId != null ? shopId : row.shopId();
            if (!shopRows.isEmpty()) {
                graphics.text(font, "§7" + shopDisplayNameFitting(resolvedShopId),
                        textLeft + SHOP_COL_NPC_LABEL, rowY, 0xFFAAAAAA, true);
            }
        }
    }

    private void renderMasterEtfItemRows(GuiGraphicsExtractor graphics, int textLeft) {
        int headerY = masterListContentTop() - 10;
        int y = masterListContentTop();
        if (isLoadingMasterList) {
            graphics.text(font, admin("status.loading"), textLeft, y + 6, 0xFFAAAAAA, true);
            return;
        }
        if (etfItemRows.isEmpty() && masterDraftRows.isEmpty()) {
            graphics.text(font, admin("empty.no_etf_items"), textLeft, y + 6, 0xFFAAAAAA, true);
            return;
        }
        graphics.text(font, "§7" + admin("header.etf"), textLeft, headerY, 0xFFAAAAAA, true);
        graphics.text(font, "§7" + admin("header.item"), textLeft + 120, headerY, 0xFFAAAAAA, true);
        graphics.text(font, "§7" + admin("header.weight"), textLeft + 200, headerY, 0xFFAAAAAA, true);
        int start = masterScrollOffset;
        int end = Math.min(etfItemRows.size(), start + masterVisibleListRows());
        for (int i = start; i < end; i++) {
            int visibleRow = i - start;
            EtfItemRow row = etfItemRows.get(i);
            int rowY = masterListRowY(visibleRow) + 6;
            if (isMasterDraftRow(i)) {
                graphics.text(font, admin("status.draft_new"), textLeft, rowY, 0xFFFFFF55, true);
            } else {
                graphics.text(font, "§7" + truncate(
                        EconomyMasterI18n.etfName(row.etfCode(), row.etfCode()), 10),
                        textLeft, rowY, 0xFFAAAAAA, true);
                graphics.text(font, "§f" + truncate(
                        EconomyMasterI18n.itemName(row.itemKey(), row.itemName()), 12),
                        textLeft + 120, rowY, 0xFFFFFFFF, true);
            }
        }
    }

    private static String optionalJsonString(JsonObject row, String key) {
        if (!row.has(key) || row.get(key).isJsonNull()) {
            return null;
        }
        String value = row.get(key).getAsString();
        return value == null || value.isBlank() ? null : value;
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 1) + "…";
    }

    private void renderMasterLabel(GuiGraphicsExtractor graphics, int x, int y, String label) {
        graphics.text(font, "§7" + label, x, y + 5, 0xFFCCCCCC, true);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (activeTab == Tab.ADMIN && !isProcessing && maxAdminScrollOffset() > 0) {
            int left = panelLeft();
            int top = adminListTop();
            int right = left + PANEL_WIDTH;
            int bottom = adminListScrollBottom();
            if (mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom) {
                scrollAdminList(scrollY > 0 ? -ADMIN_SCROLL_STEP : ADMIN_SCROLL_STEP);
                return true;
            }
        }
        if (activeTab == Tab.SPAWN && !isProcessing && !isLoadingShops && maxSpawnScrollOffset() > 0) {
            int left = panelLeft();
            int top = spawnListTop();
            int right = left + PANEL_WIDTH;
            int bottom = spawnListScrollBottom();
            if (mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom) {
                scrollSpawnList(scrollY > 0 ? -MASTER_SCROLL_STEP : MASTER_SCROLL_STEP);
                return true;
            }
        }
        if (activeTab != Tab.MASTER || isProcessing || isLoadingMasterConfig || isLoadingMasterList) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (maxMasterScrollOffset() <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int left = panelLeft();
        int top = masterSection == MasterSection.BALANCE ? masterFieldsTop() : masterListContentTop();
        int right = left + PANEL_WIDTH;
        int bottom = masterListScrollBottom();
        if (mouseX < left || mouseX >= right || mouseY < top || mouseY >= bottom) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int step = scrollY > 0 ? -MASTER_SCROLL_STEP : MASTER_SCROLL_STEP;
        scrollMasterList(step);
        return true;
    }
}
