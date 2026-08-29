package com.ogatamizuki.economy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class ShopScreen extends Screen {
    private final int shopId;
    private String npcType;
    private String shopName = "";

    private final List<ShopItemData> items = new ArrayList<>();
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 5;

    private boolean isLoading = true;
    private boolean isProcessing = false; // 購入/売却リクエスト処理中フラグ（連打防止）
    private long processingStartedAtMs = 0L;
    private static final long PROCESSING_TIMEOUT_MS = 10_000L;
    private String statusMessage = "";
    private int cachedTradeStateSignature = Integer.MIN_VALUE;
    /** 買取NPC向け: true のとき所持数が1以上のアイテムだけ表示 */
    private boolean showOwnedOnly = true;

    // 内部データクラス
    private static class ShopItemData {
        public final Integer shopItemId; // master_shop_item.id (BUYERの場合はnull)
        public final Integer itemId; // master item id（売却特定用）
        public final String name;
        public final String itemKey;
        public final String matchPotion;
        public final String matchEnchantment;
        public final Integer matchEnchantmentLevel;
        public final int buyPrice;
        public final int sellPrice;
        public final Integer userLimit;
        public final Integer dailyLimit;
        public Integer remainingUserLimit;
        public Integer remainingDailyLimit;

        public ShopItemData(JsonObject obj) {
            this.shopItemId = obj.has("shop_item_id") && !obj.get("shop_item_id").isJsonNull()
                    ? obj.get("shop_item_id").getAsInt()
                    : null;
            this.itemId = obj.has("item_id") && !obj.get("item_id").isJsonNull()
                    ? obj.get("item_id").getAsInt()
                    : (obj.has("order_no") && !obj.get("order_no").isJsonNull() ? obj.get("order_no").getAsInt()
                            : null);
            this.name = obj.get("item_name").getAsString();
            this.itemKey = obj.get("item_key").getAsString();
            this.matchPotion = optionalString(obj, "match_potion");
            this.matchEnchantment = optionalString(obj, "match_enchantment");
            this.matchEnchantmentLevel = obj.has("match_enchantment_level")
                    && !obj.get("match_enchantment_level").isJsonNull()
                            ? obj.get("match_enchantment_level").getAsInt()
                            : null;
            this.buyPrice = jsonIntOrZero(obj, "buy_price");
            this.sellPrice = jsonIntOrZero(obj, "sell_price");

            this.userLimit = obj.has("user_limit") && !obj.get("user_limit").isJsonNull()
                    ? obj.get("user_limit").getAsInt()
                    : null;
            this.dailyLimit = obj.has("daily_limit") && !obj.get("daily_limit").isJsonNull()
                    ? obj.get("daily_limit").getAsInt()
                    : null;

            this.remainingUserLimit = obj.has("remaining_user_limit") && !obj.get("remaining_user_limit").isJsonNull()
                    ? obj.get("remaining_user_limit").getAsInt()
                    : null;
            this.remainingDailyLimit = obj.has("remaining_daily_limit")
                    && !obj.get("remaining_daily_limit").isJsonNull() ? obj.get("remaining_daily_limit").getAsInt()
                            : null;
        }

        String displayName() {
            return EconomyMasterI18n.itemName(itemKey, name, matchPotion, matchEnchantment);
        }

        ItemStack iconStack() {
            return ShopItemDisplayStacks.create(itemKey, matchPotion, matchEnchantment, matchEnchantmentLevel);
        }

        private static String optionalString(JsonObject obj, String key) {
            if (!obj.has(key) || obj.get(key).isJsonNull()) {
                return null;
            }
            String value = obj.get(key).getAsString();
            return value == null || value.isBlank() ? null : value;
        }

        private static int jsonIntOrZero(JsonObject obj, String key) {
            if (!obj.has(key) || obj.get(key).isJsonNull()) {
                return 0;
            }
            return obj.get(key).getAsInt();
        }
    }

    public ShopScreen(int shopId, String npcType) {
        super(Component.literal("SHOP"));
        this.shopId = shopId;
        this.npcType = npcType;
        this.shopName = EconomyMasterI18n.trs("economy.ui.shop_default");
    }

    @Override
    protected void init() {
        super.init();
        refreshShopData(true);
    }

    private void refreshShopData(boolean showLoading) {
        if (showLoading) {
            this.isLoading = true;
            this.statusMessage = "データを読み込んでいます...";
            this.clearWidgets();
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;
        String playerUuid = mc.player.getUUID().toString();

        EconomyService.fetchShopDetails(this.shopId, playerUuid).thenAccept(response -> {
            if (response == null) {
                if (showLoading) {
                    Minecraft.getInstance().execute(() -> {
                        this.statusMessage = "ショップデータの取得に失敗しました。";
                        this.isLoading = false;
                        setupNavigationButtons();
                    });
                } else {
                    Minecraft.getInstance().execute(this::rebuildWidgets);
                }
                return;
            }

            try {
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                String shopNameParsed = json.get("shopName").getAsString();
                String npcTypeParsed = json.has("npcType") ? json.get("npcType").getAsString() : null;
                JsonArray itemsArray = json.getAsJsonArray("items");

                List<ShopItemData> parsedItems = new ArrayList<>();
                for (JsonElement element : itemsArray) {
                    parsedItems.add(new ShopItemData(element.getAsJsonObject()));
                }

                Minecraft.getInstance().execute(() -> {
                    this.shopName = EconomyMasterI18n.shopName(this.shopId, shopNameParsed);
                    if (npcTypeParsed != null) {
                        this.npcType = npcTypeParsed;
                    }
                    this.items.clear();
                    this.items.addAll(parsedItems);
                    this.isLoading = false;
                    this.rebuildWidgets();
                });
            } catch (Exception e) {
                EconomyMod.LOGGER.error("Failed to parse shop response: ", e);
                if (showLoading) {
                    Minecraft.getInstance().execute(() -> {
                        this.statusMessage = "データの解析に失敗しました。";
                        this.isLoading = false;
                        setupNavigationButtons();
                    });
                } else {
                    Minecraft.getInstance().execute(this::rebuildWidgets);
                }
            }
        });
    }

    private List<ShopItemData> displayedItems() {
        if (!"BUYER".equals(this.npcType) || !this.showOwnedOnly) {
            return this.items;
        }
        List<ShopItemData> filtered = new ArrayList<>();
        for (ShopItemData item : this.items) {
            if (item.sellPrice > 0 && getMatchingItemCount(item) > 0) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private boolean canBuy(ShopItemData item, int quantity) {
        if (isProcessing || item.shopItemId == null || item.buyPrice <= 0)
            return false;
        if (EconomyMod.getCurrentBalance() < item.buyPrice * (long) quantity)
            return false;
        if (item.remainingUserLimit != null && item.remainingUserLimit < quantity)
            return false;
        if (item.remainingDailyLimit != null && item.remainingDailyLimit < quantity)
            return false;
        return true;
    }

    private boolean canSell(ShopItemData item, int quantity) {
        if (isProcessing || item.sellPrice <= 0 || item.itemId == null)
            return false;
        return getMatchingItemCount(item) >= quantity;
    }

    /** 表示中アイテムの所持数・残高・購入枠をまとめた署名（インベントリ同期後のボタン更新用） */
    private int computeTradeStateSignature() {
        int sig = EconomyMod.getCurrentBalance();
        sig = 31 * sig + (this.showOwnedOnly ? 1 : 0);
        List<ShopItemData> visible = displayedItems();
        int startIdx = this.currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, visible.size());
        for (int i = startIdx; i < endIdx; i++) {
            ShopItemData item = visible.get(i);
            sig = 31 * sig + getMatchingItemCount(item);
            if (item.remainingUserLimit != null) {
                sig = 31 * sig + item.remainingUserLimit;
            }
            if (item.remainingDailyLimit != null) {
                sig = 31 * sig + item.remainingDailyLimit;
            }
        }
        sig = 31 * sig + visible.size();
        return sig;
    }

    private void syncTradeStateSignature() {
        this.cachedTradeStateSignature = computeTradeStateSignature();
    }

    private void refreshTradeButtonStates() {
        if (this.isLoading)
            return;
        rebuildWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isProcessing
                && this.processingStartedAtMs > 0
                && System.currentTimeMillis() - this.processingStartedAtMs > PROCESSING_TIMEOUT_MS) {
            this.isProcessing = false;
            this.processingStartedAtMs = 0L;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(EconomyMasterI18n.tr("economy.chat.shop_timeout"));
            }
            refreshTradeButtonStates();
            return;
        }
        if (this.isLoading || this.isProcessing)
            return;
        int sig = computeTradeStateSignature();
        if (sig != this.cachedTradeStateSignature) {
            refreshTradeButtonStates();
        }
    }

    @Override
    protected void rebuildWidgets() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (this.isLoading) {
            setupNavigationButtons();
            return;
        }

        // ページ範囲チェック
        List<ShopItemData> visible = displayedItems();
        int maxPage = Math.max(0, (visible.size() - 1) / ITEMS_PER_PAGE);
        if (this.currentPage > maxPage)
            this.currentPage = maxPage;

        int startIdx = this.currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, visible.size());

        for (int i = startIdx; i < endIdx; i++) {
            ShopItemData item = visible.get(i);
            int rowY = centerY - 52 + (i - startIdx) * 24;

            if ("BUYER".equals(this.npcType)) {
                // 買取所（質屋）：売却ボタンを配置 (X座標を右寄せに調整)
                if (item.sellPrice > 0) {
                    Button sell1Btn = Button
                            .builder(EconomyMasterI18n.tr("economy.ui.sell_1"), button -> handleSell(item, 1))
                            .bounds(centerX + 65, rowY - 1, 42, 16).build();
                    Button sell16Btn = Button
                            .builder(EconomyMasterI18n.tr("economy.ui.sell_16"), button -> handleSell(item, 16))
                            .bounds(centerX + 112, rowY - 1, 45, 16).build();

                    sell1Btn.active = canSell(item, 1);
                    sell16Btn.active = canSell(item, 16);

                    this.addRenderableWidget(sell1Btn);
                    this.addRenderableWidget(sell16Btn);
                }
            } else {
                // 販売所（SELLER）：購入ボタンを配置 (X座標を右寄せに調整)
                if (item.buyPrice > 0) {
                    Button buy1Btn = Button
                            .builder(EconomyMasterI18n.tr("economy.ui.buy_1"), button -> handleBuy(item, 1))
                            .bounds(centerX + 65, rowY - 1, 42, 16).build();
                    Button buy16Btn = Button
                            .builder(EconomyMasterI18n.tr("economy.ui.buy_16"), button -> handleBuy(item, 16))
                            .bounds(centerX + 112, rowY - 1, 45, 16).build();

                    buy1Btn.active = canBuy(item, 1);
                    buy16Btn.active = canBuy(item, 16);

                    this.addRenderableWidget(buy1Btn);
                    this.addRenderableWidget(buy16Btn);
                }
            }
        }

        setupNavigationButtons();
        syncTradeStateSignature();
    }

    private void setupNavigationButtons() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelCenterX = centerX + 15; // 補正後のパネル中心

        if ("BUYER".equals(this.npcType) && !this.isLoading) {
            String filterLabel = this.showOwnedOnly
                    ? EconomyMasterI18n.trs("economy.ui.filter_owned_on")
                    : EconomyMasterI18n.trs("economy.ui.filter_owned_off");
            this.addRenderableWidget(Button.builder(Component.literal(filterLabel), button -> {
                this.showOwnedOnly = !this.showOwnedOnly;
                this.currentPage = 0;
                rebuildWidgets();
            }).bounds(panelCenterX - 110, centerY - 70, 88, 14).build());
        }

        List<ShopItemData> visible = displayedItems();
        // ページ切り替えボタン（アイテム数がある場合）
        if (visible.size() > ITEMS_PER_PAGE) {
            Button prevBtn = Button.builder(EconomyMasterI18n.tr("economy.ui.prev"), button -> {
                if (this.currentPage > 0) {
                    this.currentPage--;
                    rebuildWidgets();
                }
            }).bounds(panelCenterX - 110, centerY + 96, 45, 20).build();

            Button nextBtn = Button.builder(EconomyMasterI18n.tr("economy.ui.next"), button -> {
                int maxPage = (visible.size() - 1) / ITEMS_PER_PAGE;
                if (this.currentPage < maxPage) {
                    this.currentPage++;
                    rebuildWidgets();
                }
            }).bounds(panelCenterX + 65, centerY + 96, 45, 20).build();

            prevBtn.active = this.currentPage > 0;
            nextBtn.active = this.currentPage < (visible.size() - 1) / ITEMS_PER_PAGE;

            this.addRenderableWidget(prevBtn);
            this.addRenderableWidget(nextBtn);
        }

        // 戻る/閉じるボタン
        this.addRenderableWidget(Button.builder(EconomyMasterI18n.tr("economy.ui.close"), button -> this.onClose())
                .bounds(panelCenterX - 40, centerY + 96, 80, 20).build());
    }

    private void handleBuy(ShopItemData item, int quantity) {
        if (!canBuy(item, quantity))
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || item.shopItemId == null)
            return;
        if (mc.getConnection() == null)
            return;

        // 通信中ロック
        isProcessing = true;
        processingStartedAtMs = System.currentTimeMillis();
        rebuildWidgets();

        // サーバーに購入パケットを送信（サーバー側でAPI呼び出し＆インベントリ操作）
        mc.getConnection().send(new ShopBuyRequestPayload(item.shopItemId, quantity));
    }

    private void handleSell(ShopItemData item, int quantity) {
        if (!canSell(item, quantity))
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || item.itemId == null || mc.getConnection() == null)
            return;

        // 通信中ロック
        isProcessing = true;
        processingStartedAtMs = System.currentTimeMillis();
        rebuildWidgets();

        mc.getConnection().send(new ShopSellRequestPayload(item.itemId, quantity));
    }

    /**
     * ショップ取引が成功した際にサーバーから通知されるコールバック。
     * ShopTxResultPayload（success=true）を受信したEconomyModClientから呼び出される。
     */
    public void onTransactionSuccess() {
        isProcessing = false;
        processingStartedAtMs = 0L;
        refreshTradeButtonStates(); // 所持数・残高でボタンを即時再評価
        refreshShopData(false); // ローディング表示なしで制限数を再取得
    }

    /**
     * ショップ取引が失敗した際にサーバーから通知されるコールバック。
     * ShopTxResultPayload（success=false）を受信したEconomyModClientから呼び出される。
     */
    public void onTransactionFailed() {
        isProcessing = false;
        processingStartedAtMs = 0L;
        refreshTradeButtonStates();
    }

    private void drawMinecraftBevel(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2, boolean sunken) {
        int topLeftColor, bottomRightColor, bgColor;
        if (sunken) {
            // 凹んだスロット（シャープで高級感のあるダークインセット）
            topLeftColor = 0xFF1F1F1F; // 暗い影
            bottomRightColor = 0xFF4A4A4A; // 明るいエッジ
            bgColor = 0xFF161616; // 深い背景色
        } else {
            // パネル外枠（マイクラ風ダークテーマのベース）
            topLeftColor = 0xFF5F5F5F; // 上・左のハイライト
            bottomRightColor = 0xFF1F1F1F; // 下・右のシャドウ
            bgColor = 0xFF2A2A2A; // メインのダークグレー背景
        }

        // 背景
        gui.fill(x1, y1, x2, y2, bgColor);
        // 上枠
        gui.fill(x1, y1, x2, y1 + 1, topLeftColor);
        // 左枠
        gui.fill(x1, y1, x1 + 1, y2, topLeftColor);
        // 下枠
        gui.fill(x1, y2 - 1, x2, y2, bottomRightColor);
        // 右枠
        gui.fill(x2 - 1, y1, x2, y2, bottomRightColor);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 背景のダークオーバーレイを描画
        this.extractTransparentBackground(guiGraphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // メインパネル外枠（幅 300, 高さ 212）
        drawMinecraftBevel(guiGraphics, centerX - 135, centerY - 92, centerX + 165, centerY + 120, false);
        // メインパネルにスタイリッシュな外枠ゴールドライン（上部エッジ）
        guiGraphics.fill(centerX - 134, centerY - 91, centerX + 164, centerY - 90, 0xFFDFB323);

        // 所持金情報
        String playerMoneyText = EconomyMasterI18n
                .tr("economy.ui.balance", EconomyMasterI18n.formatCurrency(EconomyMod.getCurrentBalance())).getString();

        // ヘッダータイトル（凹んだテキストスロット風）
        drawMinecraftBevel(guiGraphics, centerX - 130, centerY - 87, centerX + 160, centerY - 69, true);
        // ヘッダー上部に金色のアクセントライン
        guiGraphics.fill(centerX - 130, centerY - 87, centerX + 160, centerY - 86, 0xFFDFB323);
        guiGraphics.centeredText(this.font, "§e§l" + this.shopName, centerX + 15, centerY - 82, 0xFFFFFFFF); // ゴールド太字

        // 所持金表示（スロット風に凹ませる - 高さを14pxに狭める）
        drawMinecraftBevel(guiGraphics, centerX - 130, centerY + 65, centerX + 160, centerY + 79, true);
        // 所持金スロット下部に黄緑のアクセントライン
        guiGraphics.fill(centerX - 130, centerY + 78, centerX + 160, centerY + 79, 0xFF55FF55);
        guiGraphics.centeredText(this.font, playerMoneyText, centerX + 15, centerY + 68, 0xFF55FF55); // 黄緑色

        if (this.isLoading) {
            guiGraphics.centeredText(this.font, this.statusMessage, centerX + 15, centerY, 0xFFAAAAAA);
            // ウィジェットを最前面に描画するために super は最後に呼び出す
            super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

        if (this.items.isEmpty()) {
            guiGraphics.centeredText(this.font, EconomyMasterI18n.trs("economy.ui.no_products"), centerX + 15, centerY,
                    0xFF888888);
            // ウィジェットを最前面に描画するために super は最後に呼び出す
            super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

        List<ShopItemData> visible = displayedItems();
        if (visible.isEmpty()) {
            String emptyMsg = "BUYER".equals(this.npcType) && this.showOwnedOnly
                    ? EconomyMasterI18n.trs("economy.ui.no_owned_buyback")
                    : EconomyMasterI18n.trs("economy.ui.no_products");
            guiGraphics.centeredText(this.font, emptyMsg, centerX + 15, centerY, 0xFF888888);
            super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

        // 商品一覧の描画
        int startIdx = this.currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, visible.size());

        for (int i = startIdx; i < endIdx; i++) {
            ShopItemData item = visible.get(i);
            int rowY = centerY - 52 + (i - startIdx) * 24;

            // 各商品行の背景枠 (凹んだスロットデザイン - 横幅を右側のボタンの手手前までに縮小)
            // 少し明るめのダークグレー（0xFF1E1E1E）にしてスロット内のレイヤーに差をつける
            drawMinecraftBevel(guiGraphics, centerX - 130, rowY - 3, centerX + 60, rowY + 19, true);
            guiGraphics.fill(centerX - 129, rowY - 2, centerX + 59, rowY + 18, 0xFF202020);

            // 1. アイテムアイコン用のスロット (さらに凹んだ18x18スロット: 背景は漆黒)
            drawMinecraftBevel(guiGraphics, centerX - 128, rowY - 2, centerX - 108, rowY + 16, true);
            guiGraphics.fill(centerX - 127, rowY - 1, centerX - 109, rowY + 15, 0xFF0D0D0D);
            try {
                ItemStack stack = item.iconStack();
                if (!stack.isEmpty()) {
                    guiGraphics.fakeItem(stack, centerX - 127, rowY - 1);
                } else {
                    EconomyMod.LOGGER.warn("Item not found in registry: " + item.itemKey);
                }
            } catch (Exception e) {
                EconomyMod.LOGGER.error("Failed to load item for icon: " + item.itemKey, e);
            }

            // 2. アイテム名称 (1行目: 上部)
            guiGraphics.text(this.font, item.displayName(), centerX - 104, rowY - 1, 0xFFFFFFFF, true);

            // 3. 価格表示 (2行目: 左下)
            if ("BUYER".equals(this.npcType)) {
                guiGraphics.text(this.font,
                        EconomyMasterI18n.tr("economy.ui.price_sell", EconomyMasterI18n.formatCurrency(item.sellPrice))
                                .getString(),
                        centerX - 104, rowY + 9, 0xFFFF5555, true);
            } else {
                guiGraphics.text(this.font,
                        EconomyMasterI18n.tr("economy.ui.price_buy", EconomyMasterI18n.formatCurrency(item.buyPrice))
                                .getString(),
                        centerX - 104, rowY + 9, 0xFF55FF55, true);
            }

            // 4. 所持数の表示 (2行目: 右寄せ)
            int myCount = getMatchingItemCount(item);
            String myCountText = EconomyMasterI18n.tr("economy.ui.owned", myCount).getString();
            int myCountWidth = this.font.width(myCountText);
            int myCountX = centerX + 56 - myCountWidth;
            guiGraphics.text(this.font, myCountText, myCountX, rowY + 9, 0xFFAAAAAA, true);

            // 5. 制限数の表示 (2行目: 所持数の左側)
            if (!"BUYER".equals(this.npcType)) {
                if (item.userLimit != null || item.dailyLimit != null) {
                    String limitText = "";
                    if (item.userLimit != null) {
                        limitText += "残: " + item.remainingUserLimit;
                    }
                    if (item.dailyLimit != null) {
                        limitText += (limitText.isEmpty() ? "" : "/") + "日: " + item.remainingDailyLimit;
                    }
                    int limitWidth = this.font.width(limitText);
                    int limitX = myCountX - 6 - limitWidth;
                    guiGraphics.text(this.font, limitText, limitX, rowY + 9, 0xFFFFAA00, true);
                }
            }
        }

        // ページ数表示
        int maxPage = Math.max(0, (visible.size() - 1) / ITEMS_PER_PAGE);
        String pageStr = (this.currentPage + 1) + " / " + (maxPage + 1);
        int textWidth = this.font.width(pageStr);
        guiGraphics.text(this.font, pageStr, centerX + 15 - textWidth / 2, centerY + 83, 0xFFCCCCCC, true); // 暗い文字から白浮き文字影付きに変更

        // ボタンウィジェットなどの描画（最前面に重ねるためにメソッドの最後に呼ぶ）
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    private int getMatchingItemCount(ShopItemData item) {
        Player player = Minecraft.getInstance().player;
        if (player == null)
            return 0;
        return EconomyItemMatcher.countMatching(
                player,
                item.itemKey,
                item.matchPotion,
                item.matchEnchantment,
                item.matchEnchantmentLevel);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<ShopItemData> visible = displayedItems();
        if (this.isLoading || this.isProcessing || visible.size() <= ITEMS_PER_PAGE) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        if (!isMouseOver(mouseX, mouseY, centerX - 135, centerY - 92, centerX + 165, centerY + 120)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int maxPage = (visible.size() - 1) / ITEMS_PER_PAGE;
        if (scrollY > 0 && this.currentPage > 0) {
            this.currentPage--;
            rebuildWidgets();
            return true;
        }
        if (scrollY < 0 && this.currentPage < maxPage) {
            this.currentPage++;
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static boolean isMouseOver(double mouseX, double mouseY, int x1, int y1, int x2, int y2) {
        return mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2;
    }
}
