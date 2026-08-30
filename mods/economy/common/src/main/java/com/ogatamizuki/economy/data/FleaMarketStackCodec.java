package com.ogatamizuki.economy.data;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** フリマ出品の ItemStack を SNBT / レガシー itemKey から復元する。 */
public final class FleaMarketStackCodec {
    private FleaMarketStackCodec() {
    }

    public static ItemStack template(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return stack.copyWithCount(1);
    }

    public static String encode(HolderLookup.Provider registries, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        DynamicOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        return ItemStack.CODEC.encodeStart(ops, template(stack))
                .result()
                .map(Tag::toString)
                .orElse("");
    }

    public static ItemStack decode(HolderLookup.Provider registries, String snbt, String itemKey, int count) {
        int safeCount = Math.max(1, count);
        if (snbt != null && !snbt.isBlank()) {
            try {
                Tag tag = TagParser.parseCompoundFully(snbt);
                DynamicOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
                ItemStack parsed = ItemStack.CODEC.parse(ops, tag).result().orElse(ItemStack.EMPTY);
                if (!parsed.isEmpty()) {
                    return parsed.copyWithCount(safeCount);
                }
            } catch (CommandSyntaxException ignored) {
                // fall through to itemKey
            }
        }
        return fromItemKey(itemKey, safeCount);
    }

    public static ItemStack fromItemKey(String itemKey, int count) {
        if (itemKey == null || itemKey.isBlank()) {
            return ItemStack.EMPTY;
        }
        try {
            Identifier itemId = Identifier.parse(itemKey);
            Item item = BuiltInRegistries.ITEM.get(itemId)
                    .map(net.minecraft.core.Holder::value)
                    .orElse(Items.AIR);
            if (item == Items.AIR) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(item, Math.max(1, count));
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
}
