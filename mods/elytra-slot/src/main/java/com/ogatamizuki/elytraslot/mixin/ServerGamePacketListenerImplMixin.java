package com.ogatamizuki.elytraslot.mixin;

import com.ogatamizuki.elytraslot.ElytraSlot;
import com.ogatamizuki.elytraslot.FireworkSlot;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla {@code handleSetCreativeModeSlot} only accepts slot indices 1–45.
 * Custom attachment slots are 46/47, so creative placement never reached the server.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"), cancellable = true)
    private void elytraSlot$acceptCustomCreativeSlots(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        if (!this.player.hasInfiniteMaterials()) {
            return;
        }

        int slotNum = packet.slotNum();
        if (slotNum < 0 || slotNum >= this.player.inventoryMenu.slots.size()) {
            return;
        }

        Slot slot = this.player.inventoryMenu.getSlot(slotNum);
        if (!(slot instanceof ElytraSlot || slot instanceof FireworkSlot)) {
            return;
        }

        ItemStack itemStack = packet.itemStack();
        if (!itemStack.isItemEnabled(this.player.level().enabledFeatures())) {
            ci.cancel();
            return;
        }

        boolean validData = itemStack.isEmpty() || itemStack.getCount() <= itemStack.getMaxStackSize();
        if (!validData) {
            ci.cancel();
            return;
        }

        if (!itemStack.isEmpty() && !slot.mayPlace(itemStack)) {
            ci.cancel();
            return;
        }

        slot.setByPlayer(itemStack);
        this.player.inventoryMenu.setRemoteSlot(slotNum, itemStack);
        this.player.inventoryMenu.broadcastChanges();
        ci.cancel();
    }
}
