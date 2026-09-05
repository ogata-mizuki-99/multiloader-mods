package com.ogatamizuki.elytraslot;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ElytraSlot extends Slot {
    private final Player player;

    public ElytraSlot(Player player, int x, int y) {
        super(new ElytraContainer(player), 0, x, y);
        this.player = player;
    }

    // ── Slot contract overrides ─────────────────────────────────────────────

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.is(Items.ELYTRA);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    /**
     * Override set() only to send the custom sync packet.
     * The actual data write happens in ElytraContainer.setItem() called by super.set().
     * We deliberately do NOT call this.setChanged() here to prevent the
     * slotsChanged → broadcastChanges chain from advancing stateId unexpectedly,
     * which would cause the server to reject the next client container-click packet.
     */
    @Override
    public void set(ItemStack stack) {
        this.container.setItem(0, stack); // writes via ElytraContainer → ElytraSlotCommon
        if (!this.player.level().isClientSide() && this.player instanceof ServerPlayer serverPlayer) {
            ElytraSlotCommon.syncSlotToTracking(serverPlayer, stack);
        }
    }

    /**
     * Override remove() to also send the sync packet after removal.
     * Delegates the actual split/write to ElytraContainer.removeItem().
     */
    @Override
    public ItemStack remove(int amount) {
        ItemStack result = this.container.removeItem(0, amount);
        if (!result.isEmpty() && !this.player.level().isClientSide()
                && this.player instanceof ServerPlayer serverPlayer) {
            ElytraSlotCommon.syncSlotToTracking(serverPlayer, ElytraSlotCommon.getElytra(this.player));
        }
        return result;
    }

    @Override
    public void setByPlayer(ItemStack stack, ItemStack previousStack) {
        this.set(stack);
    }

    // ── Visibility / icon ───────────────────────────────────────────────────

    @Override
    public boolean isActive() {
        return CustomSlotVisibility.isActive(this.player);
    }

    @Override
    public Identifier getNoItemIcon() {
        if (!CustomSlotVisibility.isActive(this.player)) {
            return null;
        }
        return Identifier.fromNamespaceAndPath("elytra_slot", "container/slot/empty_elytra_slot");
    }

    public Player getOwner() {
        return this.player;
    }

    // ── Backing container ───────────────────────────────────────────────────

    /**
     * A SimpleContainer whose getItem/setItem/removeItem delegate directly to
     * ElytraSlotCommon (the platform data-attachment store), bypassing the internal
     * NonNullList so that Slot.getItem() always reads the authoritative value.
     *
     * setChanged() is intentionally a no-op to prevent the listener chain
     * (SimpleContainer → ContainerListener → AbstractContainerMenu.slotsChanged
     * → broadcastChanges) from incrementing stateId outside of the normal
     * click-processing flow.
     */
    static class ElytraContainer extends SimpleContainer {
        private final Player player;

        ElytraContainer(Player player) {
            super(1);
            this.player = player;
        }

        @Override
        public ItemStack getItem(int slot) {
            return ElytraSlotCommon.getElytra(player);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            ElytraSlotCommon.setElytra(player, stack);
            // Deliberately skip super.setItem() and setChanged()
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            ItemStack current = ElytraSlotCommon.getElytra(player);
            if (current.isEmpty()) return ItemStack.EMPTY;
            ItemStack taken = current.split(count);
            ElytraSlotCommon.setElytra(player, current);
            return taken;
        }

        @Override
        public java.util.List<ItemStack> removeAllItems() {
            ItemStack current = ElytraSlotCommon.getElytra(player);
            ElytraSlotCommon.setElytra(player, ItemStack.EMPTY);
            return current.isEmpty() ? java.util.List.of() : java.util.List.of(current);
        }

        @Override
        public boolean isEmpty() {
            return ElytraSlotCommon.getElytra(player).isEmpty();
        }

        @Override
        public void clearContent() {
            ElytraSlotCommon.setElytra(player, ItemStack.EMPTY);
        }

        /** No-op: prevents stateId from advancing outside click-processing flow. */
        @Override
        public void setChanged() {
            // Intentionally empty – broadcastChanges() is triggered explicitly
            // by vanilla after container clicks via suppressRemoteUpdates flow.
        }
    }
}
