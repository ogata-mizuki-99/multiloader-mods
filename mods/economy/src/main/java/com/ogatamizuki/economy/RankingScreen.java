package com.ogatamizuki.economy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RankingScreen extends Screen {
    private final Screen parentScreen;
    private final List<JsonObject> records = new ArrayList<>();
    
    static final String[] METRICS = {
        "totalMoney", "balance", "bankBalance", "totalEarnings", "totalLost", "totalDebt",
        "playTime", "travelDistance", "blocksBroken", "deaths", "playerKills",
        "mobKills", "harvests", "potionsBrewed", "fishCaught",
        "etfBuyAmount", "etfShortAmount", "etfProfitAmount", "totalTradeCount"
    };

    private static final String[] METRIC_LABELS = {
        "総資産", "手持ち現金", "銀行残高", "累計獲得金額", "累計ロスト金額", "総借金金額",
        "参加時間", "移動距離", "ブロック破壊数", "死亡回数", "プレイヤーキル数",
        "モブキル数", "収穫数", "ポーション生産量", "釣った魚の数",
        "ETF累計購入額", "ETF累計空売り額", "ETF利益額", "ETF総取引回数"
    };
    
    private int selectedMetricIndex = 0;
    private final List<JsonObject> sortedRecords = new ArrayList<>();
    private static final NumberFormat FMT = NumberFormat.getNumberInstance(Locale.JAPAN);

    public RankingScreen(Screen parentScreen, JsonObject rankingData) {
        this(parentScreen, rankingData, null);
    }

    public RankingScreen(Screen parentScreen, JsonObject rankingData, String initialSortField) {
        super(Component.literal("ランキング一覧"));
        this.parentScreen = parentScreen;

        if (rankingData != null && rankingData.has("records")) {
            JsonArray arr = rankingData.getAsJsonArray("records");
            for (JsonElement el : arr) {
                if (el.isJsonObject()) {
                    this.records.add(el.getAsJsonObject());
                }
            }
        }
        if (initialSortField != null) {
            this.selectedMetricIndex = RankingMetric.indexOfSortField(initialSortField);
        }
        sortRecords();
    }

    private void sortRecords() {
        this.sortedRecords.clear();
        this.sortedRecords.addAll(this.records);
        String field = METRICS[selectedMetricIndex];
        this.sortedRecords.sort((a, b) -> {
            double valA = a.has(field) ? a.get(field).getAsDouble() : 0.0;
            double valB = b.has(field) ? b.get(field).getAsDouble() : 0.0;
            return Double.compare(valB, valA); // descending
        });
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 切替ボタン
        this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
            selectedMetricIndex = (selectedMetricIndex - 1 + METRICS.length) % METRICS.length;
            sortRecords();
            rebuildWidgets();
        }).bounds(centerX - 120, centerY - 62, 30, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
            selectedMetricIndex = (selectedMetricIndex + 1) % METRICS.length;
            sortRecords();
            rebuildWidgets();
        }).bounds(centerX + 90, centerY - 62, 30, 20).build());

        // 戻るボタン
        this.addRenderableWidget(Button.builder(Component.literal("戻る"), b -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
        }).bounds(centerX - 40, centerY + 85, 80, 20).build());
    }

    private void drawBevel(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2, boolean sunken) {
        int strokeColor = sunken ? 0x25FFFFFF : 0x40FFFFFF;
        int bgColor = sunken ? 0xFF07090E : 0xFF14161E;
        gui.fill(x1, y1, x2, y2, bgColor);
        gui.fill(x1, y1, x2, y1 + 1, strokeColor);
        gui.fill(x1, y1, x1 + 1, y2, strokeColor);
        gui.fill(x1, y2 - 1, x2, y2, strokeColor);
        gui.fill(x2 - 1, y1, x2, y2, strokeColor);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 背景外枠パネル
        drawBevel(guiGraphics, centerX - 130, centerY - 88, centerX + 130, centerY + 112, false);

        // タイトル
        guiGraphics.centeredText(this.font, "RANKING BOARD", centerX, centerY - 80, 0xFFFFFFFF);

        // カテゴリー表示
        drawBevel(guiGraphics, centerX - 85, centerY - 62, centerX + 85, centerY - 42, true);
        guiGraphics.centeredText(this.font, METRIC_LABELS[selectedMetricIndex], centerX, centerY - 56, 0xFFFBBF24);

        // ランキングリストの枠
        int listTop = centerY - 38;
        int listBottom = centerY + 78;
        drawBevel(guiGraphics, centerX - 120, listTop, centerX + 120, listBottom, true);

        // リスト描画
        int rowH = 11;
        String field = METRICS[selectedMetricIndex];
        
        if (this.sortedRecords.isEmpty()) {
            guiGraphics.centeredText(this.font, "データがありません", centerX, centerY + 10, 0xFF64748B);
        } else {
            for (int i = 0; i < Math.min(10, this.sortedRecords.size()); i++) {
                JsonObject record = this.sortedRecords.get(i);
                int rowY = listTop + 2 + i * rowH;
                
                // 行ストライプ背景
                int rowBg = (i % 2 == 0) ? 0x0FFFFFFF : 0x05FFFFFF;
                guiGraphics.fill(centerX - 119, rowY, centerX + 119, rowY + rowH - 1, rowBg);

                String name = record.has("username") ? record.get("username").getAsString() : "Unknown";
                double val = record.has(field) ? record.get(field).getAsDouble() : 0.0;

                String valStr;
                if (field.equals("travelDistance")) {
                    valStr = FMT.format((long) val) + "m";
                } else if (field.equals("playTime")) {
                    long sec = (long) val;
                    long h = sec / 3600;
                    long m = (sec % 3600) / 60;
                    valStr = h + "時間" + m + "分";
                } else if (field.contains("Money") || field.contains("Amount") || field.contains("Profit") || field.contains("Debt") || field.contains("Earnings") || field.contains("Lost") || field.contains("Balance")) {
                    valStr = "¥" + FMT.format((long) val);
                } else {
                    valStr = FMT.format((long) val);
                }

                // 順位の色
                int rankColor = 0xFFE2E8F0;
                if (i == 0) rankColor = 0xFFFFD700; // 金
                else if (i == 1) rankColor = 0xFFC0C0C0; // 銀
                else if (i == 2) rankColor = 0xFFCD7F32; // 銅

                guiGraphics.text(this.font, (i + 1) + ".", centerX - 112, rowY + 1, rankColor, false);
                guiGraphics.text(this.font, name, centerX - 92, rowY + 1, 0xFFE2E8F0, false);
                
                int valWidth = this.font.width(valStr);
                guiGraphics.text(this.font, valStr, centerX + 112 - valWidth, rowY + 1, 0xFFFBBF24, false);
            }
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }
}
