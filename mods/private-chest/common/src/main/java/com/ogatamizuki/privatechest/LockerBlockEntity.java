package com.ogatamizuki.privatechest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.permissions.Permissions;

import java.util.UUID;

public class LockerBlockEntity extends BlockEntity implements MenuProvider, Container {
    public static final int SLOT_COUNT = 81;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    private UUID ownerUuid = new UUID(0, 0);
    private String ownerName = "Unknown";
    private int openCount = 0;
    private final ChestLidController lidController = new ChestLidController();

    public LockerBlockEntity(BlockPos pos, BlockState state) {
        super(PrivateChestCommon.LOCKER_BLOCK_ENTITY_TYPE.get(), pos, state);
    }

    public static void playSound(Level level, BlockPos pos, SoundEvent sound) {
        double x = (double) pos.getX() + 0.5D;
        double y = (double) pos.getY() + 0.5D;
        double z = (double) pos.getZ() + 0.5D;
        level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
    }

    public void setOwner(UUID uuid, String name) {
        this.ownerUuid = uuid;
        this.ownerName = name;
        setChanged();
    }

    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    public String getOwnerName() {
        return this.ownerName;
    }

    public boolean isOwner(Player player) {
        if (player.isCreative()) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                if (serverPlayer.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return player.getUUID().equals(this.ownerUuid);
    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    public void drops() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        for (ItemStack stack : this.items) {
            Containers.dropItemStack(this.level, this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), stack);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        this.drops();
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);
        tag.putString("ownerUuid", this.ownerUuid.toString());
        tag.putString("ownerName", this.ownerName);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput tag) {
        super.loadAdditional(tag);
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items);
        this.ownerUuid = UUID.fromString(tag.getStringOr("ownerUuid", new UUID(0, 0).toString()));
        this.ownerName = tag.getStringOr("ownerName", "Unknown");
    }

    public void startOpen(Player player) {
        if (this.level == null) {
            return;
        }
        if (!player.isSpectator()) {
            this.openCount++;
            this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, this.openCount);
            if (this.openCount == 1) {
                playSound(this.level, this.worldPosition, net.minecraft.sounds.SoundEvents.CHEST_OPEN);
            }
        }
    }

    public void stopOpen(Player player) {
        if (this.level == null) {
            return;
        }
        if (!player.isSpectator()) {
            int prevOpenCount = this.openCount;
            this.openCount = Math.max(0, this.openCount - 1);
            this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, this.openCount);
            if (prevOpenCount > 0 && this.openCount == 0) {
                playSound(this.level, this.worldPosition, net.minecraft.sounds.SoundEvents.CHEST_CLOSE);
            }
        }
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == 1) {
            int prevOpenCount = this.openCount;
            this.openCount = type;
            this.lidController.shouldBeOpen(type > 0);
            if (this.level != null && this.level.isClientSide()) {
                if (type > 0 && prevOpenCount == 0) {
                    playSound(this.level, this.worldPosition, net.minecraft.sounds.SoundEvents.CHEST_OPEN);
                } else if (type == 0 && prevOpenCount > 0) {
                    playSound(this.level, this.worldPosition, net.minecraft.sounds.SoundEvents.CHEST_CLOSE);
                }
            }
            return true;
        }
        return super.triggerEvent(id, type);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, LockerBlockEntity blockEntity) {
        blockEntity.lidController.tickLid();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LockerBlockEntity blockEntity) {
    }

    public float getOpenProgress(float partialTicks) {
        return this.lidController.getOpenness(partialTicks);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.privatechest.locker", this.ownerName);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new LockerMenu(containerId, playerInventory, this);
    }

    // --- Container implementation ---

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        stack.limitSize(this.getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }
}
