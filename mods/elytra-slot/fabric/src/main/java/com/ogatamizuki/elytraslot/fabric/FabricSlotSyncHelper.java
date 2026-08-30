package com.ogatamizuki.elytraslot.fabric;

import com.ogatamizuki.elytraslot.ElytraSlotCommon;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Reconciles Fabric client cursor state when custom-slot sync races container packets. */
final class FabricSlotSyncHelper {
    private FabricSlotSyncHelper() {}

    static void applyElytraSync(Minecraft mc, Player target, ItemStack synced) {
        ItemStack previous = ElytraSlotCommon.getElytra(target);
        ElytraSlotCommon.setElytra(target, synced);
        reconcileCarriedAfterPlacement(mc, target, synced, previous);
    }

    static void applyFireworkSync(Minecraft mc, Player target, ItemStack synced) {
        ItemStack previous = ElytraSlotCommon.getFirework(target);
        ElytraSlotCommon.setFirework(target, synced);
        reconcileCarriedAfterPlacement(mc, target, synced, previous);
    }

    private static void reconcileCarriedAfterPlacement(
            Minecraft mc, Player target, ItemStack synced, ItemStack previousSlot) {
        if (mc.player != target || mc.player == null) {
            return;
        }
        AbstractContainerMenu menu = mc.player.containerMenu;
        if (menu == null) {
            return;
        }

        ItemStack carried = menu.getCarried();
        if (synced.isEmpty() || carried.isEmpty()) {
            return;
        }
        if (!ItemStack.isSameItemSameComponents(carried, synced)) {
            return;
        }

        int previousCount = previousSlot.isEmpty() ? 0 : previousSlot.getCount();
        int gained = synced.getCount() - previousCount;
        if (gained <= 0) {
            return;
        }

        int shrink = Math.min(carried.getCount(), gained);
        ItemStack next = carried.copy();
        next.shrink(shrink);
        menu.setCarried(next.isEmpty() ? ItemStack.EMPTY : next);
    }
}
