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

/**
 * NeoForge 26.2 grants custom-slot glide via {@code GLIDING_FLIGHT}, so the old
 * {@code getItemBySlot} intercept no longer runs. Fabric still cancels on Shift via
 * {@code EntityElytraEvents.CUSTOM}; this mixin restores the same cancel for both
 * loaders when glide comes from the custom elytra slot (not chest-slot elytra).
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "canGlide", at = @At("HEAD"), cancellable = true)
    private void elytraSlot$cancelGlideWhenSneaking(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof Player player) || !player.isShiftKeyDown()) {
            return;
        }
        // Chest elytra keeps vanilla behavior; only cancel custom-slot glide.
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            return;
        }
        ItemStack custom = ElytraSlotCommon.getElytra(player);
        if (custom.is(Items.ELYTRA) && custom.getDamageValue() < custom.getMaxDamage() - 1) {
            cir.setReturnValue(false);
        }
    }
}
