package com.ogatamizuki.economy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import com.mojang.serialization.MapCodec;

public class AtmBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<AtmBlock> CODEC = simpleCodec(AtmBlock::new);

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public AtmBlock(BlockBehaviour.Properties properties) {
        super(properties);
        // デフォルトの向きを北に設定
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!EconomyMod.isEconomyReady()) {
            if (level.isClientSide()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[経済] 経済データの同期が完了していないため、ATMを利用できません。"));
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        if (level.isClientSide()) {
            // クライアント側でATM画面を開く (サーバーでのクラッシュ防止のためClientAccessを経由)
            ClientAccess.openAtmScreen();
        }
        return InteractionResult.SUCCESS;
    }

    // 設置時にプレイヤーの向きに合わせてブロックの向きを決定
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    // ブロックステートのプロパティ定義に FACING を追加
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
