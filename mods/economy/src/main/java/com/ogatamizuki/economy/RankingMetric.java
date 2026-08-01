package com.ogatamizuki.economy;

/**
 * ランキング表示用のソート項目（コマンド・GUI 共通）
 */
public record RankingMetric(String sortField, String label, boolean isDistance, boolean isTime) {

    public static RankingMetric resolve(String metric) {
        String sortField = "totalMoney";
        String label = "総資産";
        boolean isDistance = false;
        boolean isTime = false;

        if (metric != null) {
            String m = metric.toLowerCase();
            if (m.equals("total") || m.equals("総資産") || m.equals("資産")) {
                sortField = "totalMoney";
                label = "総資産";
            } else if (m.equals("balance") || m.equals("手持ち") || m.equals("所持金") || m.equals("現金")) {
                sortField = "balance";
                label = "手持ち現金";
            } else if (m.equals("bank") || m.equals("銀行") || m.equals("預金")) {
                sortField = "bankBalance";
                label = "銀行残高";
            } else if (m.equals("earnings") || m.equals("獲得額") || m.equals("累計獲得")) {
                sortField = "totalEarnings";
                label = "累計獲得金額";
            } else if (m.equals("lost") || m.equals("ロスト") || m.equals("ロスト額")) {
                sortField = "totalLost";
                label = "累計ロスト金額";
            } else if (m.equals("debt") || m.equals("借金") || m.equals("借金額")) {
                sortField = "totalDebt";
                label = "総借金金額";
            } else if (m.equals("time") || m.equals("時間") || m.equals("参加時間")) {
                sortField = "playTime";
                label = "参加時間";
                isTime = true;
            } else if (m.equals("distance") || m.equals("距離") || m.equals("移動距離")) {
                sortField = "travelDistance";
                label = "移動距離";
                isDistance = true;
            } else if (m.equals("broken") || m.equals("破壊") || m.equals("ブロック破壊")) {
                sortField = "blocksBroken";
                label = "ブロック破壊数";
            } else if (m.equals("deaths") || m.equals("死亡")) {
                sortField = "deaths";
                label = "死亡回数";
            } else if (m.equals("player_kills") || m.equals("プレイヤーキル") || m.equals("pvp")) {
                sortField = "playerKills";
                label = "プレイヤーキル数";
            } else if (m.equals("kills") || m.equals("キル") || m.equals("モブキル")) {
                sortField = "mobKills";
                label = "モブキル数";
            } else if (m.equals("harvest") || m.equals("収穫") || m.equals("収穫数")) {
                sortField = "harvests";
                label = "収穫数";
            } else if (m.equals("potion") || m.equals("ポーション")) {
                sortField = "potionsBrewed";
                label = "ポーション生産量";
            } else if (m.equals("fish") || m.equals("釣り") || m.equals("魚")) {
                sortField = "fishCaught";
                label = "釣った魚の数";
            } else if (m.equals("etf_buy") || m.equals("etf購入")) {
                sortField = "etfBuyAmount";
                label = "ETF累計購入額";
            } else if (m.equals("etf_short") || m.equals("etf空売り")) {
                sortField = "etfShortAmount";
                label = "ETF累計空売り額";
            } else if (m.equals("etf_profit") || m.equals("利益") || m.equals("etf利益")) {
                sortField = "etfProfitAmount";
                label = "ETF利益額";
            } else if (m.equals("etf_trades") || m.equals("etf取引数")) {
                sortField = "totalTradeCount";
                label = "ETF総取引回数";
            }
        }

        return new RankingMetric(sortField, label, isDistance, isTime);
    }

    public static int indexOfSortField(String sortField) {
        for (int i = 0; i < RankingScreen.METRICS.length; i++) {
            if (RankingScreen.METRICS[i].equals(sortField)) {
                return i;
            }
        }
        return 0;
    }

    public String formatValue(double val) {
        java.text.NumberFormat fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale.JAPAN);
        if (isDistance) {
            return fmt.format((long) val) + "m";
        }
        if (isTime) {
            long sec = (long) val;
            long h = sec / 3600;
            long m = (sec % 3600) / 60;
            return h + "時間" + m + "分";
        }
        if (sortField.contains("Money") || sortField.contains("Amount") || sortField.contains("Earnings")
                || sortField.contains("Lost") || sortField.contains("Debt") || sortField.contains("Balance")) {
            return "¥" + fmt.format((long) val);
        }
        return fmt.format((long) val);
    }
}
