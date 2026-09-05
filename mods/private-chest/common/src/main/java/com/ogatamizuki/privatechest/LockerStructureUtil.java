package com.ogatamizuki.privatechest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PlayerHeadBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class LockerStructureUtil {
    private LockerStructureUtil() {
    }

    /** ロッカー3段（下・中・上ヘッド）のいずれかか */
    public static boolean isLockerStructureBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(PrivateChestCommon.LOCKER_BLOCK.get())) {
            return true;
        }
        if (state.getBlock() instanceof PlayerHeadBlock) {
            BlockPos middlePos = pos.below();
            BlockState middleState = level.getBlockState(middlePos);
            return middleState.is(PrivateChestCommon.LOCKER_BLOCK.get())
                    && middleState.getValue(LockerBlock.PART) == LockerBlock.LockerPart.MIDDLE;
        }
        return false;
    }
}
