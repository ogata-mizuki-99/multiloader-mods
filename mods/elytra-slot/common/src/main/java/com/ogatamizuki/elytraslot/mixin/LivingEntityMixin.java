package com.ogatamizuki.elytraslot.mixin;

import com.ogatamizuki.elytraslot.ElytraSlotCommon;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "getItemBySlot", at = @At("RETURN"), cancellable = true)
    private void onGetItemBySlot(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        if (slot == EquipmentSlot.CHEST && (Object) this instanceof Player player) {
            ItemStack original = cir.getReturnValue();
            if (!original.is(Items.ELYTRA)) {
                // Only override with custom elytra if the call originates from glide/flight logic
                boolean isFlightCheck = false;
                for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                    String methodName = element.getMethodName();
                    if (methodName.equals("canGlide") || methodName.equals("updateFallFlying") || methodName.startsWith("lambda$updateFallFlying$")) {
                        isFlightCheck = true;
                        break;
                    }
                }
                if (isFlightCheck) {
                    if (player.isShiftKeyDown()) {
                        return; // Sneaking/shifting cancels flight by not reporting elytra in the custom slot
                    }
                    ItemStack elytra = ElytraSlotCommon.getElytra(player);
                    if (elytra.is(Items.ELYTRA)) {
                        cir.setReturnValue(elytra);
                    }
                }
            }
        }
    }
}
