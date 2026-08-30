package com.ogatamizuki.deconstructor;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class EnchantmentManagerBlockEntity extends BlockEntity implements MenuProvider {
    public int time;
    public float flip;
    public float oFlip;
    public float flipT;
    public float flipA;
    public float open;
    public float oOpen;
    public float rot;
    public float oRot;
    public float tRot;
    private static final java.util.Random RANDOM = new java.util.Random();

    private final SimpleContainer inventory = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            EnchantmentManagerBlockEntity.this.setChanged();
        }
    };

    public EnchantmentManagerBlockEntity(BlockPos pos, BlockState state) {
        super(DeconstructorBlocks.ENCHANT_MANAGER_BLOCK_ENTITY_TYPE.get(), pos, state);
    }

    public static void bookAnimationTick(Level level, BlockPos pos, BlockState state, EnchantmentManagerBlockEntity blockEntity) {
        blockEntity.oOpen = blockEntity.open;
        blockEntity.oRot = blockEntity.rot;
        boolean needsPlayerLookup = blockEntity.open > 0.01F || level.getGameTime() % 4L == 0L;
        Player player = needsPlayerLookup
                ? level.getNearestPlayer((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, 3.0D, false)
                : null;
        if (player != null) {
            double d0 = player.getX() - ((double)pos.getX() + 0.5D);
            double d1 = player.getZ() - ((double)pos.getZ() + 0.5D);
            blockEntity.tRot = (float)Mth.atan2(d1, d0);
            blockEntity.open += 0.1F;
            if (blockEntity.open < 0.5F || RANDOM.nextInt(40) == 0) {
                float f1 = blockEntity.flipT;

                do {
                    blockEntity.flipT += (float)(RANDOM.nextInt(4) - RANDOM.nextInt(4));
                } while(f1 == blockEntity.flipT);
            }
        } else {
            blockEntity.tRot += 0.02F;
            blockEntity.open -= 0.1F;
        }

        while(blockEntity.rot >= (float)Math.PI) {
            blockEntity.rot -= ((float)Math.PI * 2F);
        }

        while(blockEntity.rot < -(float)Math.PI) {
            blockEntity.rot += ((float)Math.PI * 2F);
        }

        while(blockEntity.tRot >= (float)Math.PI) {
            blockEntity.tRot -= ((float)Math.PI * 2F);
        }

        while(blockEntity.tRot < -(float)Math.PI) {
            blockEntity.tRot += ((float)Math.PI * 2F);
        }

        float f2;
        for(f2 = blockEntity.tRot - blockEntity.rot; f2 >= (float)Math.PI; f2 -= ((float)Math.PI * 2F)) {
        }

        while(f2 < -(float)Math.PI) {
            f2 += ((float)Math.PI * 2F);
        }

        blockEntity.rot += f2 * 0.4F;
        blockEntity.open = Mth.clamp(blockEntity.open, 0.0F, 1.0F);
        ++blockEntity.time;
        blockEntity.oFlip = blockEntity.flip;
        float f = (blockEntity.flipT - blockEntity.flip) * 0.4F;
        f = Mth.clamp(f, -0.2F, 0.2F);
        blockEntity.flipA += (f - blockEntity.flipA) * 0.9F;
        blockEntity.flip += blockEntity.flipA;
    }

    public ItemStack getItem(int slot) {
        return inventory.getItem(slot);
    }

    public void setItem(int slot, ItemStack stack) {
        setItemIfChanged(slot, stack);
    }

    public void setItemIfChanged(int slot, ItemStack stack) {
        ItemStack current = inventory.getItem(slot);
        if (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() == stack.getCount()) {
            return;
        }
        this.inventory.setItem(slot, stack);
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.deconstructor.enchant_manager");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new EnchantmentManagerMenu(containerId, playerInventory, this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null && !this.level.isClientSide()) {
            net.minecraft.world.Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), this.getItem(0));
            net.minecraft.world.Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), this.getItem(1));
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput tag) {
        super.saveAdditional(tag);
        var child = tag.child("enchant_manager_inventory");
        if (!this.getItem(0).isEmpty()) {
            child.store("slot_0", ItemStack.OPTIONAL_CODEC, this.getItem(0));
        }
        if (!this.getItem(1).isEmpty()) {
            child.store("slot_1", ItemStack.OPTIONAL_CODEC, this.getItem(1));
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput tag) {
        super.loadAdditional(tag);
        var child = tag.childOrEmpty("enchant_manager_inventory");
        this.inventory.setItem(0, child.read("slot_0", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        this.inventory.setItem(1, child.read("slot_1", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }
}
