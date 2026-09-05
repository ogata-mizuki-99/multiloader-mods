package com.ogatamizuki.economy;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** 経済データ管理用掲示板ブロック（3x2・全員が残高閲覧、管理操作は OP/ソロのみ）。 */
public class EconomyAdminBlock extends Block {
    public static final MapCodec<EconomyAdminBlock> CODEC = simpleCodec(EconomyAdminBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<BoardPart> PART = EnumProperty.create("part", BoardPart.class);

    private static final ThreadLocal<Boolean> BREAKING_STRUCTURE = ThreadLocal.withInitial(() -> false);

    @Override
    protected MapCodec<EconomyAdminBlock> codec() {
        return CODEC;
    }

    public EconomyAdminBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, BoardPart.BOTTOM_CENTER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    public static BlockPos getPosForPart(BlockPos anchor, BoardPart part, Direction facing) {
        Direction right = facing.getClockWise();
        Direction left = facing.getCounterClockWise();
        return switch (part) {
            case BOTTOM_LEFT -> anchor.relative(left, 1);
            case BOTTOM_CENTER -> anchor;
            case BOTTOM_RIGHT -> anchor.relative(right, 1);
            case TOP_LEFT -> anchor.relative(left, 1).above();
            case TOP_CENTER -> anchor.above();
            case TOP_RIGHT -> anchor.relative(right, 1).above();
        };
    }

    public static BlockPos getAnchorPos(BlockPos pos, BoardPart part, Direction facing) {
        Direction right = facing.getClockWise();
        Direction left = facing.getCounterClockWise();
        return switch (part) {
            case BOTTOM_LEFT -> pos.relative(right, 1);
            case BOTTOM_CENTER -> pos;
            case BOTTOM_RIGHT -> pos.relative(left, 1);
            case TOP_LEFT -> pos.relative(right, 1).below();
            case TOP_CENTER -> pos.below();
            case TOP_RIGHT -> pos.relative(left, 1).below();
        };
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        Direction facing = context.getHorizontalDirection().getOpposite();

        for (BoardPart part : BoardPart.values()) {
            BlockPos targetPos = getPosForPart(pos, part, facing);
            if (targetPos.getY() >= level.getMaxY() || targetPos.getY() < level.getMinY()) {
                return null;
            }
            if (!level.getBlockState(targetPos).canBeReplaced(context)) {
                return null;
            }
        }
        return defaultBlockState().setValue(FACING, facing).setValue(PART, BoardPart.BOTTOM_CENTER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) {
            return;
        }
        Direction facing = state.getValue(FACING);
        for (BoardPart part : BoardPart.values()) {
            if (part == BoardPart.BOTTOM_CENTER) {
                continue;
            }
            BlockPos targetPos = getPosForPart(pos, part, facing);
            level.setBlock(targetPos, state.setValue(PART, part), Block.UPDATE_ALL);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !BREAKING_STRUCTURE.get()) {
            breakStructure(level, pos, state, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void destroy(net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockState state) {
        if (level instanceof Level serverLevel && !serverLevel.isClientSide() && !BREAKING_STRUCTURE.get()) {
            breakStructure(serverLevel, pos, state, null);
        }
        super.destroy(level, pos, state);
    }

    private void breakStructure(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        BREAKING_STRUCTURE.set(true);
        try {
            Direction facing = state.getValue(FACING);
            BoardPart currentPart = state.getValue(PART);
            BlockPos anchor = getAnchorPos(pos, currentPart, facing);

            for (BoardPart part : BoardPart.values()) {
                BlockPos targetPos = getPosForPart(anchor, part, facing);
                if (targetPos.equals(pos)) {
                    continue;
                }
                BlockState targetState = level.getBlockState(targetPos);
                if (targetState.is(this)) {
                    if (player != null && player.preventsBlockDrops()) {
                        level.setBlock(targetPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 35);
                    } else {
                        level.destroyBlock(targetPos, true);
                    }
                }
            }
        } finally {
            BREAKING_STRUCTURE.set(false);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            ClientAccess.openAdminScreen();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public enum BoardPart implements StringRepresentable {
        BOTTOM_LEFT("bottom_left"),
        BOTTOM_CENTER("bottom_center"),
        BOTTOM_RIGHT("bottom_right"),
        TOP_LEFT("top_left"),
        TOP_CENTER("top_center"),
        TOP_RIGHT("top_right");

        private final String name;

        BoardPart(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
