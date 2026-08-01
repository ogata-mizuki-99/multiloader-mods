package com.ogatamizuki.economy;

import net.minecraft.network.chat.Component;

/**
 * マスタ由来の表示名をクライアント言語で解決する。
 * JSON の日本語名はフォールバック（カスタム上書き・未定義キー用）。
 */
public final class EconomyMasterI18n {
    private EconomyMasterI18n() {
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

    private static String safeFallback(String fallback) {
        return fallback == null || fallback.isBlank() ? "" : fallback;
    }
}
