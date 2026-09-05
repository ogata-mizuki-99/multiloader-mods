package com.ogatamizuki.privatechest;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PlayerHeadItem;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class OwnerPlayerHeadItem extends PlayerHeadItem {
    public OwnerPlayerHeadItem(Properties properties) {
        super(Blocks.PLAYER_HEAD, Blocks.PLAYER_WALL_HEAD, properties);
    }

    @Override
    public net.minecraft.network.chat.Component getName(ItemStack stack) {
        ResolvableProfile profile = stack.get(net.minecraft.core.component.DataComponents.PROFILE);
        if (profile != null && profile.name().isPresent()) {
            return net.minecraft.network.chat.Component.translatable("item.privatechest.owner_player_head.named", profile.name().get());
        }
        return net.minecraft.network.chat.Component.translatable("item.privatechest.owner_player_head");
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        OwnerPlayerProfileUtil.ensureProfile(stack, null);
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (entity instanceof Player player) {
            OwnerPlayerProfileUtil.ensureProfile(stack, player);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        OwnerPlayerProfileUtil.ensureProfile(context.getItemInHand(), context.getPlayer());
        return super.useOn(context);
    }

    public static void applyOwnerProfile(ItemStack stack, Player player) {
        OwnerPlayerProfileUtil.applyOwnerProfile(stack, player);
    }

    public static void applyOwnerProfile(ItemStack stack, ResolvableProfile profile) {
        OwnerPlayerProfileUtil.applyOwnerProfile(stack, profile);
    }

    public static void applyOwnerProfile(ItemStack stack, UUID uuid, String name) {
        OwnerPlayerProfileUtil.applyOwnerProfile(stack, uuid, name);
    }
}
