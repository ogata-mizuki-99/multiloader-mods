package com.ogatamizuki.elytraslot.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Client-side click handling for externally backed Elytra/Firework slots. */
public final class CustomSlotClickHandler {
    @FunctionalInterface
    public interface ServerSync {
        void sync(Slot customSlot);
    }

    private CustomSlotClickHandler() {}

    public static boolean handle(
            AbstractContainerMenu menu,
            Slot customSlot,
            int mouseButton,
            ContainerInput containerInput,
            Minecraft mc,
            ServerSync serverSync) {
        if (containerInput == ContainerInput.PICKUP) {
            return handlePickup(menu, customSlot, mouseButton, serverSync);
        }
        if (containerInput == ContainerInput.QUICK_MOVE) {
            return handleQuickMove(menu, customSlot, mc, serverSync);
        }
        return false;
    }

    private static boolean handlePickup(
            AbstractContainerMenu menu,
            Slot customSlot,
            int mouseButton,
            ServerSync serverSync) {
        ItemStack carried = menu.getCarried();
        ItemStack current = customSlot.getItem();

        if (carried.isEmpty()) {
            if (current.isEmpty()) {
                return false;
            }
            int count = (mouseButton == 1) ? (current.getCount() + 1) / 2 : current.getCount();
            ItemStack taken = customSlot.remove(count);
            menu.setCarried(taken);
            serverSync.sync(customSlot);
            return true;
        }

        if (!customSlot.mayPlace(carried)) {
            return false;
        }

        if (current.isEmpty()) {
            int count = (mouseButton == 1) ? 1 : Math.min(carried.getCount(), customSlot.getMaxStackSize(carried));
            ItemStack placed = carried.copyWithCount(count);
            carried.shrink(count);
            menu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            customSlot.setByPlayer(placed, ItemStack.EMPTY);
            serverSync.sync(customSlot);
            return true;
        }

        if (ItemStack.isSameItemSameComponents(current, carried)) {
            int max = customSlot.getMaxStackSize(carried);
            int space = max - current.getCount();
            if (space <= 0) {
                return false;
            }
            int count = (mouseButton == 1) ? 1 : Math.min(carried.getCount(), space);
            ItemStack merged = current.copy();
            merged.grow(count);
            carried.shrink(count);
            menu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            customSlot.setByPlayer(merged, current);
            serverSync.sync(customSlot);
            return true;
        }

        ItemStack oldSlot = current.copy();
        int placeCount = Math.min(carried.getCount(), customSlot.getMaxStackSize(carried));
        ItemStack placed = carried.copyWithCount(placeCount);
        carried.shrink(placeCount);
        menu.setCarried(oldSlot);
        customSlot.setByPlayer(placed, current);
        serverSync.sync(customSlot);
        return true;
    }

    private static boolean handleQuickMove(
            AbstractContainerMenu menu,
            Slot customSlot,
            Minecraft mc,
            ServerSync serverSync) {
        ItemStack current = customSlot.getItem();
        if (current.isEmpty() || mc.player == null) {
            return false;
        }
        if (mc.player.getInventory().add(current.copy())) {
            customSlot.setByPlayer(ItemStack.EMPTY, current);
            serverSync.sync(customSlot);
            return true;
        }
        return false;
    }
}
