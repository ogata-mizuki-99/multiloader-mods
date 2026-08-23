package com.ogatamizuki.economy;

import net.minecraft.network.chat.Component;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * ランキング表示用のソート項目（コマンド・GUI 共通）。
 * 表示名は lang キー（{@code economy.ranking.metric.*}）で解決する。
 */
public record RankingMetric(String sortField, String labelKey, boolean isDistance, boolean isTime) {

    public static RankingMetric forSortField(String sortField) {
        if (sortField == null || sortField.isBlank()) {
            return resolve(null);
        }
        boolean isDistance = "travelDistance".equals(sortField);
        boolean isTime = "playTime".equals(sortField);
        return new RankingMetric(sortField, labelKeyForSortField(sortField), isDistance, isTime);
    }

    public static RankingMetric resolve(String metric) {
        if (metric != null) {
            for (String field : RankingScreen.METRICS) {
                if (field.equalsIgnoreCase(metric)) {
                    return forSortField(field);
                }
            }
        }
        String sortField = "totalMoney";
        String labelKey = "economy.ranking.metric.totalMoney";
        boolean isDistance = false;
        boolean isTime = false;

        if (metric != null) {
            String m = metric.toLowerCase(Locale.ROOT);
            if (m.equals("total") || m.equals("総資産") || m.equals("資産")) {
                sortField = "totalMoney";
                labelKey = "economy.ranking.metric.totalMoney";
            } else if (m.equals("balance") || m.equals("手持ち") || m.equals("所持金") || m.equals("現金")) {
                sortField = "balance";
                labelKey = "economy.ranking.metric.balance";
            } else if (m.equals("bank") || m.equals("銀行") || m.equals("預金")) {
                sortField = "bankBalance";
                labelKey = "economy.ranking.metric.bankBalance";
            } else if (m.equals("earnings") || m.equals("獲得額") || m.equals("累計獲得")) {
                sortField = "totalEarnings";
                labelKey = "economy.ranking.metric.totalEarnings";
            } else if (m.equals("lost") || m.equals("ロスト") || m.equals("ロスト額")) {
                sortField = "totalLost";
                labelKey = "economy.ranking.metric.totalLost";
            } else if (m.equals("debt") || m.equals("借金") || m.equals("借金額")) {
                sortField = "totalDebt";
                labelKey = "economy.ranking.metric.totalDebt";
            } else if (m.equals("time") || m.equals("時間") || m.equals("参加時間")) {
                sortField = "playTime";
                labelKey = "economy.ranking.metric.playTime";
                isTime = true;
            } else if (m.equals("distance") || m.equals("距離") || m.equals("移動距離")) {
                sortField = "travelDistance";
                labelKey = "economy.ranking.metric.travelDistance";
                isDistance = true;
            } else if (m.equals("broken") || m.equals("破壊") || m.equals("ブロック破壊")) {
                sortField = "blocksBroken";
                labelKey = "economy.ranking.metric.blocksBroken";
            } else if (m.equals("deaths") || m.equals("死亡")) {
                sortField = "deaths";
                labelKey = "economy.ranking.metric.deaths";
            } else if (m.equals("player_kills") || m.equals("プレイヤーキル") || m.equals("pvp")) {
                sortField = "playerKills";
                labelKey = "economy.ranking.metric.playerKills";
            } else if (m.equals("kills") || m.equals("キル") || m.equals("モブキル")) {
                sortField = "mobKills";
                labelKey = "economy.ranking.metric.mobKills";
            } else if (m.equals("harvest") || m.equals("収穫") || m.equals("収穫数")) {
                sortField = "harvests";
                labelKey = "economy.ranking.metric.harvests";
            } else if (m.equals("potion") || m.equals("ポーション")) {
                sortField = "potionsBrewed";
                labelKey = "economy.ranking.metric.potionsBrewed";
            } else if (m.equals("fish") || m.equals("釣り") || m.equals("魚")) {
                sortField = "fishCaught";
                labelKey = "economy.ranking.metric.fishCaught";
            } else if (m.equals("etf_buy") || m.equals("etf購入")) {
                sortField = "etfBuyAmount";
                labelKey = "economy.ranking.metric.etfBuyAmount";
            } else if (m.equals("etf_short") || m.equals("etf空売り")) {
                sortField = "etfShortAmount";
                labelKey = "economy.ranking.metric.etfShortAmount";
            } else if (m.equals("etf_profit") || m.equals("利益") || m.equals("etf利益")) {
                sortField = "etfProfitAmount";
                labelKey = "economy.ranking.metric.etfProfitAmount";
            } else if (m.equals("etf_trades") || m.equals("etf取引数")) {
                sortField = "totalTradeCount";
                labelKey = "economy.ranking.metric.totalTradeCount";
            }
        }

        return new RankingMetric(sortField, labelKey, isDistance, isTime);
    }

    public static String labelKeyForSortField(String sortField) {
        return "economy.ranking.metric." + sortField;
    }

    public Component labelComponent() {
        return Component.translatable(labelKey);
    }

    /** クライアント言語で解決した表示名（GUI 用）。 */
    public String label() {
        return labelComponent().getString();
    }

    public static int indexOfSortField(String sortField) {
        for (int i = 0; i < RankingScreen.METRICS.length; i++) {
            if (RankingScreen.METRICS[i].equals(sortField)) {
                return i;
            }
        }
        return 0;
    }

    public Component formatValueComponent(double val) {
        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.ROOT);
        if (isDistance) {
            return Component.literal(fmt.format((long) val) + "m");
        }
        if (isTime) {
            long sec = (long) val;
            long h = sec / 3600;
            long m = (sec % 3600) / 60;
            return Component.translatable("economy.ranking.format_time", h, m);
        }
        if (sortField.contains("Money") || sortField.contains("Amount") || sortField.contains("Earnings")
                || sortField.contains("Lost") || sortField.contains("Debt") || sortField.contains("Balance")
                || sortField.contains("Profit")) {
            return Component.literal("¥" + fmt.format((long) val));
        }
        return Component.literal(fmt.format((long) val));
    }

    public String formatValue(double val) {
        return formatValueComponent(val).getString();
    }
}
