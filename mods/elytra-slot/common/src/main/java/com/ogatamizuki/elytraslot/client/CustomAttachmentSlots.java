package com.ogatamizuki.elytraslot.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Player;

public final class CustomAttachmentSlots {
    private CustomAttachmentSlots() {
    }

    /**
     * Creative item-picker tab maps extra InventoryMenu slots onto the hotbar row.
     * Show custom slots only on the survival-inventory tab (or outside creative).
     */
    public static boolean shouldRender(Player player) {
        if (!player.isCreative()) {
            return true;
        }

        if (Minecraft.getInstance().screen instanceof CreativeModeInventoryScreen creativeScreen) {
            return creativeScreen.isInventoryOpen();
        }

        return false;
    }
}
