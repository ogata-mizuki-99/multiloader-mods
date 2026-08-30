package com.ogatamizuki.economy;

import net.minecraft.network.chat.Component;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * マスタ由来の表示名をクライアント言語で解決する。
 * JSON の日本語名はフォールバック（カスタム上書き・未定義キー用）。
 */
public final class EconomyMasterI18n {
    private EconomyMasterI18n() {
    }

    public static boolean useCents() {
        Locale locale = Locale.getDefault();
        if (EconomyPlatform.isClient()) {
            locale = com.ogatamizuki.economy.client.ClientLocaleHelper.getClientLocale();
        }
        return "en".equals(locale.getLanguage());
    }

    public static String formatCurrency(long amount) {
        Locale locale = Locale.getDefault();
        if (EconomyPlatform.isClient()) {
            locale = com.ogatamizuki.economy.client.ClientLocaleHelper.getClientLocale();
        }
        String format = trs("economy.currency.format");
        if (format.equals("economy.currency.format")) {
            format = useCents() ? "$%s" : "¥%s";
        }
        NumberFormat formatter = getNumberFormatter();
        String formattedNumber;
        if (useCents()) {
            formatter.setMinimumFractionDigits(2);
            formatter.setMaximumFractionDigits(2);
            formattedNumber = formatter.format(amount / 100.0);
        } else {
            formatter.setMinimumFractionDigits(0);
            formatter.setMaximumFractionDigits(0);
            formattedNumber = formatter.format(amount);
        }
        return String.format(format, formattedNumber);
    }

    public static NumberFormat getNumberFormatter() {
        Locale locale = Locale.getDefault();
        if (EconomyPlatform.isClient()) {
            locale = com.ogatamizuki.economy.client.ClientLocaleHelper.getClientLocale();
        }
        return NumberFormat.getNumberInstance(locale);
    }

    public static long parseInputToRawValue(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0L;
        }
        String clean = text.replace(",", "").trim();
        try {
            if (useCents()) {
                double val = Double.parseDouble(clean);
                return Math.round(val * 100.0);
            } else {
                if (clean.contains(".")) {
                    double val = Double.parseDouble(clean);
                    return Math.round(val);
                } else {
                    return Long.parseLong(clean);
                }
            }
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public static String formatRawValueForInput(long rawValue) {
        if (useCents()) {
            NumberFormat f = NumberFormat.getNumberInstance(Locale.US);
            f.setMinimumFractionDigits(2);
            f.setMaximumFractionDigits(2);
            f.setGroupingUsed(false);
            return f.format(rawValue / 100.0);
        } else {
            return String.valueOf(rawValue);
        }
    }

    public static String shopKey(int shopId) {
        return "economy.shop." + shopId;
    }

    public static Component shopNameComponent(int shopId, String fallback) {
        return Component.translatableWithFallback(shopKey(shopId), safeFallback(fallback));
    }

    public static String shopName(int shopId, String fallback) {
        return shopNameComponent(shopId, fallback).getString();
    }

    public static String rewardKey(String actionType) {
        return "economy.reward." + (actionType == null ? "" : actionType);
    }

    public static Component rewardNameComponent(String actionType, String fallback) {
        if (actionType == null || actionType.isBlank()) {
            return Component.literal(safeFallback(fallback));
        }
        return Component.translatableWithFallback(rewardKey(actionType), safeFallback(fallback));
    }

    public static String rewardName(String actionType, String fallback) {
        return rewardNameComponent(actionType, fallback).getString();
    }

    public static String etfNameKey(String code) {
        return "economy.etf." + code;
    }

    public static String etfDescKey(String code) {
        return "economy.etf." + code + ".desc";
    }

    public static Component etfNameComponent(String code, String fallback) {
        if (code == null || code.isBlank()) {
            return Component.literal(safeFallback(fallback));
        }
        return Component.translatableWithFallback(etfNameKey(code), safeFallback(fallback));
    }

    public static String etfName(String code, String fallback) {
        return etfNameComponent(code, fallback).getString();
    }

    public static Component etfDescriptionComponent(String code, String fallback) {
        if (code == null || code.isBlank()) {
            return Component.literal(safeFallback(fallback));
        }
        return Component.translatableWithFallback(etfDescKey(code), safeFallback(fallback));
    }

    public static String etfDescription(String code, String fallback) {
        return etfDescriptionComponent(code, fallback).getString();
    }

    public static Component itemNameComponent(String itemKey, String fallback) {
        return EconomyItemDisplayNames.resolveFromItemKey(itemKey, fallback);
    }

    public static String itemName(String itemKey, String fallback) {
        return itemNameComponent(itemKey, fallback).getString();
    }

    /**
     * ポーション／エンチャント本など同一 itemKey のバリアントは、マスタ {@code name} を優先する。
     * （バニラの「クラフト不可能なポーション」等に潰さない）
     */
    public static String itemName(
            String itemKey,
            String fallback,
            String matchPotion,
            String matchEnchantment
    ) {
        if ((matchPotion != null && !matchPotion.isBlank())
                || (matchEnchantment != null && !matchEnchantment.isBlank())) {
            return safeFallback(fallback).isEmpty() ? itemName(itemKey, fallback) : fallback;
        }
        return itemName(itemKey, fallback);
    }

    public static Component tr(String key) {
        return Component.translatable(key);
    }

    public static String trs(String key) {
        return Component.translatable(key).getString();
    }

    public static Component tr(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static Component parseChatMessage(String messageOrKey) {
        if (messageOrKey == null) return Component.empty();
        if (messageOrKey.startsWith("economy.chat.")) {
            String[] parts = messageOrKey.split("\\|");
            String key = parts[0];
            Object[] args = new Object[parts.length - 1];
            for (int i = 1; i < parts.length; i++) {
                String val = parts[i];
                if (val.startsWith("economy.")) {
                    args[i - 1] = Component.translatable(val);
                } else if (val.contains(":")) {
                    args[i - 1] = EconomyItemDisplayNames.resolveFromItemKey(val, val);
                } else if (val.matches("-?\\d+")) {
                    long num = Long.parseLong(val);
                    if (key.contains("sell_success") && i == 3) {
                        args[i - 1] = formatCurrency(num);
                    } else if (key.contains("trade_success")) {
                        args[i - 1] = formatCurrency(num);
                    } else if (key.contains("list_success") && i == 3) {
                        args[i - 1] = formatCurrency(num);
                    } else if (key.contains("sold_notification") && (i == 2 || i == 3)) {
                        args[i - 1] = formatCurrency(num);
                    } else if (key.contains("borrow_success") && (i == 1 || i == 2)) {
                        args[i - 1] = formatCurrency(num);
                    } else if (key.contains("repay_success") && i == 1) {
                        args[i - 1] = formatCurrency(num);
                    } else if (key.contains("death_penalty")) {
                        args[i - 1] = formatCurrency(num);
                    } else if (key.contains("reward")) {
                        if (i == 2 || i == 4) {
                            args[i - 1] = formatCurrency(num);
                        } else {
                            args[i - 1] = val;
                        }
                    } else {
                        args[i - 1] = getNumberFormatter().format(num);
                    }
                } else {
                    args[i - 1] = val;
                }
            }
            return Component.translatable(key, args);
        }
        return Component.literal(messageOrKey);
    }

    public static Component chatMessage(String messageOrKey) {
        if (messageOrKey != null && messageOrKey.startsWith("economy.")) {
            return parseChatMessage(messageOrKey);
        }
        return Component.literal(messageOrKey == null ? "" : messageOrKey);
    }

    public static String chatMessageString(String messageOrKey) {
        return chatMessage(messageOrKey).getString();
    }

    private static String safeFallback(String fallback) {
        return fallback == null || fallback.isBlank() ? "" : fallback;
    }
}
