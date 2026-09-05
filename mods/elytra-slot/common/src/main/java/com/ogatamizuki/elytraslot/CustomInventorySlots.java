package com.ogatamizuki.elytraslot;

import com.ogatamizuki.elytraslot.mixin.InventoryMenuMixin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;

public final class CustomInventorySlots {
    private CustomInventorySlots() {
    }

    public static void addCustomSlotsIfAbsent(InventoryMenu menu, Player owner) {
        ((InventoryMenuMixin) (Object) menu).elytraSlot$addCustomSlotsIfAbsent(owner);
    }
}
