package com.ogatamizuki.privatechest;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PlayerHeadBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class LockerBlock extends BaseEntityBlock {
    private static final ThreadLocal<Boolean> BREAKING_STRUCTURE = ThreadLocal.withInitial(() -> false);

    public static final MapCodec<LockerBlock> CODEC = simpleCodec(LockerBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<LockerPart> PART = EnumProperty.create("part", LockerPart.class);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public LockerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, LockerPart.BOTTOM));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() < level.getMaxY() - 2
                && level.getBlockState(pos.above()).canBeReplaced(context)
                && level.getBlockState(pos.above(2)).canBeReplaced(context)) {
            return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(PART, LockerPart.BOTTOM);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) {
            return;
        }

        BlockPos middlePos = pos.above();
        BlockPos headPos = pos.above(2);
        Direction facing = state.getValue(FACING);

        if (placer instanceof Player player) {
            OwnerPlayerHeadUtil.placeWithOwnerProfile(level, headPos, facing, player.getProfile());
        } else if (placer != null) {
            OwnerPlayerHeadUtil.placeWithOwnerProfile(level, headPos, facing, placer.getUUID(), placer.getName().getString());
        } else {
            OwnerPlayerHeadUtil.placeDefaultHead(level, headPos, facing);
        }

        level.setBlock(middlePos, state.setValue(PART, LockerPart.MIDDLE), Block.UPDATE_ALL);

        if (placer instanceof Player player) {
            java.util.UUID uuid = player.getUUID();
            String name = player.getName().getString();

            BlockEntity beBottom = level.getBlockEntity(pos);
            if (beBottom instanceof LockerBlockEntity lockerBe) {
                lockerBe.setOwner(uuid, name);
            }
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

    public void breakStructure(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        breakEntireStructure(level, getBottomPos(pos, state.getValue(PART)), player, pos);
    }

    /** ロッカー3段（下・中・上ヘッド）をまとめて破壊する。{@code skipPos} は呼び出し元で既に破壊処理中のブロック（省略可）。 */
    public void breakEntireStructure(Level level, BlockPos bottom, @Nullable Player player, @Nullable BlockPos skipPos) {
        BlockPos middle = bottom.above();
        BlockPos head = bottom.above(2);
        if (!isLockerStructure(level, bottom, middle, head)) {
            return;
        }

        BREAKING_STRUCTURE.set(true);
        try {
            if (skipPos == null || !skipPos.equals(head)) {
                removePart(level, head, player);
            }
            if (skipPos == null || !skipPos.equals(middle)) {
                removePart(level, middle, player);
            }
            if (skipPos == null || !skipPos.equals(bottom)) {
                removePart(level, bottom, player);
            }
        } finally {
            BREAKING_STRUCTURE.set(false);
        }
    }

    private static void removePart(Level level, BlockPos partPos, @Nullable Player player) {
        BlockState partState = level.getBlockState(partPos);
        if (partState.isAir()) {
            return;
        }
        boolean isHead = partState.getBlock() instanceof PlayerHeadBlock;
        if (player != null && player.preventsBlockDrops()) {
            level.setBlock(partPos, Blocks.AIR.defaultBlockState(), 35);
            level.levelEvent(player, 2001, partPos, Block.getId(partState));
        } else {
            level.destroyBlock(partPos, !isHead);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess scheduledTickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, net.minecraft.util.RandomSource random) {
        if (BREAKING_STRUCTURE.get()) {
            return state;
        }
        LockerPart part = state.getValue(PART);
        if (part == LockerPart.BOTTOM) {
            if (direction == Direction.UP) {
                if (!neighborState.is(this) || neighborState.getValue(PART) != LockerPart.MIDDLE) {
                    return Blocks.AIR.defaultBlockState();
                }
            }
        } else if (part == LockerPart.MIDDLE) {
            if (direction == Direction.DOWN) {
                if (!neighborState.is(this) || neighborState.getValue(PART) != LockerPart.BOTTOM) {
                    return Blocks.AIR.defaultBlockState();
                }
            } else if (direction == Direction.UP) {
                if (!(neighborState.getBlock() instanceof PlayerHeadBlock)) {
                    return Blocks.AIR.defaultBlockState();
                }
            }
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }

    private static boolean isLockerStructure(Level level, BlockPos bottom, BlockPos middle, BlockPos head) {
        BlockState bottomState = level.getBlockState(bottom);
        BlockState middleState = level.getBlockState(middle);
        BlockState headState = level.getBlockState(head);
        return bottomState.is(PrivateChestCommon.LOCKER_BLOCK.get())
                && bottomState.getValue(PART) == LockerPart.BOTTOM
                && middleState.is(PrivateChestCommon.LOCKER_BLOCK.get())
                && middleState.getValue(PART) == LockerPart.MIDDLE
                && headState.getBlock() instanceof PlayerHeadBlock;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos bottomPos = getBottomPos(pos, state.getValue(PART));
        BlockEntity be = level.getBlockEntity(bottomPos);

        if (be instanceof LockerBlockEntity lockerBe) {
            if (lockerBe.isOwner(player)) {
                player.openMenu(lockerBe);
            } else {
                player.sendSystemMessage(
                        Component.translatable("privatechest.message.locked", lockerBe.getOwnerName()).withStyle(ChatFormatting.RED));
            }
        }
        return InteractionResult.SUCCESS;
    }

    private BlockPos getBottomPos(BlockPos pos, LockerPart part) {
        if (part == LockerPart.BOTTOM) {
            return pos;
        }
        return pos.below();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(PART) == LockerPart.BOTTOM) {
            return new LockerBlockEntity(pos, state);
        }
        return null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return false;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 0;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 0;
    }

    @Override
    public boolean ignitedByLava(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return false;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> blockEntityType) {
        if (state.getValue(PART) == LockerPart.BOTTOM) {
            return createTickerHelper(blockEntityType, PrivateChestCommon.LOCKER_BLOCK_ENTITY_TYPE.get(), level.isClientSide() ? LockerBlockEntity::clientTick : LockerBlockEntity::serverTick);
        }
        return null;
    }

    public enum LockerPart implements StringRepresentable {
        BOTTOM("bottom"),
        MIDDLE("middle");

        private final String name;

        LockerPart(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
