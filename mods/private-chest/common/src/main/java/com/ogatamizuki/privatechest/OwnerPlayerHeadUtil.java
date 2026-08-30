package com.ogatamizuki.privatechest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;

import java.util.UUID;

public final class OwnerPlayerHeadUtil {
    private OwnerPlayerHeadUtil() {
    }

    public static void placeWithOwnerProfile(Level level, BlockPos pos, Direction lockerFacing, ResolvableProfile profile) {
        if (level.isClientSide()) {
            return;
        }
        ItemStack headStack = new ItemStack(PrivateChestCommon.OWNER_PLAYER_HEAD_ITEM.get());
        headStack.set(DataComponents.PROFILE, profile);
        placeHead(level, pos, lockerFacing, headStack);
    }

    public static void placeWithOwnerProfile(Level level, BlockPos pos, Direction lockerFacing, UUID uuid, String name) {
        if (level.isClientSide()) {
            return;
        }
        ItemStack headStack = new ItemStack(PrivateChestCommon.OWNER_PLAYER_HEAD_ITEM.get());
        OwnerPlayerHeadItem.applyOwnerProfile(headStack, uuid, name);
        placeHead(level, pos, lockerFacing, headStack);
    }

    public static void placeDefaultHead(Level level, BlockPos pos, Direction lockerFacing) {
        if (level.isClientSide()) {
            return;
        }
        ItemStack headStack = new ItemStack(PrivateChestCommon.OWNER_PLAYER_HEAD_ITEM.get());
        OwnerPlayerProfileUtil.ensureProfile(headStack, null);
        placeHead(level, pos, lockerFacing, headStack);
    }

    private static void placeHead(Level level, BlockPos pos, Direction lockerFacing, ItemStack headStack) {
        BlockState headState = Blocks.PLAYER_HEAD.defaultBlockState()
                .setValue(SkullBlock.ROTATION, RotationSegment.convertToSegment(lockerFacing));
        level.setBlock(pos, headState, Block.UPDATE_ALL);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            blockEntity.applyComponentsFromItemStack(headStack);
            blockEntity.setChanged();
            level.sendBlockUpdated(pos, headState, headState, Block.UPDATE_ALL);
        }
    }
}
