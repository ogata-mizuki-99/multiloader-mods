package com.ogatamizuki.elytraslot.mixin;

import com.ogatamizuki.elytraslot.ElytraSlot;
import com.ogatamizuki.elytraslot.ElytraSlotCommon;
import com.ogatamizuki.elytraslot.FireworkSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractCraftingMenu {

    private InventoryMenuMixin() {
        super(null, 0, 0, 0);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(Inventory playerInventory, boolean active, Player owner, CallbackInfo ci) {
        // Always add custom slots so client and server keep the same slot count for container sync.
        // Creative visibility is handled via Slot.isActive() / getNoItemIcon(), not by removing slots.
        elytraSlot$addCustomSlotsIfAbsent(owner);
    }

    public void elytraSlot$addCustomSlotsIfAbsent(Player owner) {
        if (ElytraSlotCommon.hasCustomSlots((InventoryMenu) (Object) this)) {
            return;
        }
        int[] pos = ElytraSlotCommon.resolveSlotPositions(owner);
        this.addSlot(new ElytraSlot(owner, pos[0], pos[1]));
        this.addSlot(new FireworkSlot(owner, pos[2], pos[3]));
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void onQuickMoveStack(Player player, int index, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.item.ItemStack> cir) {
        net.minecraft.world.inventory.Slot clickedSlot = this.slots.get(index);
        if (clickedSlot == null || !clickedSlot.hasItem()) return;
        net.minecraft.world.item.ItemStack stack = clickedSlot.getItem();

        // 1. Shift-clicking from player inventory to custom slots
        // Check by slot type: slots 0-8 are crafting/armour, 9-44 are regular inventory, 45 is off-hand
        boolean isPlayerInventorySlot = !(clickedSlot instanceof ElytraSlot) && !(clickedSlot instanceof FireworkSlot);
        if (isPlayerInventorySlot) {
            if (stack.is(net.minecraft.world.item.Items.ELYTRA)) {
                for (net.minecraft.world.inventory.Slot slot : this.slots) {
                    if (slot instanceof ElytraSlot elytraSlot && !elytraSlot.hasItem()) {
                        net.minecraft.world.item.ItemStack moved = stack.copy();
                        elytraSlot.set(moved);
                        clickedSlot.set(net.minecraft.world.item.ItemStack.EMPTY);
                        clickedSlot.setChanged();
                        elytraSlot.setChanged();
                        cir.setReturnValue(net.minecraft.world.item.ItemStack.EMPTY);
                        return;
                    }
                }
            } else if (stack.is(net.minecraft.world.item.Items.FIREWORK_ROCKET)) {
                for (net.minecraft.world.inventory.Slot slot : this.slots) {
                    if (slot instanceof FireworkSlot fireworkSlot && !fireworkSlot.hasItem()) {
                        net.minecraft.world.item.ItemStack moved = stack.copy();
                        fireworkSlot.set(moved);
                        clickedSlot.set(net.minecraft.world.item.ItemStack.EMPTY);
                        clickedSlot.setChanged();
                        fireworkSlot.setChanged();
                        cir.setReturnValue(net.minecraft.world.item.ItemStack.EMPTY);
                        return;
                    }
                }
            }
        }
        // 2. Shift-clicking from custom slots back to player inventory
        else if (clickedSlot instanceof ElytraSlot || clickedSlot instanceof FireworkSlot) {
            if (this.moveItemStackTo(stack, 9, 45, true)) {
                if (stack.isEmpty()) {
                    clickedSlot.set(net.minecraft.world.item.ItemStack.EMPTY);
                }
                clickedSlot.setChanged();
                cir.setReturnValue(net.minecraft.world.item.ItemStack.EMPTY);
                return;
            }
        }
    }
}
