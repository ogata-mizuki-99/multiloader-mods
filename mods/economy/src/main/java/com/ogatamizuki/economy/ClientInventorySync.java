package com.ogatamizuki.economy;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * サーバー権威の取引後、クライアント側インベントリ表示をサーバー残数と同期します。
 * ショップ画面表示中はサーバーからのインベントリ同期パケットが遅延することがあるため、
 * 取引結果パケットに含まれる残数でクライアントを補正します。
 */
public final class ClientInventorySync {
    private ClientInventorySync() {
    }

    public static void syncItemCount(String itemKey, int targetCount) {
        syncMatchingCount(itemKey, "", "", 0, targetCount);
    }

    public static void syncMatchingCount(
            String itemKey,
            String matchPotion,
            String matchEnchantment,
            int matchEnchantmentLevel,
            int targetCount
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || itemKey == null || itemKey.isEmpty() || targetCount < 0) {
            return;
        }

        try {
            Integer level = matchEnchantment != null && !matchEnchantment.isBlank()
                    ? matchEnchantmentLevel
                    : null;
            int current = EconomyItemMatcher.countMatching(
                    mc.player, itemKey, emptyToNull(matchPotion), emptyToNull(matchEnchantment), level);
            if (current == targetCount) {
                return;
            }
            if (current > targetCount) {
                EconomyItemMatcher.removeMatching(
                        mc.player,
                        itemKey,
                        emptyToNull(matchPotion),
                        emptyToNull(matchEnchantment),
                        level,
                        current - targetCount
                );
            } else {
                addItems(mc.player, itemKey, targetCount - current);
            }
        } catch (Exception e) {
            EconomyMod.LOGGER.error("Failed to sync client inventory for {}", itemKey, e);
        }
    }

    static int countItems(Player player, Identifier itemId) {
        return EconomyItemMatcher.countMatching(player, itemId.toString(), null, null, null);
    }

    private static void addItems(Player player, String itemKey, int quantity) {
        Identifier itemId;
        try {
            itemId = Identifier.parse(itemKey);
        } catch (Exception e) {
            return;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId)
                .map(Holder::value)
                .orElse(Items.AIR);
        if (item == Items.AIR) {
            return;
        }

        int remaining = quantity;
        while (remaining > 0) {
            int stackSize = Math.min(remaining, item.getDefaultMaxStackSize());
            player.getInventory().add(new ItemStack(item, stackSize));
            remaining -= stackSize;
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
