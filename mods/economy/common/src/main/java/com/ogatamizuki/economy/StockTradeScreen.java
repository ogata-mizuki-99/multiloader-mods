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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StockTradeScreen extends Screen {
    private static final NumberFormat YEN_FORMAT = NumberFormat.getNumberInstance(Locale.JAPAN);
    private static final String[] TRADE_TYPES = {"BUY", "SELL", "SELL_SHORT", "BUY_COVER"};
    private static final String[] TRADE_LABEL_KEYS = {
            "economy.ui.etf_buy", "economy.ui.etf_sell", "economy.ui.etf_short", "economy.ui.etf_cover"
    };
    private static final String[] TAB_LABEL_KEYS = {
            "economy.ui.etf_tab_market", "economy.ui.etf_tab_trade", "economy.ui.etf_tab_short"
    };

    // centerY からの相対Y座標（描画テキストとボタンで共通）
    private static final int ROW_CHART_TOP = -16;
    private static final int ROW_CHART_BOTTOM = 50; // チャートの高さを縮小してボタンの被りを解消
    private static final int ROW_TRADE_BTNS = -12;
    private static final int ROW_QTY_LABEL = 12;
    private static final int ROW_QTY_BTNS = 22;
    private static final int ROW_COST = 44; // 概算金額（実行ボタンの上）
    private static final int ROW_EXECUTE = 56;
    private static final int ROW_BALANCE = 80;
    private static final int ROW_CLOSE = 96;
    private static final int PANEL_TOP = -80; // パネル上端を縮小
    private static final int PANEL_BOTTOM = 118;

    private final List<StockData> stocks = new ArrayList<>();
    private final List<Integer> priceHistory = new ArrayList<>();
    private int selectedIndex = 0;
    private int selectedTradeType = 0; // Tab1: 0 (BUY) or 1 (SELL), Tab2: 2 (SELL_SHORT) or 3 (BUY_COVER)
    private int tradeQuantity = 1;
    private int activeTab = 0; // 0: 市場情報, 1: 通常取引, 2: 空売り取引
    private boolean showPriceList = false; // 価格一覧表示フラグ
    private int priceListScroll = 0; // 価格一覧のスクロールオフセット
    private boolean showComponentList = false; // 構成アイテム表示フラグ
    private int componentListScroll = 0; // 構成アイテムスクロールオフセット
    private boolean isComponentsLoading = false; // 構成アイテムロード中フラグ
    private final List<ComponentData> componentItems = new ArrayList<>();
    private static final int PRICE_LIST_ROWS_VISIBLE = 6; // 一覧に一度に表示する行数

    private static class ComponentData {
        final String itemKey;
        final double influenceWeight;

        ComponentData(String itemKey, double influenceWeight) {
            this.itemKey = itemKey;
            this.influenceWeight = influenceWeight;
        }
    }

    private boolean isLoading = true;
    private boolean isProcessing = false;
    private String statusMessage = "データを読み込んでいます...";

    private Button rankingButton;
    private boolean hasRankingData = false;
    private JsonObject rankingData = null;

    private static class StockData {
        final String id;
        final String code;
        final String name;
        int currentPrice;
        int portfolioQuantity;
        int shortSellAvailable;

        StockData(JsonObject obj) {
            this.id = obj.get("id").getAsString();
            this.code = obj.get("code").getAsString();
            this.name = obj.has("name") ? obj.get("name").getAsString() : this.code;
            this.currentPrice = obj.get("currentPrice").getAsInt();
            this.portfolioQuantity = obj.has("portfolioQuantity") ? obj.get("portfolioQuantity").getAsInt() : 0;
            this.shortSellAvailable = obj.has("shortSellAvailable") ? obj.get("shortSellAvailable").getAsInt() : 0;
        }

        String localizedName() {
            return EconomyMasterI18n.etfName(code, name);
        }
    }

    private static String tradeLabel(int index) {
        return EconomyMasterI18n.trs(TRADE_LABEL_KEYS[index]);
    }

    private static String tabLabel(int index) {
        return EconomyMasterI18n.trs(TAB_LABEL_KEYS[index]);
    }

    protected StockTradeScreen() {
        super(Component.literal("ETF"));
    }

    @Override
    protected void init() {
        super.init();

        // 非同期で最新のランキングデータを取得し、存在すればボタンを有効化
        EconomyService.fetchLatestRanking().thenAccept(res -> {
            if (res != null) {
                try {
                    JsonObject json = JsonParser.parseString(res).getAsJsonObject();
                    if (json.has("records") && json.getAsJsonArray("records").size() > 0) {
                        JsonObject finalJson = json;
                        Minecraft.getInstance().execute(() -> {
                            this.rankingData = finalJson;
                            this.hasRankingData = true;
                            if (this.rankingButton != null) {
                                this.rankingButton.active = true;
                            }
                        });
                        return;
                    }
                } catch (Exception e) {
                    EconomyCommon.LOGGER.error("Failed to parse latest ranking in StockTradeScreen: ", e);
                }
            }
            Minecraft.getInstance().execute(() -> {
                this.rankingData = null;
                this.hasRankingData = false;
                if (this.rankingButton != null) {
                    this.rankingButton.active = false;
                }
            });
        });

        refreshStockData(true);
    }

    private void refreshStockData(boolean showLoading) {
        if (showLoading) {
            this.isLoading = true;
            this.statusMessage = "データを読み込んでいます...";
            this.clearWidgets();
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        String playerUuid = mc.player.getUUID().toString();

        EconomyService.fetchStocks(playerUuid).thenAccept(response -> {
            if (response == null) {
                Minecraft.getInstance().execute(() -> {
                    this.statusMessage = "ETFデータの取得に失敗しました。";
                    this.isLoading = false;
                    this.rebuildWidgets();
                });
                return;
            }

            try {
                JsonArray array = JsonParser.parseString(response).getAsJsonArray();
                List<StockData> parsedStocks = new ArrayList<>();
                for (JsonElement element : array) {
                    parsedStocks.add(new StockData(element.getAsJsonObject()));
                }
                Minecraft.getInstance().execute(() -> {
                    this.stocks.clear();
                    this.stocks.addAll(parsedStocks);
                    if (this.selectedIndex >= this.stocks.size()) {
                        this.selectedIndex = Math.max(0, this.stocks.size() - 1);
                    }
                    this.isLoading = false;
                    rebuildWidgets();
                    loadPriceHistory();
                });
            } catch (Exception e) {
                EconomyCommon.LOGGER.error("Failed to parse stocks response: ", e);
                Minecraft.getInstance().execute(() -> {
                    this.statusMessage = "データの解析に失敗しました。";
                    this.isLoading = false;
                    this.rebuildWidgets();
                });
            }
        });
    }

    private void loadPriceHistory() {
        if (this.stocks.isEmpty()) {
            this.priceHistory.clear();
            return;
        }
        StockData stock = this.stocks.get(this.selectedIndex);
        EconomyService.fetchStockHistory(stock.id, 60).thenAccept(response -> {
            if (response == null) return;
            try {
                JsonArray array = JsonParser.parseString(response).getAsJsonArray();
                List<Integer> points = new ArrayList<>();
                for (JsonElement element : array) {
                    points.add(element.getAsJsonObject().get("price").getAsInt());
                }
                Minecraft.getInstance().execute(() -> {
                    this.priceHistory.clear();
                    this.priceHistory.addAll(points);
                });
            } catch (Exception e) {
                EconomyCommon.LOGGER.error("Failed to parse stock history response: ", e);
            }
        });
    }

    private void selectStock(int newIndex) {
        if (newIndex < 0 || newIndex >= this.stocks.size()) return;
        this.selectedIndex = newIndex;
        this.priceHistory.clear();
        rebuildWidgets();
        loadPriceHistory();
    }

    @Override
    protected void rebuildWidgets() {
        this.clearWidgets();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (this.isLoading || this.stocks.isEmpty()) {
            this.addRenderableWidget(Button.builder(EconomyMasterI18n.tr("economy.ui.close"), b -> this.onClose())
                    .bounds(centerX - 40, centerY + ROW_CLOSE, 80, 20).build());
            return;
        }

        // タブ変更時の取引タイプの初期化ガード
        if (this.activeTab == 1 && (this.selectedTradeType < 0 || this.selectedTradeType > 1)) {
            this.selectedTradeType = 0;
        } else if (this.activeTab == 2 && (this.selectedTradeType < 2 || this.selectedTradeType > 3)) {
            this.selectedTradeType = 2;
        }

        StockData stock = this.stocks.get(this.selectedIndex);

        // タブ切り替えボタン (画面上部に配置、3列に配置)
        int tabWidth = 80;
        int tabSpacing = 4;
        int totalTabsWidth = (tabWidth * 3) + (tabSpacing * 2);
        int startX = centerX - (totalTabsWidth / 2);
        for (int i = 0; i < TAB_LABEL_KEYS.length; i++) {
            final int tabIndex = i;
            Button tabBtn = Button.builder(Component.literal(tabLabel(i)), b -> {
                this.activeTab = tabIndex;
                this.showPriceList = false; // タブ切替時は一覧を閉じる
                this.showComponentList = false;
                rebuildWidgets();
            }).bounds(startX + i * (tabWidth + tabSpacing), centerY - 76, tabWidth, 20).build();
            // 一覧表示中は通常取引・空売り取引タブを非活性化
            if (tabIndex == 1 || tabIndex == 2) {
                tabBtn.active = !this.showPriceList && !this.showComponentList && (this.activeTab != tabIndex) && !this.isProcessing;
            } else {
                tabBtn.active = (this.activeTab != tabIndex) && !this.isProcessing;
            }
            this.addRenderableWidget(tabBtn);
        }

        if (this.activeTab == 0) {
            // 【市場情報タブ】
            if (this.showPriceList) {
                // 価格一覧表示モード
                // ▲ボタン: 一覧エリア上端 (centerY-28) に小さく配置
                Button scrollUpBtn = Button.builder(Component.literal("▲"), b -> {
                    if (this.priceListScroll > 0) {
                        this.priceListScroll--;
                        rebuildWidgets();
                    }
                }).bounds(centerX + 94, centerY - 28, 28, 14).build();
                scrollUpBtn.active = this.priceListScroll > 0;
                this.addRenderableWidget(scrollUpBtn);

                Button scrollDownBtn = Button.builder(Component.literal("▼"), b -> {
                    int maxScroll = Math.max(0, this.stocks.size() - PRICE_LIST_ROWS_VISIBLE);
                    if (this.priceListScroll < maxScroll) {
                        this.priceListScroll++;
                        rebuildWidgets();
                    }
                }).bounds(centerX + 94, centerY + 43, 28, 14).build();
                scrollDownBtn.active = this.priceListScroll < Math.max(0, this.stocks.size() - PRICE_LIST_ROWS_VISIBLE);
                this.addRenderableWidget(scrollDownBtn);

                // チャートへ戻るボタン (閉じるの1段上)
                Button backBtn = Button.builder(EconomyMasterI18n.tr("economy.ui.etf.back_chart"), b -> {
                    this.showPriceList = false;
                    this.priceListScroll = 0;
                    rebuildWidgets();
                }).bounds(centerX - 50, centerY + 71, 100, 20).build();
                this.addRenderableWidget(backBtn);
            } else if (this.showComponentList) {
                // 構成アイテム表示モード
                // ▲ボタン: 一覧エリア上端 (centerY-28) に小さく配置
                Button scrollUpBtn = Button.builder(Component.literal("▲"), b -> {
                    if (this.componentListScroll > 0) {
                        this.componentListScroll--;
                        rebuildWidgets();
                    }
                }).bounds(centerX + 94, centerY - 28, 28, 14).build();
                scrollUpBtn.active = this.componentListScroll > 0;
                this.addRenderableWidget(scrollUpBtn);

                Button scrollDownBtn = Button.builder(Component.literal("▼"), b -> {
                    int maxScroll = Math.max(0, this.componentItems.size() - PRICE_LIST_ROWS_VISIBLE);
                    if (this.componentListScroll < maxScroll) {
                        this.componentListScroll++;
                        rebuildWidgets();
                    }
                }).bounds(centerX + 94, centerY + 43, 28, 14).build();
                scrollDownBtn.active = this.componentListScroll < Math.max(0, this.componentItems.size() - PRICE_LIST_ROWS_VISIBLE);
                this.addRenderableWidget(scrollDownBtn);

                // チャートへ戻るボタン (閉じるの1段上)
                Button backBtn = Button.builder(EconomyMasterI18n.tr("economy.ui.etf.back_chart"), b -> {
                    this.showComponentList = false;
                    this.componentListScroll = 0;
                    rebuildWidgets();
                }).bounds(centerX - 50, centerY + 71, 100, 20).build();
                this.addRenderableWidget(backBtn);
            } else {
                // チャート表示モード
                this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
                    if (this.selectedIndex > 0) {
                        selectStock(this.selectedIndex - 1);
                    }
                }).bounds(centerX - 120, centerY - 34, 30, 20).build());

                this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                    if (this.selectedIndex < this.stocks.size() - 1) {
                        selectStock(this.selectedIndex + 1);
                    }
                }).bounds(centerX + 90, centerY - 34, 30, 20).build());

                // 価格一覧ボタン
                Button listBtn = Button.builder(EconomyMasterI18n.tr("economy.ui.etf.price_list"), b -> {
                    this.showPriceList = true;
                    this.priceListScroll = 0;
                    rebuildWidgets();
                }).bounds(centerX - 125, centerY + ROW_EXECUTE, 80, 20).build();
                this.addRenderableWidget(listBtn);

                // 構成アイテムボタン
                Button compBtn = Button.builder(EconomyMasterI18n.tr("economy.ui.etf.components"), b -> {
                    this.showComponentList = true;
                    this.componentListScroll = 0;
                    this.isComponentsLoading = true;
                    this.componentItems.clear();
                    rebuildWidgets();

                    StockData currentStock = this.stocks.get(this.selectedIndex);
                    EconomyService.fetchStockComponents(currentStock.id).thenAccept(res -> {
                        List<ComponentData> parsedComponents = new ArrayList<>();
                        if (res != null) {
                            try {
                                JsonArray array = JsonParser.parseString(res).getAsJsonArray();
                                for (JsonElement element : array) {
                                    JsonObject obj = element.getAsJsonObject();
                                    parsedComponents.add(new ComponentData(
                                        obj.get("item_key").getAsString(),
                                        obj.get("influence_weight").getAsDouble()
                                    ));
                                }
                            } catch (Exception e) {
                                EconomyCommon.LOGGER.error("Failed to parse stock components response: ", e);
                            }
                        }
                        Minecraft.getInstance().execute(() -> {
                            this.isComponentsLoading = false;
                            this.componentItems.clear();
                            this.componentItems.addAll(parsedComponents);
                            this.rebuildWidgets();
                        });
                    });
                }).bounds(centerX - 40, centerY + ROW_EXECUTE, 80, 20).build();
                this.addRenderableWidget(compBtn);

                // ランキングボタン
                this.rankingButton = Button.builder(EconomyMasterI18n.tr("economy.ranking.button"), button -> {
                    if (this.hasRankingData && this.rankingData != null) {
                        Minecraft.getInstance().gui.setScreen(new RankingScreen(this, this.rankingData));
                    }
                }).bounds(centerX + 45, centerY + ROW_EXECUTE, 80, 20).build();
                this.rankingButton.active = this.hasRankingData;
                this.addRenderableWidget(this.rankingButton);
            }
        } else if (this.activeTab == 1) {
            // 【通常取引タブ: 現物買(0), 現物売(1)】
            for (int i = 0; i < 2; i++) {
                final int typeIndex = i;
                Button typeBtn = Button.builder(Component.literal(tradeLabel(i)), b -> {
                    this.selectedTradeType = typeIndex;
                    rebuildWidgets();
                }).bounds(centerX - 60 + i * 65, centerY + ROW_TRADE_BTNS, 55, 20).build();
                typeBtn.active = (this.selectedTradeType != typeIndex) && !this.isProcessing;
                this.addRenderableWidget(typeBtn);
            }

            int[] quantities = {1, 10, 100};
            for (int i = 0; i < quantities.length; i++) {
                final int qty = quantities[i];
                Button qtyBtn = Button.builder(Component.literal(String.valueOf(qty)), b -> {
                    this.tradeQuantity = qty;
                    rebuildWidgets();
                }).bounds(centerX - 60 + i * 42, centerY + ROW_QTY_BTNS, 38, 20).build();
                qtyBtn.active = !this.isProcessing;
                this.addRenderableWidget(qtyBtn);
            }

            Button executeBtn = Button.builder(EconomyMasterI18n.tr("economy.ui.execute"), b -> executeTrade(stock))
                    .bounds(centerX - 40, centerY + ROW_EXECUTE, 80, 20).build();
            executeBtn.active = !this.isProcessing && canExecuteTrade(stock);
            this.addRenderableWidget(executeBtn);
        } else {
            // 【空売り取引タブ: 空売(2), 買戻(3)】
            for (int i = 0; i < 2; i++) {
                final int typeIndex = i + 2;
                Button typeBtn = Button.builder(Component.literal(tradeLabel(typeIndex)), b -> {
                    this.selectedTradeType = typeIndex;
                    rebuildWidgets();
                }).bounds(centerX - 60 + i * 65, centerY + ROW_TRADE_BTNS, 55, 20).build();
                typeBtn.active = (this.selectedTradeType != typeIndex) && !this.isProcessing;
                this.addRenderableWidget(typeBtn);
            }

            int[] quantities = {1, 10, 100};
            for (int i = 0; i < quantities.length; i++) {
                final int qty = quantities[i];
                Button qtyBtn = Button.builder(Component.literal(String.valueOf(qty)), b -> {
                    this.tradeQuantity = qty;
                    rebuildWidgets();
                }).bounds(centerX - 60 + i * 42, centerY + ROW_QTY_BTNS, 38, 20).build();
                qtyBtn.active = !this.isProcessing;
                this.addRenderableWidget(qtyBtn);
            }

            Button executeBtn = Button.builder(EconomyMasterI18n.tr("economy.ui.execute"), b -> executeTrade(stock))
                    .bounds(centerX - 40, centerY + ROW_EXECUTE, 80, 20).build();
            executeBtn.active = !this.isProcessing && canExecuteTrade(stock);
            this.addRenderableWidget(executeBtn);
        }

        // 閉じる ボタンは共通で最下部 (常に表示・中央)
        this.addRenderableWidget(Button.builder(EconomyMasterI18n.tr("economy.ui.close"), b -> this.onClose())
                .bounds(centerX - 40, centerY + ROW_CLOSE, 80, 20).build());
    }

    private boolean canExecuteTrade(StockData stock) {
        int cost = stock.currentPrice * tradeQuantity;
        int balance = EconomyCommon.getCurrentBalance();
        int qty = stock.portfolioQuantity;

        return switch (TRADE_TYPES[selectedTradeType]) {
            case "BUY" -> qty >= 0 && balance >= cost;
            case "SELL" -> qty > 0 && tradeQuantity <= qty;
            case "SELL_SHORT" -> true;
            case "BUY_COVER" -> qty < 0 && tradeQuantity <= Math.abs(qty) && balance >= cost;
            default -> false;
        };
    }

    private void executeTrade(StockData stock) {
        if (!canExecuteTrade(stock)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;

        this.isProcessing = true;
        rebuildWidgets();

        EconomyPlatform.sendToServer(new StockTradeRequestPayload(
                stock.id,
                TRADE_TYPES[selectedTradeType],
                tradeQuantity
        ));
    }

    public void onTransactionSuccess(int newBalance, int currentPrice, int portfolioQuantity) {
        this.isProcessing = false;
        EconomyCommon.setCurrentBalance(newBalance);
        if (this.selectedIndex < this.stocks.size()) {
            StockData stock = this.stocks.get(this.selectedIndex);
            stock.currentPrice = currentPrice;
            stock.portfolioQuantity = portfolioQuantity;
        }
        loadPriceHistory();
        rebuildWidgets();
    }

    public void onTransactionFailed() {
        this.isProcessing = false;
        rebuildWidgets();
    }

    private void drawBevel(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2, boolean sunken) {
        // ダークモード風の配色に変更
        int strokeColor = sunken ? 0x25FFFFFF : 0x40FFFFFF;
        int bgColor = sunken ? 0xFF07090E : 0xFF14161E;
        gui.fill(x1, y1, x2, y2, bgColor);
        // 細い均一なボーダーラインで洗練されたフラット感を演出
        gui.fill(x1, y1, x2, y1 + 1, strokeColor);
        gui.fill(x1, y1, x1 + 1, y2, strokeColor);
        gui.fill(x1, y2 - 1, x2, y2, strokeColor);
        gui.fill(x2 - 1, y1, x2, y2, strokeColor);
    }

    private void drawPriceChart(GuiGraphicsExtractor gui, int centerX, int centerY) {
        int left = centerX - 120;
        int top = centerY + ROW_CHART_TOP;
        int right = centerX + 120;
        int bottom = centerY + ROW_CHART_BOTTOM;
        drawBevel(gui, left, top, right, bottom, true);
        gui.text(this.font, EconomyMasterI18n.trs("economy.ui.etf.history"), left + 6, top + 4, 0xFF94A3B8, false);

        if (this.priceHistory.isEmpty()) {
            gui.centeredText(this.font, EconomyMasterI18n.trs("economy.ui.etf.history_loading"), centerX, top + 20, 0xFF64748B);
            return;
        }

        // 最新の株価を履歴の最後の要素に強制適用して同期を完全にする
        StockData stock = this.stocks.get(this.selectedIndex);
        List<Integer> prices = new ArrayList<>(this.priceHistory);
        if (!prices.isEmpty()) {
            prices.set(prices.size() - 1, stock.currentPrice);
        }

        int min = prices.get(0);
        int max = prices.get(0);
        for (int price : prices) {
            min = Math.min(min, price);
            max = Math.max(max, price);
        }
        int range = Math.max(max - min, 1);

        int chartLeft = left + 4;
        int chartRight = right - 4;
        int chartTop = top + 14;
        int chartBottom = bottom - 4;
        
        // グラフ描画エリアの上下マージン
        int plotTop = chartTop + 10;
        int plotBottom = chartBottom - 10;
        int plotHeight = Math.max(plotBottom - plotTop, 1);
        int chartWidth = Math.max(chartRight - chartLeft, 1);

        if (prices.size() == 1) {
            int y = plotTop + plotHeight / 2;
            gui.fill(chartLeft, y, chartRight, y + 1, 0xFF38BDF8);
            gui.text(this.font, "¥" + YEN_FORMAT.format(prices.get(0)), chartLeft + 2, plotTop - 4, 0xFFFFCC00, false);
            return;
        }

        // 中間点線のグリッド線を描画
        int midY = plotTop + plotHeight / 2;
        for (int gx = chartLeft; gx < chartRight; gx += 4) {
            gui.fill(gx, midY, gx + 2, midY + 1, 0x1AFFFFFF); // 薄い半透明グリッド
        }

        // 価格目盛りテキスト（プロット領域のY座標と整合）
        gui.text(this.font, "¥" + YEN_FORMAT.format(max), chartLeft + 2, plotTop - 4, 0xFFFFCC00, false);
        gui.text(this.font, "¥" + YEN_FORMAT.format(min), chartLeft + 2, plotBottom - 4, 0xFF94A3B8, false);
        gui.text(this.font, "¥" + YEN_FORMAT.format((max + min) / 2), chartLeft + 2, midY - 4, 0xFF64748B, false);

        int prevX = chartLeft;
        int prevY = plotBottom - ((prices.get(0) - min) * plotHeight / range);
        for (int i = 1; i < prices.size(); i++) {
            int x = chartLeft + (i * chartWidth / (prices.size() - 1));
            int y = plotBottom - ((prices.get(i) - min) * plotHeight / range);
            drawLine(gui, prevX, prevY, x, y, 0xFF00D2FF); // ネオンブルー
            prevX = x;
            prevY = y;
        }
    }

    private void drawLine(GuiGraphicsExtractor gui, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            gui.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = err * 2;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 背景外枠パネルを描画
        drawBevel(guiGraphics, centerX - 130, centerY + PANEL_TOP, centerX + 130, centerY + PANEL_BOTTOM, false);

        if (this.isLoading) {
            guiGraphics.centeredText(this.font, this.statusMessage, centerX, centerY, 0xFF94A3B8);
            super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

        if (this.stocks.isEmpty()) {
            guiGraphics.centeredText(this.font, EconomyMasterI18n.trs("economy.ui.etf.none"), centerX, centerY, 0xFF94A3B8);
            super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

        StockData stock = this.stocks.get(this.selectedIndex);

        if (this.activeTab == 0) {
            if (this.showPriceList) {
                // 【価格一覧の描画】

                // ── タイトル + 列ヘッダー統合パネル ──
                drawBevel(guiGraphics, centerX - 125, centerY - 54, centerX + 125, centerY - 32, true);
                guiGraphics.centeredText(this.font, "ETF 価格一覧", centerX, centerY - 51, 0xFFFBBF24);
                // タイトル下の区切り線
                guiGraphics.fill(centerX - 124, centerY - 43, centerX + 124, centerY - 42, 0x1AFFFFFF);
                // 列ヘッダーテキスト (明るめのグレーで視認性向上)
                guiGraphics.text(this.font, "銘柄名 / コード", centerX - 120, centerY - 41, 0xFF94A3B8, false);
                guiGraphics.text(this.font, "現在値", centerX + 20, centerY - 41, 0xFF94A3B8, false);
                guiGraphics.text(this.font, "保有", centerX + 60, centerY - 41, 0xFF94A3B8, false);

                // ── 一覧エリア ──
                final int LIST_LEFT  = centerX - 125;
                final int LIST_RIGHT = centerX + 92;
                final int listTop    = centerY - 28;
                final int listBottom = centerY + 58;
                drawBevel(guiGraphics, LIST_LEFT, listTop, LIST_RIGHT, listBottom, true);

                final int ROW_H = 14;
                int visibleRows = Math.min(PRICE_LIST_ROWS_VISIBLE, this.stocks.size() - this.priceListScroll);
                for (int i = 0; i < visibleRows; i++) {
                    int idx = this.priceListScroll + i;
                    if (idx >= this.stocks.size()) break;
                    StockData s = this.stocks.get(idx);
                    int rowY = listTop + 1 + i * ROW_H;

                    // 行ストライプ背景
                    int rowBg = (i % 2 == 0) ? 0x0FFFFFFF : 0x05FFFFFF;
                    guiGraphics.fill(LIST_LEFT + 1, rowY, LIST_RIGHT - 1, rowY + ROW_H - 1, rowBg);

                    // 銘柄名・コード
                    String nameLabel = s.localizedName() + " (" + EconomyMasterI18n.tr("economy.etf.code." + s.code).getString() + ")";
                    guiGraphics.text(this.font, nameLabel, centerX - 120, rowY + 2, 0xFFE2E8F0, false);

                    // 現在価格 (エメラルドグリーンやオレンジで見やすく)
                    String priceLabel = EconomyMasterI18n.formatCurrency(s.currentPrice);
                    int priceW = this.font.width(priceLabel);
                    guiGraphics.text(this.font, priceLabel, centerX + 55 - priceW, rowY + 2, 0xFFFBBF24, false);

                    // 保有ポジション
                    String posLabel;
                    int posColor;
                    if (s.portfolioQuantity > 0) {
                        posLabel = "+" + s.portfolioQuantity + "\u53e3";
                        posColor = 0xFF10B981; // エメラルドグリーン
                    } else if (s.portfolioQuantity < 0) {
                        posLabel = "\u7a7a" + Math.abs(s.portfolioQuantity) + "\u53e3";
                        posColor = 0xFFF43F5E; // コーラルレッド
                    } else {
                        posLabel = "--";
                        posColor = 0xFF64748B; // ブルーグレー
                    }
                    guiGraphics.text(this.font, posLabel, centerX + 60, rowY + 2, posColor, false);
                }

                // スクロールインジケーター
                int totalStocks = this.stocks.size();
                if (totalStocks > PRICE_LIST_ROWS_VISIBLE) {
                    String scrollInfo = (this.priceListScroll + 1) + "-"
                        + Math.min(this.priceListScroll + PRICE_LIST_ROWS_VISIBLE, totalStocks)
                        + "/" + totalStocks;
                    guiGraphics.centeredText(this.font, scrollInfo, centerX + 108, centerY + 10, 0xFF64748B);
                }

                // ── 価格変動ルール説明 ──
                drawBevel(guiGraphics, centerX - 125, centerY + 61, centerX + 125, centerY + 71, true);
                guiGraphics.centeredText(this.font,
                    "\u4fa1\u683c\u5909\u52d5: NPC\u3068\u306e\u30a2\u30a4\u30c6\u30e0\u58f2\u8cb7\u30fb\u5e02\u5834\u306e\u81ea\u7136\u5909\u52d5\u3067\u5909\u5316\u3057\u307e\u3059",
                    centerX, centerY + 63, 0xFF94A3B8);
            } else if (this.showComponentList) {
                // 【構成アイテムの描画】
                // ── タイトル + 列ヘッダー統合パネル ──
                drawBevel(guiGraphics, centerX - 125, centerY - 54, centerX + 125, centerY - 32, true);
                guiGraphics.centeredText(this.font,
                        EconomyMasterI18n.tr("economy.ui.etf_components", stock.localizedName()).getString(),
                        centerX, centerY - 51, 0xFFFBBF24);
                // タイトル下の区切り線
                guiGraphics.fill(centerX - 124, centerY - 43, centerX + 124, centerY - 42, 0x1AFFFFFF);
                // 列ヘッダーテキスト
                guiGraphics.text(this.font, "アイテム名 / キー", centerX - 120, centerY - 41, 0xFF94A3B8, false);
                guiGraphics.text(this.font, "影響度（ウェイト）", centerX + 25, centerY - 41, 0xFF94A3B8, false);

                // ── 一覧エリア ──
                final int LIST_LEFT  = centerX - 125;
                final int LIST_RIGHT = centerX + 92;
                final int listTop    = centerY - 28;
                final int listBottom = centerY + 58;
                drawBevel(guiGraphics, LIST_LEFT, listTop, LIST_RIGHT, listBottom, true);

                if (this.isComponentsLoading) {
                    guiGraphics.centeredText(this.font, "読み込み中...", centerX - 16, centerY + 10, 0xFF64748B);
                } else if (this.componentItems.isEmpty()) {
                    guiGraphics.centeredText(this.font, "構成アイテムがありません", centerX - 16, centerY + 10, 0xFF64748B);
                } else {
                    final int ROW_H = 14;
                    int visibleRows = Math.min(PRICE_LIST_ROWS_VISIBLE, this.componentItems.size() - this.componentListScroll);
                    for (int i = 0; i < visibleRows; i++) {
                        int idx = this.componentListScroll + i;
                        if (idx >= this.componentItems.size()) break;
                        ComponentData comp = this.componentItems.get(idx);
                        int rowY = listTop + 1 + i * ROW_H;

                        // 行ストライプ背景
                        int rowBg = (i % 2 == 0) ? 0x0FFFFFFF : 0x05FFFFFF;
                        guiGraphics.fill(LIST_LEFT + 1, rowY, LIST_RIGHT - 1, rowY + ROW_H - 1, rowBg);

                        // アイテム名（日本語ローカライズ取得）
                        String itemLabel = getLocalizedItemName(comp.itemKey);
                        guiGraphics.text(this.font, itemLabel, centerX - 120, rowY + 2, 0xFFE2E8F0, false);

                        // 影響度（ウェイト、％表示）
                        int pct = (int) Math.round(comp.influenceWeight * 100);
                        String weightLabel = pct + "%";
                        int weightW = this.font.width(weightLabel);
                        guiGraphics.text(this.font, weightLabel, centerX + 85 - weightW, rowY + 2, 0xFFFBBF24, false);
                    }

                    // スクロールインジケーター
                    int totalComps = this.componentItems.size();
                    if (totalComps > PRICE_LIST_ROWS_VISIBLE) {
                        String scrollInfo = (this.componentListScroll + 1) + "-"
                            + Math.min(this.componentListScroll + PRICE_LIST_ROWS_VISIBLE, totalComps)
                            + "/" + totalComps;
                        guiGraphics.centeredText(this.font, scrollInfo, centerX + 108, centerY + 10, 0xFF64748B);
                    }
                }

                // ── 構成アイテム説明 ──
                drawBevel(guiGraphics, centerX - 125, centerY + 61, centerX + 125, centerY + 71, true);
                guiGraphics.centeredText(this.font,
                    "対象アイテムをショップで売買すると株価が変動します",
                    centerX, centerY + 63, 0xFF94A3B8);
            } else {
                // 【市場情報チャートの描画】
                drawBevel(guiGraphics, centerX - 125, centerY - 58, centerX + 125, centerY - 38, true);
                guiGraphics.centeredText(this.font,
                        EconomyMasterI18n.tr("economy.ui.etf_price", stock.localizedName(), EconomyMasterI18n.tr("economy.etf.code." + stock.code).getString(),
                                EconomyMasterI18n.formatCurrency(stock.currentPrice)).getString(),
                        centerX, centerY - 48, 0xFFFBBF24);

                // ポジション情報
                String positionText;
                if (stock.portfolioQuantity > 0) {
                    positionText = EconomyMasterI18n.tr("economy.ui.etf.position_own", stock.portfolioQuantity).getString();
                } else if (stock.portfolioQuantity < 0) {
                    positionText = EconomyMasterI18n.tr("economy.ui.etf.position_short", Math.abs(stock.portfolioQuantity)).getString();
                } else {
                    positionText = EconomyMasterI18n.trs("economy.ui.etf.position_none");
                }
                guiGraphics.centeredText(this.font, positionText, centerX, centerY - 28, 0xFFE2E8F0);

                // 株価推移チャート
                drawPriceChart(guiGraphics, centerX, centerY);
            }
        } else {
            // 【通常取引 (Tab 1) / 空売り取引 (Tab 2) タブの描画】
            drawBevel(guiGraphics, centerX - 125, centerY - 58, centerX + 125, centerY - 38, true);
            guiGraphics.centeredText(this.font,
                    EconomyMasterI18n.tr("economy.ui.etf_spot", stock.localizedName(), EconomyMasterI18n.tr("economy.etf.code." + stock.code).getString(),
                            EconomyMasterI18n.formatCurrency(stock.currentPrice)).getString(),
                    centerX, centerY - 48, 0xFFFBBF24);

            // 簡単な説明文の追加 (§7を外して見やすいグレーに変更)
            if (this.activeTab == 1) {
                guiGraphics.centeredText(this.font, "通常取引: 安く買って高く売ろう", centerX, centerY - 33, 0xFF94A3B8);
            } else if (this.activeTab == 2) {
                guiGraphics.centeredText(this.font, "空売り取引: 高く売って安く買い戻そう", centerX, centerY - 33, 0xFF94A3B8);
            }

            // 取引設定テキスト (明るい白グレーでコントラストを最大化)
            guiGraphics.centeredText(this.font,
                    EconomyMasterI18n.tr("economy.ui.etf_trade_type", tradeLabel(selectedTradeType)).getString(),
                    centerX, centerY - 24, 0xFFE2E8F0);
            guiGraphics.centeredText(this.font,
                    EconomyMasterI18n.tr("economy.ui.etf_qty", tradeQuantity).getString(),
                    centerX, centerY + ROW_QTY_LABEL, 0xFFE2E8F0);

            // 取引概算金額（実行ボタンの直上に表示して重ならないようにする）
            int cost = stock.currentPrice * tradeQuantity;
            String estimateKey = (selectedTradeType == 0 || selectedTradeType == 3)
                    ? "economy.ui.etf_estimate_pay"
                    : "economy.ui.etf_estimate_gain";
            guiGraphics.centeredText(this.font,
                    EconomyMasterI18n.tr(estimateKey, YEN_FORMAT.format(cost)).getString(),
                    centerX, centerY + ROW_COST, 0xFFFBBF24);
        }

        // --- 画面共通下部パーツ ---
        // 所持金・取引可能額表示
        if (!(this.activeTab == 0 && (this.showPriceList || this.showComponentList))) {
            int balanceY = centerY + ROW_BALANCE;
            drawBevel(guiGraphics, centerX - 125, balanceY, centerX + 125, balanceY + 14, true);
            String balanceText = EconomyMasterI18n.tr("economy.ui.balance",
                    YEN_FORMAT.format(EconomyCommon.getCurrentBalance())).getString();
            if (this.activeTab == 2) {
                guiGraphics.text(this.font, balanceText, centerX - 120, balanceY + 3, 0xFF10B981, false);
                guiGraphics.text(this.font, "\u53d6\u5f15\u53ef\u80fd\u984d: \u00a5" + YEN_FORMAT.format(stock.shortSellAvailable), centerX + 10, balanceY + 3, 0xFFFBBF24, false);
            } else {
                guiGraphics.centeredText(this.font, balanceText, centerX, balanceY + 3, 0xFF10B981);
            }
        }

        // 処理中表示
        if (this.isProcessing) {
            guiGraphics.centeredText(this.font, EconomyMasterI18n.trs("economy.ui.processing"), centerX, centerY + ROW_CLOSE - 12, 0xFFF43F5E);
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isLoading || this.isProcessing || this.activeTab != 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int listLeft = centerX - 125;
        int listRight = centerX + 92;
        int listTop = centerY - 28;
        int listBottom = centerY + 58;
        if (!isMouseOver(mouseX, mouseY, listLeft, listTop, listRight, listBottom)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        if (this.showPriceList && this.stocks.size() > PRICE_LIST_ROWS_VISIBLE) {
            int maxScroll = Math.max(0, this.stocks.size() - PRICE_LIST_ROWS_VISIBLE);
            int next = this.priceListScroll - (int) Math.signum(scrollY);
            next = Math.max(0, Math.min(next, maxScroll));
            if (next != this.priceListScroll) {
                this.priceListScroll = next;
                rebuildWidgets();
                return true;
            }
        } else if (this.showComponentList && !this.isComponentsLoading && !this.componentItems.isEmpty()
                && this.componentItems.size() > PRICE_LIST_ROWS_VISIBLE) {
            int maxScroll = Math.max(0, this.componentItems.size() - PRICE_LIST_ROWS_VISIBLE);
            int next = this.componentListScroll - (int) Math.signum(scrollY);
            next = Math.max(0, Math.min(next, maxScroll));
            if (next != this.componentListScroll) {
                this.componentListScroll = next;
                rebuildWidgets();
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static boolean isMouseOver(double mouseX, double mouseY, int x1, int y1, int x2, int y2) {
        return mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2;
    }

    private String getLocalizedItemName(String itemKey) {
        return EconomyMasterI18n.itemName(itemKey, itemKey);
    }
}
