package com.ogatamizuki.guide;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;

public final class GuideIcons {
    private GuideIcons() {}

    public static ItemStack resolve(@Nullable Identifier iconId) {
        if (iconId == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(iconId)
                .map(Holder::value)
                .orElse(Items.AIR);
        if (item != Items.AIR) {
            return new ItemStack(item);
        }
        Block block = BuiltInRegistries.BLOCK.get(iconId)
                .map(Holder::value)
                .orElse(null);
        if (block != null) {
            Item blockItem = block.asItem();
            if (blockItem != Items.AIR) {
                return new ItemStack(blockItem);
            }
        }
        return ItemStack.EMPTY;
    }
}
