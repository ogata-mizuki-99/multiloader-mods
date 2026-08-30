package com.ogatamizuki.economy;

import com.ogatamizuki.economy.data.FleaMarketStackCodec;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * フリマ等のアイテム表示名。
 * {@link ItemStack#getHoverName()} は翻訳 Component のため、クライアント言語で表示される。
 */
public final class EconomyItemDisplayNames {
    private EconomyItemDisplayNames() {
    }

    public static Component resolve(
            HolderLookup.Provider registries,
            String stackNbt,
            String itemKey,
            String fallbackName
    ) {
        ItemStack stack = FleaMarketStackCodec.decode(registries, stackNbt, itemKey, 1);
        if (!stack.isEmpty()) {
            return stack.getHoverName();
        }
        return resolveFromItemKey(itemKey, fallbackName);
    }

    public static Component resolveFromItemKey(String itemKey, String fallbackName) {
        if (itemKey != null && !itemKey.isBlank()) {
            try {
                Identifier id = Identifier.parse(itemKey);
                Item item = BuiltInRegistries.ITEM.get(id).map(net.minecraft.core.Holder::value).orElse(null);
                if (item != null) {
                    return new ItemStack(item).getHoverName();
                }
            } catch (Exception ignored) {
            }
        }
        if (fallbackName != null && !fallbackName.isBlank()) {
            return Component.literal(fallbackName);
        }
        return Component.literal("アイテム");
    }
}
