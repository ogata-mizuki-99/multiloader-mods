package com.ogatamizuki.economy;

import com.ogatamizuki.economy.master.EconomyMasterData;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Optional;

/**
 * 質屋売却向け: itemKey + ポーション効果 / 格納エンチャントの照合。
 */
public final class EconomyItemMatcher {
    private EconomyItemMatcher() {
    }

    public static boolean hasVariantMatch(EconomyMasterData.ItemDef item) {
        return (item.matchPotion() != null && !item.matchPotion().isBlank())
                || (item.matchEnchantment() != null && !item.matchEnchantment().isBlank());
    }

    public static boolean matches(ItemStack stack, EconomyMasterData.ItemDef item) {
        if (stack == null || stack.isEmpty() || item == null) {
            return false;
        }
        Identifier expectedItem;
        try {
            expectedItem = Identifier.parse(item.itemKey());
        } catch (Exception e) {
            return false;
        }
        if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(expectedItem)) {
            return false;
        }
        if (!hasVariantMatch(item)) {
            return true;
        }
        if (item.matchPotion() != null && !item.matchPotion().isBlank()) {
            return potionId(stack).filter(id -> id.equals(item.matchPotion())).isPresent();
        }
        if (item.matchEnchantment() != null && !item.matchEnchantment().isBlank()) {
            int level = item.matchEnchantmentLevel() != null ? item.matchEnchantmentLevel() : 1;
            return matchesSingleStoredEnchantment(stack, item.matchEnchantment(), level);
        }
        return true;
    }

    /** クライアント側などで ItemDef が無いとき用。 */
    public static boolean matches(
            ItemStack stack,
            String itemKey,
            String matchPotion,
            String matchEnchantment,
            Integer matchEnchantmentLevel
    ) {
        if (stack == null || stack.isEmpty() || itemKey == null || itemKey.isBlank()) {
            return false;
        }
        Identifier expectedItem;
        try {
            expectedItem = Identifier.parse(itemKey);
        } catch (Exception e) {
            return false;
        }
        if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(expectedItem)) {
            return false;
        }
        boolean hasPotion = matchPotion != null && !matchPotion.isBlank();
        boolean hasEnchant = matchEnchantment != null && !matchEnchantment.isBlank();
        if (!hasPotion && !hasEnchant) {
            return true;
        }
        if (hasPotion) {
            return potionId(stack).filter(id -> id.equals(matchPotion)).isPresent();
        }
        int level = matchEnchantmentLevel != null ? matchEnchantmentLevel : 1;
        return matchesSingleStoredEnchantment(stack, matchEnchantment, level);
    }

    public static int countMatching(Player player, EconomyMasterData.ItemDef item) {
        if (player == null || item == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (matches(stack, item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static int countMatching(
            Player player,
            String itemKey,
            String matchPotion,
            String matchEnchantment,
            Integer matchEnchantmentLevel
    ) {
        if (player == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (matches(stack, itemKey, matchPotion, matchEnchantment, matchEnchantmentLevel)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static int removeMatching(Player player, EconomyMasterData.ItemDef item, int quantity) {
        if (player == null || item == null || quantity <= 0) {
            return 0;
        }
        int remaining = quantity;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!matches(stack, item)) {
                continue;
            }
            if (stack.getCount() <= remaining) {
                remaining -= stack.getCount();
                player.getInventory().setItem(i, ItemStack.EMPTY);
            } else {
                stack.shrink(remaining);
                remaining = 0;
            }
        }
        return quantity - remaining;
    }

    public static int removeMatching(
            Player player,
            String itemKey,
            String matchPotion,
            String matchEnchantment,
            Integer matchEnchantmentLevel,
            int quantity
    ) {
        if (player == null || quantity <= 0) {
            return 0;
        }
        int remaining = quantity;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!matches(stack, itemKey, matchPotion, matchEnchantment, matchEnchantmentLevel)) {
                continue;
            }
            if (stack.getCount() <= remaining) {
                remaining -= stack.getCount();
                player.getInventory().setItem(i, ItemStack.EMPTY);
            } else {
                stack.shrink(remaining);
                remaining = 0;
            }
        }
        return quantity - remaining;
    }

    public static Optional<String> potionId(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return Optional.empty();
        }
        return contents.potion().flatMap(holder -> holder.unwrapKey().map(key -> key.identifier().toString()));
    }

    public static boolean matchesSingleStoredEnchantment(ItemStack stack, String enchantmentId, int level) {
        ItemEnchantments enchants = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchants.isEmpty()) {
            return false;
        }
        int entries = 0;
        boolean found = false;
        for (var entry : enchants.entrySet()) {
            entries++;
            Holder<Enchantment> holder = entry.getKey();
            String id = holder.unwrapKey()
                    .map(key -> key.identifier().toString())
                    .orElse("");
            if (enchantmentId.equals(id) && entry.getIntValue() == level) {
                found = true;
            }
        }
        return found && entries == 1;
    }

    public static String matchIdentityKey(EconomyMasterData.ItemDef item) {
        return matchIdentityKey(item.itemKey(), item.matchPotion(), item.matchEnchantment(), item.matchEnchantmentLevel());
    }

    public static String matchIdentityKey(
            String itemKey,
            String matchPotion,
            String matchEnchantment,
            Integer matchEnchantmentLevel
    ) {
        StringBuilder sb = new StringBuilder(itemKey == null ? "" : itemKey);
        if (matchPotion != null && !matchPotion.isBlank()) {
            sb.append("|potion=").append(matchPotion);
        }
        if (matchEnchantment != null && !matchEnchantment.isBlank()) {
            sb.append("|ench=").append(matchEnchantment);
            sb.append("|lvl=").append(matchEnchantmentLevel != null ? matchEnchantmentLevel : 1);
        }
        return sb.toString();
    }
}
