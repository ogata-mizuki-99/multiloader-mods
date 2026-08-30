package com.ogatamizuki.elytraslot.mixin.client;

import com.ogatamizuki.elytraslot.ElytraSlot;
import com.ogatamizuki.elytraslot.ElytraSlotCommon;
import com.ogatamizuki.elytraslot.FireworkSlot;
import com.ogatamizuki.elytraslot.client.CustomSlotClickHandler;
import com.ogatamizuki.elytraslot.client.CustomSlotWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {

    public CreativeModeInventoryScreenMixin() {
        super(null, null, null);
    }

    @ModifyArgs(
            method = "selectTab",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V"
            )
    )
    private void elytraSlot$useCustomSlotCoordinates(Args args) {
        Slot target = args.get(0);
        if (target instanceof ElytraSlot elytraSlot) {
            int[] pos = ElytraSlotCommon.resolveCreativeSlotPositions(elytraSlot.getOwner());
            args.set(2, pos[0]);
            args.set(3, pos[1]);
        } else if (target instanceof FireworkSlot fireworkSlot) {
            int[] pos = ElytraSlotCommon.resolveCreativeSlotPositions(fireworkSlot.getOwner());
            args.set(2, pos[2]);
            args.set(3, pos[3]);
        }
    }

    private static Slot resolveCustomSlot(Slot slot, Minecraft mc) {
        if (slot instanceof ElytraSlot || slot instanceof FireworkSlot) {
            return slot;
        }
        if (slot instanceof CustomSlotWrapper wrapper && wrapper.elytraSlot$isCustomAttachment()) {
            if (mc.player != null && slot.index >= 0 && slot.index < mc.player.inventoryMenu.slots.size()) {
                Slot candidate = mc.player.inventoryMenu.getSlot(slot.index);
                if (candidate instanceof ElytraSlot || candidate instanceof FireworkSlot) {
                    return candidate;
                }
            }
        }
        return null;
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void elytraSlot$handleCreativeCustomSlotClick(
            Slot slot,
            int slotId,
            int mouseButton,
            ContainerInput containerInput,
            CallbackInfo ci) {
        if (slot == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }

        Slot customSlot = resolveCustomSlot(slot, mc);
        if (customSlot == null) {
            return;
        }

        boolean handled = CustomSlotClickHandler.handle(
                this.getMenu(),
                customSlot,
                mouseButton,
                containerInput,
                mc,
                resolved -> mc.getConnection().send(new ServerboundSetCreativeModeSlotPacket(
                        resolved.index,
                        resolved.getItem()
                ))
        );

        if (handled) {
            ci.cancel();
        }
    }
}
