package com.ogatamizuki.deconstructor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class DeconstructorBlock extends BaseEntityBlock {
    private final int maxExtractCount;

    public DeconstructorBlock(Properties properties, int maxExtractCount) {
        super(properties);
        this.maxExtractCount = maxExtractCount;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    public int getMaxExtractCount() {
        return maxExtractCount;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DeconstructorBlockEntity(pos, state, maxExtractCount);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DeconstructorBlockEntity deconstructorBe) {
                // コンテナを開く
                player.openMenu(deconstructorBe);
            }
        }
        return InteractionResult.SUCCESS;
    }


}
