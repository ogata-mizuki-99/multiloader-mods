package com.ogatamizuki.elytraslot;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FireworkSlot extends Slot {
    private final Player player;

    public FireworkSlot(Player player, int x, int y) {
        super(new FireworkContainer(player), 0, x, y);
        this.player = player;
    }

    // ── Slot contract overrides ─────────────────────────────────────────────

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.is(Items.FIREWORK_ROCKET);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 64;
    }

    @Override
    public void set(ItemStack stack) {
        this.container.setItem(0, stack);
        if (!this.player.level().isClientSide() && this.player instanceof ServerPlayer serverPlayer) {
            ElytraSlotCommon.syncFireworkSlotToTracking(serverPlayer, stack);
        }
    }

    @Override
    public ItemStack remove(int amount) {
        ItemStack result = this.container.removeItem(0, amount);
        if (!result.isEmpty() && !this.player.level().isClientSide()
                && this.player instanceof ServerPlayer serverPlayer) {
            ElytraSlotCommon.syncFireworkSlotToTracking(serverPlayer, ElytraSlotCommon.getFirework(this.player));
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
        return Identifier.fromNamespaceAndPath("elytra_slot", "container/slot/empty_firework_slot");
    }

    public Player getOwner() {
        return this.player;
    }

    // ── Backing container ───────────────────────────────────────────────────

    static class FireworkContainer extends SimpleContainer {
        private final Player player;

        FireworkContainer(Player player) {
            super(1);
            this.player = player;
        }

        @Override
        public ItemStack getItem(int slot) {
            return ElytraSlotCommon.getFirework(player);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            ElytraSlotCommon.setFirework(player, stack);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            ItemStack current = ElytraSlotCommon.getFirework(player);
            if (current.isEmpty()) return ItemStack.EMPTY;
            ItemStack taken = current.split(count);
            ElytraSlotCommon.setFirework(player, current);
            return taken;
        }

        @Override
        public java.util.List<ItemStack> removeAllItems() {
            ItemStack current = ElytraSlotCommon.getFirework(player);
            ElytraSlotCommon.setFirework(player, ItemStack.EMPTY);
            return current.isEmpty() ? java.util.List.of() : java.util.List.of(current);
        }

        @Override
        public boolean isEmpty() {
            return ElytraSlotCommon.getFirework(player).isEmpty();
        }

        @Override
        public void clearContent() {
            ElytraSlotCommon.setFirework(player, ItemStack.EMPTY);
        }

        @Override
        public void setChanged() {
            // Intentionally empty
        }
    }
}
