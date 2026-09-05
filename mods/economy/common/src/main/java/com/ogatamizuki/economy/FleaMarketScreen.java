package com.ogatamizuki.economy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import com.ogatamizuki.economy.data.FleaMarketStackCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class FleaMarketScreen extends Screen {
    private static final String[] TAB_LABEL_KEYS = { "economy.ui.flea.tab.market", "economy.ui.flea.tab.mine",
            "economy.ui.flea.tab.new" };

    private int activeTab = 0; // 0: 市場一覧, 1: 自分の出品, 2: 新規出品
    private boolean isLoading = true;
    private boolean isProcessing = false;
    private String statusMessage = "データを読み込んでいます...";

    // 取得したフリマ出品データ一覧
    private final List<ListingData> listings = new ArrayList<>();
    // スクロールオフセット（各画面用）
    private int marketScroll = 0;
    private int myScroll = 0;
    private int inventoryScroll = 0;

    // 出品用の選択アイテム
    private InventoryItemData selectedInventoryItem = null;
    private EditBox priceInputBox;
    private EditBox quantityInputBox;

    private static final int ROWS_VISIBLE = 5;
    private static final int TAB_Y_OFFSET = -65;
    private static final int TAB_HEIGHT = 18;
    private static final int ROW_START_Y_OFFSET = -42;
    private static final int ROW_HEIGHT = 22;
    private static final int SCROLL_UP_Y_OFFSET = -42;
    private static final int SCROLL_DOWN_Y_OFFSET = 48;
    private static final int LIST_TEXT_MAX_WIDTH = 210;

    // フリマ出品データ構造
    private static class ListingData {
        final String id;
        final String sellerUuid;
        final String sellerName;
        final String itemKey;
        final String itemName;
        final int price;
        final int remainingQuantity;
        final ItemStack displayStack;

        ListingData(JsonObject obj) {
            this.id = obj.get("id").getAsString();
            this.sellerUuid = obj.get("sellerUuid").getAsString();
            this.sellerName = obj.get("sellerName").getAsString();
            this.itemKey = obj.get("itemKey").getAsString();
            this.itemName = obj.get("itemName").getAsString();
            this.price = obj.get("price").getAsInt();
            this.remainingQuantity = obj.get("remainingQuantity").getAsInt();
            String stackNbt = obj.has("itemStackNbt") ? obj.get("itemStackNbt").getAsString() : "";
            Minecraft mc = Minecraft.getInstance();
            var registries = mc.level != null ? mc.level.registryAccess()
                    : mc.player != null
                            ? mc.player.registryAccess()
                            : null;
            if (registries != null) {
                this.displayStack = com.ogatamizuki.economy.data.FleaMarketStackCodec.decode(
                        registries, stackNbt, this.itemKey, 1);
            } else {
                this.displayStack = com.ogatamizuki.economy.data.FleaMarketStackCodec.fromItemKey(this.itemKey, 1);
            }
        }
    }

    // プレイヤーインベントリ内のアイテム情報（同一 components で集計）
    private static class InventoryItemData {
        final String itemKey;
        final String name;
        final int count;
        final ItemStack sample;

        InventoryItemData(ItemStack sample, int count) {
            this.sample = sample.copyWithCount(1);
            this.itemKey = BuiltInRegistries.ITEM.getKey(sample.getItem()).toString();
            this.name = sample.getHoverName().getString();
            this.count = count;
        }
    }

    public FleaMarketScreen() {
        super(EconomyMasterI18n.tr("economy.ui.flea.title"));
    }

    @Override
    protected void init() {
        super.init();
        refreshFleaMarketData(true);
    }

    private void refreshFleaMarketData(boolean showLoading) {
        if (showLoading) {
            this.isLoading = true;
            this.statusMessage = EconomyMasterI18n.trs("economy.ui.flea.status.loading");
            this.clearWidgets();
        }

        EconomyService.fetchFleaMarketListings().thenAccept(response -> {
            if (response == null) {
                Minecraft.getInstance().execute(() -> {
                    this.statusMessage = EconomyMasterI18n.trs("economy.ui.flea.status.load_fail");
                    this.isLoading = false;
                    this.rebuildWidgets();
                });
                return;
            }

            try {
                JsonArray array = JsonParser.parseString(response).getAsJsonArray();
                List<ListingData> parsedListings = new ArrayList<>();
                for (JsonElement element : array) {
                    parsedListings.add(new ListingData(element.getAsJsonObject()));
                }
                Minecraft.getInstance().execute(() -> {
                    this.listings.clear();
                    this.listings.addAll(parsedListings);
                    this.isLoading = false;
                    this.rebuildWidgets();
                });
            } catch (Exception e) {
                EconomyCommon.LOGGER.error("Failed to parse flea market listings: ", e);
                Minecraft.getInstance().execute(() -> {
                    this.statusMessage = EconomyMasterI18n.trs("economy.ui.flea.status.parse_fail");
                    this.isLoading = false;
                    this.rebuildWidgets();
                });
            }
        });
    }

    @Override
    protected void rebuildWidgets() {
        this.clearWidgets();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (this.isLoading) {
            this.addRenderableWidget(Button.builder(EconomyMasterI18n.tr("economy.ui.close"), b -> this.onClose())
                    .bounds(centerX - 40, centerY + 96, 80, 20).build());
            return;
        }

        // タブ切り替えボタン
        int tabWidth = 80;
        int tabSpacing = 4;
        int totalTabsWidth = (tabWidth * 3) + (tabSpacing * 2);
        int startX = centerX - (totalTabsWidth / 2);
        for (int i = 0; i < TAB_LABEL_KEYS.length; i++) {
            final int tabIndex = i;
            Button tabBtn = Button.builder(EconomyMasterI18n.tr(TAB_LABEL_KEYS[i]), b -> {
                this.activeTab = tabIndex;
                this.selectedInventoryItem = null;
                rebuildWidgets();
            }).bounds(startX + i * (tabWidth + tabSpacing), centerY + TAB_Y_OFFSET, tabWidth, TAB_HEIGHT).build();
            tabBtn.active = (this.activeTab != tabIndex) && !this.isProcessing;
            this.addRenderableWidget(tabBtn);
        }

        // ビューごとのウィジェット配置
        if (this.activeTab == 0) {
            // 市場一覧: 全出品を表示（自分の出品は購入不可）
            List<ListingData> marketList = this.listings;

            int visibleRows = Math.min(ROWS_VISIBLE, marketList.size() - this.marketScroll);
            for (int i = 0; i < visibleRows; i++) {
                int idx = this.marketScroll + i;
                if (idx >= marketList.size())
                    break;
                ListingData l = marketList.get(idx);
                int rowY = centerY + ROW_START_Y_OFFSET + i * ROW_HEIGHT;
                boolean ownListing = isOwnListing(l);

                // 購入ボタン
                Button buy1Btn = Button.builder(EconomyMasterI18n.tr("economy.ui.buy_1"), b -> handleBuy(l, 1))
                        .bounds(centerX + 35, rowY, 38, 18).build();
                Button buy16Btn = Button.builder(EconomyMasterI18n.tr("economy.ui.buy_16"), b -> handleBuy(l, 16))
                        .bounds(centerX + 76, rowY, 44, 18).build();
                Button buyAllBtn = Button
                        .builder(EconomyMasterI18n.tr("economy.ui.buy_all"), b -> handleBuy(l, l.remainingQuantity))
                        .bounds(centerX + 123, rowY, 32, 18).build();

                buy1Btn.active = !ownListing && !this.isProcessing && l.remainingQuantity >= 1
                        && EconomyCommon.getCurrentBalance() >= l.price;
                buy16Btn.active = !ownListing && !this.isProcessing && l.remainingQuantity >= 16
                        && EconomyCommon.getCurrentBalance() >= l.price * 16L;
                buyAllBtn.active = !ownListing && !this.isProcessing && l.remainingQuantity > 0
                        && EconomyCommon.getCurrentBalance() >= l.price * (long) l.remainingQuantity;

                this.addRenderableWidget(buy1Btn);
                this.addRenderableWidget(buy16Btn);
                this.addRenderableWidget(buyAllBtn);
            }

            // スクロールボタン
            Button upBtn = Button.builder(Component.literal("▲"), b -> {
                if (this.marketScroll > 0) {
                    this.marketScroll--;
                    rebuildWidgets();
                }
            }).bounds(centerX + 160, centerY + SCROLL_UP_Y_OFFSET, 20, 20).build();
            upBtn.active = this.marketScroll > 0;

            Button downBtn = Button.builder(Component.literal("▼"), b -> {
                int maxScroll = Math.max(0, marketList.size() - ROWS_VISIBLE);
                if (this.marketScroll < maxScroll) {
                    this.marketScroll++;
                    rebuildWidgets();
                }
            }).bounds(centerX + 160, centerY + SCROLL_DOWN_Y_OFFSET, 20, 20).build();
            downBtn.active = this.marketScroll < Math.max(0, marketList.size() - ROWS_VISIBLE);

            this.addRenderableWidget(upBtn);
            this.addRenderableWidget(downBtn);

        } else if (this.activeTab == 1) {
            // 自分の出品: 出品のキャンセル・回収
            List<ListingData> myList = new ArrayList<>();
            Player localPlayer = Minecraft.getInstance().player;
            String localUuid = localPlayer != null ? localPlayer.getUUID().toString() : "";
            for (ListingData l : this.listings) {
                if (l.sellerUuid.equalsIgnoreCase(localUuid)) {
                    myList.add(l);
                }
            }

            int visibleRows = Math.min(ROWS_VISIBLE, myList.size() - this.myScroll);
            for (int i = 0; i < visibleRows; i++) {
                int idx = this.myScroll + i;
                if (idx >= myList.size())
                    break;
                ListingData l = myList.get(idx);
                int rowY = centerY + ROW_START_Y_OFFSET + i * ROW_HEIGHT;

                // 回収ボタン
                Button cancelBtn = Button.builder(EconomyMasterI18n.tr("economy.ui.flea.cancel"), b -> handleCancel(l))
                        .bounds(centerX + 80, rowY, 70, 18).build();
                cancelBtn.active = !this.isProcessing;
                this.addRenderableWidget(cancelBtn);
            }

            // スクロールボタン
            Button upBtn = Button.builder(Component.literal("▲"), b -> {
                if (this.myScroll > 0) {
                    this.myScroll--;
                    rebuildWidgets();
                }
            }).bounds(centerX + 160, centerY + SCROLL_UP_Y_OFFSET, 20, 20).build();
            upBtn.active = this.myScroll > 0;

            Button downBtn = Button.builder(Component.literal("▼"), b -> {
                int maxScroll = Math.max(0, myList.size() - ROWS_VISIBLE);
                if (this.myScroll < maxScroll) {
                    this.myScroll++;
                    rebuildWidgets();
                }
            }).bounds(centerX + 160, centerY + SCROLL_DOWN_Y_OFFSET, 20, 20).build();
            downBtn.active = this.myScroll < Math.max(0, myList.size() - ROWS_VISIBLE);

            this.addRenderableWidget(upBtn);
            this.addRenderableWidget(downBtn);

        } else if (this.activeTab == 2) {
            // 新規出品: 手持ちアイテムの一覧から出品
            if (this.selectedInventoryItem == null) {
                // インベントリ一覧の構築
                List<InventoryItemData> invList = getPlayerInventoryItems();
                int visibleRows = Math.min(ROWS_VISIBLE, invList.size() - this.inventoryScroll);
                for (int i = 0; i < visibleRows; i++) {
                    int idx = this.inventoryScroll + i;
                    if (idx >= invList.size())
                        break;
                    InventoryItemData item = invList.get(idx);
                    int rowY = centerY + ROW_START_Y_OFFSET + i * ROW_HEIGHT;

                    // 選択ボタン
                    Button selectBtn = Button.builder(EconomyMasterI18n.tr("economy.ui.flea.list_item"), b -> {
                        this.selectedInventoryItem = item;
                        rebuildWidgets();
                    }).bounds(centerX + 80, rowY, 70, 18).build();
                    selectBtn.active = !this.isProcessing;
                    this.addRenderableWidget(selectBtn);
                }

                // スクロールボタン
                Button upBtn = Button.builder(Component.literal("▲"), b -> {
                    if (this.inventoryScroll > 0) {
                        this.inventoryScroll--;
                        rebuildWidgets();
                    }
                }).bounds(centerX + 160, centerY + SCROLL_UP_Y_OFFSET, 20, 20).build();
                upBtn.active = this.inventoryScroll > 0;

                Button downBtn = Button.builder(Component.literal("▼"), b -> {
                    int maxScroll = Math.max(0, invList.size() - ROWS_VISIBLE);
                    if (this.inventoryScroll < maxScroll) {
                        this.inventoryScroll++;
                        rebuildWidgets();
                    }
                }).bounds(centerX + 160, centerY + SCROLL_DOWN_Y_OFFSET, 20, 20).build();
                downBtn.active = this.inventoryScroll < Math.max(0, invList.size() - ROWS_VISIBLE);

                this.addRenderableWidget(upBtn);
                this.addRenderableWidget(downBtn);
            } else {
                // 出品設定ダイアログ風フォーム
                // 単価EditBox
                this.priceInputBox = new EditBox(this.font, centerX - 40, centerY - 15, 100, 20,
                        EconomyMasterI18n.tr("economy.ui.amount"));
                this.priceInputBox.setValue("100");
                this.addRenderableWidget(this.priceInputBox);

                // 数量EditBox
                this.quantityInputBox = new EditBox(this.font, centerX - 40, centerY + 15, 100, 20,
                        EconomyMasterI18n.tr("economy.ui.quantity"));
                this.quantityInputBox.setValue("1");
                this.addRenderableWidget(this.quantityInputBox);

                // 出品登録実行ボタン
                Button confirmBtn = Button
                        .builder(EconomyMasterI18n.tr("economy.ui.flea.confirm"), b -> handleListConfirm())
                        .bounds(centerX - 95, centerY + 45, 90, 20).build();
                confirmBtn.active = !this.isProcessing;
                this.addRenderableWidget(confirmBtn);

                // キャンセルボタン
                Button backBtn = Button.builder(EconomyMasterI18n.tr("economy.ui.flea.back"), b -> {
                    this.selectedInventoryItem = null;
                    rebuildWidgets();
                }).bounds(centerX + 5, centerY + 45, 90, 20).build();
                backBtn.active = !this.isProcessing;
                this.addRenderableWidget(backBtn);
            }
        }

        // 閉じるボタン
        this.addRenderableWidget(Button.builder(Component.literal("閉じる"), b -> this.onClose())
                .bounds(centerX - 40, centerY + 96, 80, 20).build());
    }

    private List<InventoryItemData> getPlayerInventoryItems() {
        List<InventoryItemData> result = new ArrayList<>();
        Player player = Minecraft.getInstance().player;
        if (player == null)
            return result;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            boolean found = false;
            for (int j = 0; j < result.size(); j++) {
                InventoryItemData d = result.get(j);
                if (ItemStack.isSameItemSameComponents(d.sample, stack)) {
                    result.set(j, new InventoryItemData(d.sample, d.count + stack.getCount()));
                    found = true;
                    break;
                }
            }
            if (!found) {
                result.add(new InventoryItemData(stack, stack.getCount()));
            }
        }
        return result;
    }

    private String resolveListingDisplayName(ListingData listing) {
        if (!listing.displayStack.isEmpty()) {
            return listing.displayStack.getHoverName().getString();
        }
        if (listing.itemName != null && !listing.itemName.isBlank()) {
            return listing.itemName;
        }
        return resolveItemDisplayName(listing.itemKey, listing.itemName);
    }

    private String resolveItemDisplayName(String itemKey, String fallbackName) {
        try {
            Identifier id = Identifier.parse(itemKey);
            Item item = BuiltInRegistries.ITEM.get(id).map(net.minecraft.core.Holder::value).orElse(null);
            if (item != null) {
                return new ItemStack(item).getHoverName().getString();
            }
        } catch (Exception ignored) {
        }
        return fallbackName;
    }

    private String truncateText(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        String trimmed = text;
        while (trimmed.length() > 0 && this.font.width(trimmed + ellipsis) > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? ellipsis : trimmed + ellipsis;
    }

    private void handleListConfirm() {
        if (this.selectedInventoryItem == null || this.priceInputBox == null || this.quantityInputBox == null)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null)
            return;

        try {
            int price = Integer.parseInt(this.priceInputBox.getValue());
            int quantity = Integer.parseInt(this.quantityInputBox.getValue());

            if (price <= 0 || quantity <= 0)
                return;

            this.isProcessing = true;
            rebuildWidgets();

            String stackSnbt = "";
            if (mc.level != null) {
                stackSnbt = FleaMarketStackCodec.encode(
                        mc.level.registryAccess(),
                        this.selectedInventoryItem.sample.copyWithCount(1));
            }
            EconomyPlatform.sendToServer(new FleaMarketListRequestPayload(
                    this.selectedInventoryItem.itemKey,
                    this.selectedInventoryItem.name,
                    price,
                    quantity,
                    stackSnbt != null ? stackSnbt : ""));
        } catch (NumberFormatException e) {
            // 無効な数値
        }
    }

    private void handleBuy(ListingData l, int qty) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null)
            return;

        this.isProcessing = true;
        rebuildWidgets();

        EconomyPlatform.sendToServer(new FleaMarketBuyRequestPayload(l.id, qty));
    }

    private void handleCancel(ListingData l) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null)
            return;

        this.isProcessing = true;
        rebuildWidgets();

        EconomyPlatform.sendToServer(new FleaMarketCancelRequestPayload(l.id));
    }

    public void onTransactionSuccess() {
        this.isProcessing = false;
        this.selectedInventoryItem = null;
        refreshFleaMarketData(false);
    }

    public void onTransactionFailed() {
        this.isProcessing = false;
        rebuildWidgets();
    }

    private void drawBevel(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2, boolean sunken) {
        int topLeftColor, bottomRightColor, bgColor;
        if (sunken) {
            topLeftColor = 0xFF1F1F1F;
            bottomRightColor = 0xFF4A4A4A;
            bgColor = 0xFF161616;
        } else {
            topLeftColor = 0xFF5F5F5F;
            bottomRightColor = 0xFF1F1F1F;
            bgColor = 0xFF2A2A2A;
        }
        gui.fill(x1, y1, x2, y2, bgColor);
        gui.fill(x1, y1, x2, y1 + 1, topLeftColor);
        gui.fill(x1, y1, x1 + 1, y2, topLeftColor);
        gui.fill(x1, y2 - 1, x2, y2, bottomRightColor);
        gui.fill(x2 - 1, y1, x2, y2, bottomRightColor);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // メインパネル外枠
        drawBevel(guiGraphics, centerX - 190, centerY - 92, centerX + 190, centerY + 120, false);
        guiGraphics.fill(centerX - 189, centerY - 91, centerX + 189, centerY - 90, 0xFFDFB323); // 上部ゴールドライン

        // ヘッダータイトル
        drawBevel(guiGraphics, centerX - 185, centerY - 87, centerX + 185, centerY - 69, true);
        guiGraphics.fill(centerX - 185, centerY - 87, centerX + 185, centerY - 86, 0xFFDFB323);
        guiGraphics.centeredText(this.font, this.title.getString(), centerX, centerY - 82, 0xFFFFFFFF);

        // タブ行背景（タイトルと被らない位置）
        drawBevel(guiGraphics, centerX - 185, centerY - 67, centerX + 185, centerY - 45, true);

        // リストエリア背景
        if (!this.isLoading) {
            drawBevel(guiGraphics, centerX - 185, centerY - 44, centerX + 185, centerY + 72, true);
        }

        // 所持金情報
        int balanceY = centerY + 76;
        drawBevel(guiGraphics, centerX - 185, balanceY, centerX + 185, balanceY + 14, true);
        guiGraphics.fill(centerX - 185, balanceY + 13, centerX + 185, balanceY + 14, 0xFF55FF55);
        guiGraphics
                .centeredText(this.font,
                        EconomyMasterI18n.tr("economy.ui.flea.balance",
                                EconomyMasterI18n.formatCurrency(EconomyCommon.getCurrentBalance())).getString(),
                        centerX,
                        balanceY + 3, 0xFF55FF55);

        if (this.isLoading) {
            guiGraphics.centeredText(this.font, this.statusMessage, centerX, centerY, 0xFFAAAAAA);
            super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

        // リスト描画
        if (this.activeTab == 0) {
            // 市場一覧の描画
            List<ListingData> marketList = this.listings;

            if (marketList.isEmpty()) {
                guiGraphics.centeredText(this.font, EconomyMasterI18n.trs("economy.ui.flea.empty.market"), centerX,
                        centerY, 0xFF888888);
            } else {
                int visibleRows = Math.min(ROWS_VISIBLE, marketList.size() - this.marketScroll);
                for (int i = 0; i < visibleRows; i++) {
                    int idx = this.marketScroll + i;
                    ListingData l = marketList.get(idx);
                    int rowY = centerY + ROW_START_Y_OFFSET + i * ROW_HEIGHT;
                    boolean ownListing = isOwnListing(l);

                    drawBevel(guiGraphics, centerX - 185, rowY - 2, centerX + 30, rowY + 20, true);
                    guiGraphics.fill(centerX - 184, rowY - 1, centerX + 29, rowY + 19, 0xFF202020);

                    // アイコンスロット
                    drawBevel(guiGraphics, centerX - 182, rowY, centerX - 162, rowY + 18, true);
                    guiGraphics.fill(centerX - 181, rowY + 1, centerX - 163, rowY + 17, 0xFF0D0D0D);
                    try {
                        if (!l.displayStack.isEmpty()) {
                            guiGraphics.fakeItem(l.displayStack, centerX - 181, rowY + 1);
                        }
                    } catch (Exception e) {
                    }

                    // テキスト情報
                    String displayName = resolveListingDisplayName(l);
                    String sellerLabel = ownListing
                            ? EconomyMasterI18n.trs("economy.ui.flea.seller.own")
                            : EconomyMasterI18n.tr("economy.ui.flea.seller.other", l.sellerName).getString();
                    String titleText = truncateText(displayName + " (" + sellerLabel + ")", LIST_TEXT_MAX_WIDTH);
                    guiGraphics.text(this.font, titleText, centerX - 158, rowY, 0xFFFFFFFF, true);
                    guiGraphics.text(this.font,
                            EconomyMasterI18n.tr("economy.ui.flea.price_unit",
                                    EconomyMasterI18n.formatCurrency(l.price), l.remainingQuantity).getString(),
                            centerX - 158,
                            rowY + 9, 0xFFFFAA00, true);
                }
            }
        } else if (this.activeTab == 1) {
            // 自分の出品描画
            List<ListingData> myList = new ArrayList<>();
            Player localPlayer = Minecraft.getInstance().player;
            String localUuid = localPlayer != null ? localPlayer.getUUID().toString() : "";
            for (ListingData l : this.listings) {
                if (l.sellerUuid.equalsIgnoreCase(localUuid)) {
                    myList.add(l);
                }
            }

            if (myList.isEmpty()) {
                guiGraphics.centeredText(this.font, EconomyMasterI18n.trs("economy.ui.flea.empty.mine"), centerX,
                        centerY, 0xFF888888);
            } else {
                int visibleRows = Math.min(ROWS_VISIBLE, myList.size() - this.myScroll);
                for (int i = 0; i < visibleRows; i++) {
                    int idx = this.myScroll + i;
                    ListingData l = myList.get(idx);
                    int rowY = centerY + ROW_START_Y_OFFSET + i * ROW_HEIGHT;

                    drawBevel(guiGraphics, centerX - 185, rowY - 2, centerX + 75, rowY + 20, true);
                    guiGraphics.fill(centerX - 184, rowY - 1, centerX + 74, rowY + 19, 0xFF202020);

                    // アイコンスロット
                    drawBevel(guiGraphics, centerX - 182, rowY, centerX - 162, rowY + 18, true);
                    guiGraphics.fill(centerX - 181, rowY + 1, centerX - 163, rowY + 17, 0xFF0D0D0D);
                    try {
                        if (!l.displayStack.isEmpty()) {
                            guiGraphics.fakeItem(l.displayStack, centerX - 181, rowY + 1);
                        }
                    } catch (Exception e) {
                    }

                    String displayName = resolveListingDisplayName(l);
                    guiGraphics.text(this.font, truncateText(displayName, LIST_TEXT_MAX_WIDTH), centerX - 158, rowY,
                            0xFFFFFFFF, true);
                    guiGraphics.text(this.font,
                            EconomyMasterI18n.tr("economy.ui.flea.price_unit_remain",
                                    EconomyMasterI18n.formatCurrency(l.price), l.remainingQuantity).getString(),
                            centerX - 158,
                            rowY + 9, 0xFFFFAA00, true);
                }
            }
        } else if (this.activeTab == 2) {
            // 新規出品の描画
            if (this.selectedInventoryItem == null) {
                List<InventoryItemData> invList = getPlayerInventoryItems();
                if (invList.isEmpty()) {
                    guiGraphics.centeredText(this.font, EconomyMasterI18n.trs("economy.ui.flea.empty.inventory"),
                            centerX, centerY, 0xFF888888);
                } else {
                    int visibleRows = Math.min(ROWS_VISIBLE, invList.size() - this.inventoryScroll);
                    for (int i = 0; i < visibleRows; i++) {
                        int idx = this.inventoryScroll + i;
                        InventoryItemData item = invList.get(idx);
                        int rowY = centerY + ROW_START_Y_OFFSET + i * ROW_HEIGHT;

                        drawBevel(guiGraphics, centerX - 185, rowY - 2, centerX + 75, rowY + 20, true);
                        guiGraphics.fill(centerX - 184, rowY - 1, centerX + 74, rowY + 19, 0xFF202020);

                        // アイコンスロット
                        drawBevel(guiGraphics, centerX - 182, rowY, centerX - 162, rowY + 18, true);
                        guiGraphics.fill(centerX - 181, rowY + 1, centerX - 163, rowY + 17, 0xFF0D0D0D);
                        try {
                            if (!item.sample.isEmpty()) {
                                guiGraphics.fakeItem(item.sample, centerX - 181, rowY + 1);
                            }
                        } catch (Exception e) {
                        }

                        guiGraphics.text(this.font, truncateText(item.name, LIST_TEXT_MAX_WIDTH), centerX - 158, rowY,
                                0xFFFFFFFF, true);
                        guiGraphics.text(this.font, EconomyMasterI18n.tr("economy.ui.owned", item.count).getString(),
                                centerX - 158, rowY + 9, 0xFFFFAA00,
                                true);
                    }
                }
            } else {
                // 出品設定ダイアログ描画
                guiGraphics.centeredText(this.font,
                        EconomyMasterI18n.tr("economy.ui.flea.item_title", this.selectedInventoryItem.name).getString(),
                        centerX,
                        centerY - 40, 0xFFFFFFFF);
                String symbol = EconomyMasterI18n.trs("economy.currency.format").replace("%s", "").trim();
                guiGraphics.text(this.font, EconomyMasterI18n.tr("economy.ui.flea.price_label", symbol).getString(),
                        centerX - 145, centerY - 9, 0xFFCCCCCC, true);
                guiGraphics.text(this.font, EconomyMasterI18n.trs("economy.ui.flea.quantity_label"), centerX - 145,
                        centerY + 21, 0xFFCCCCCC, true);
            }
        }

        if (this.isProcessing) {
            guiGraphics.centeredText(this.font, EconomyMasterI18n.trs("economy.ui.processing"), centerX, centerY + 90,
                    0xFFF43F5E);
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isLoading || this.isProcessing) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        if (!isMouseOver(mouseX, mouseY, centerX - 185, centerY - 44, centerX + 185, centerY + 72)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        if (this.activeTab == 0) {
            if (scrollListOffset(scrollY, this.listings.size(), this.marketScroll, next -> this.marketScroll = next)) {
                return true;
            }
        } else if (this.activeTab == 1) {
            int listSize = countMyListings();
            if (scrollListOffset(scrollY, listSize, this.myScroll, next -> this.myScroll = next)) {
                return true;
            }
        } else if (this.activeTab == 2 && this.selectedInventoryItem == null) {
            int listSize = getPlayerInventoryItems().size();
            if (scrollListOffset(scrollY, listSize, this.inventoryScroll, next -> this.inventoryScroll = next)) {
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean scrollListOffset(double scrollY, int listSize, int currentScroll, IntConsumer setter) {
        if (listSize <= ROWS_VISIBLE) {
            return false;
        }
        int maxScroll = listSize - ROWS_VISIBLE;
        int next = currentScroll - (int) Math.signum(scrollY);
        next = Math.max(0, Math.min(next, maxScroll));
        if (next != currentScroll) {
            setter.accept(next);
            rebuildWidgets();
            return true;
        }
        return false;
    }

    private String getLocalPlayerUuid() {
        Player localPlayer = Minecraft.getInstance().player;
        return localPlayer != null ? localPlayer.getUUID().toString() : "";
    }

    private boolean isOwnListing(ListingData listing) {
        return listing.sellerUuid.equalsIgnoreCase(getLocalPlayerUuid());
    }

    private int countMyListings() {
        int count = 0;
        for (ListingData l : this.listings) {
            if (isOwnListing(l)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isMouseOver(double mouseX, double mouseY, int x1, int y1, int x2, int y2) {
        return mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2;
    }
}
