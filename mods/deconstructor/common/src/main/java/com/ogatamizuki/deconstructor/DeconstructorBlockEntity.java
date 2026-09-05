package com.ogatamizuki.deconstructor;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DeconstructorBlockEntity extends BlockEntity implements MenuProvider {
    private ItemStack inputStack = ItemStack.EMPTY;
    private final int maxExtractCount;

    public DeconstructorBlockEntity(BlockPos pos, BlockState state) {
        super(DeconstructorBlocks.DECONSTRUCTOR_BLOCK_ENTITY_TYPE.get(), pos, state);
        var precision = DeconstructorBlocks.PRECISION_DECONSTRUCTOR.get();
        this.maxExtractCount = (precision != null && state.is(precision)) ? 3 : 1;
    }

    public DeconstructorBlockEntity(BlockPos pos, BlockState state, int maxExtractCount) {
        super(DeconstructorBlocks.DECONSTRUCTOR_BLOCK_ENTITY_TYPE.get(), pos, state);
        this.maxExtractCount = maxExtractCount;
    }

    public ItemStack getInputStack() {
        return inputStack;
    }

    public void setInputStack(ItemStack stack) {
        setInputStackIfChanged(stack);
    }

    public void setInputStackIfChanged(ItemStack stack) {
        if (ItemStack.isSameItemSameComponents(this.inputStack, stack) && this.inputStack.getCount() == stack.getCount()) {
            return;
        }
        this.inputStack = stack.copy();
        setChanged();
    }

    public int getMaxExtractCount() {
        return maxExtractCount;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(maxExtractCount == 3 ? "container.deconstructor.precision_deconstructor" : "container.deconstructor.deconstructor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DeconstructorMenu(containerId, playerInventory, this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null && !this.level.isClientSide()) {
            net.minecraft.world.Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), this.getInputStack());
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput tag) {
        super.saveAdditional(tag);
        if (!this.inputStack.isEmpty()) {
            tag.child("input_inventory").store("item", ItemStack.OPTIONAL_CODEC, this.inputStack);
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput tag) {
        super.loadAdditional(tag);
        this.inputStack = tag.childOrEmpty("input_inventory").read("item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }
}
