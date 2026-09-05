package com.ogatamizuki.elytraslot.mixin.client;

import com.ogatamizuki.elytraslot.ElytraSlot;
import com.ogatamizuki.elytraslot.FireworkSlot;
import com.ogatamizuki.elytraslot.client.CustomSlotWrapper;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$SlotWrapper")
public class SlotWrapperMixin implements CustomSlotWrapper {
    @Shadow
    @Final
    private Slot target;

    @Override
    public boolean elytraSlot$isCustomAttachment() {
        return this.target instanceof ElytraSlot || this.target instanceof FireworkSlot;
    }
}
