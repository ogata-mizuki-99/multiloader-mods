package com.ogatamizuki.economy;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * ショップ画面用のコンポーネント付きアイコン生成（クライアント専用）。
 */
public final class ShopItemDisplayStacks {
    private ShopItemDisplayStacks() {
    }

    public static ItemStack create(
            String itemKey,
            String matchPotion,
            String matchEnchantment,
            Integer matchEnchantmentLevel
    ) {
        if (itemKey == null || itemKey.isBlank()) {
            return ItemStack.EMPTY;
        }
        Identifier id;
        try {
            id = Identifier.parse(itemKey);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
        Item mcItem = BuiltInRegistries.ITEM.get(id).map(Holder::value).orElse(Items.AIR);
        if (mcItem == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(mcItem);
        if (matchPotion != null && !matchPotion.isBlank()) {
            applyPotion(stack, matchPotion);
        }
        if (matchEnchantment != null && !matchEnchantment.isBlank()) {
            applyStoredEnchantment(stack, matchEnchantment, matchEnchantmentLevel != null ? matchEnchantmentLevel : 1);
        }
        return stack;
    }

    private static void applyPotion(ItemStack stack, String potionId) {
        try {
            Identifier id = Identifier.parse(potionId);
            BuiltInRegistries.POTION.get(id).ifPresent(holder ->
                    stack.set(DataComponents.POTION_CONTENTS, new PotionContents(holder)));
        } catch (Exception ignored) {
        }
    }

    private static void applyStoredEnchantment(ItemStack stack, String enchantmentId, int level) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }
            Identifier id = Identifier.parse(enchantmentId);
            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, id);
            var lookup = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            lookup.get(key).ifPresent(holder -> {
                ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                mutable.set(holder, level);
                stack.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
            });
        } catch (Exception ignored) {
        }
    }
}
