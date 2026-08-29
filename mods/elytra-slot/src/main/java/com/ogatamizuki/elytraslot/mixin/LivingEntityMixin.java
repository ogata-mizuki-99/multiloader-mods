package com.ogatamizuki.elytraslot.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixin for {@link LivingEntity}.
 * Previously used to intercept canGlide / getItemBySlot for the dedicated elytra slot,
 * but NeoForge 26.2's canGlide(isNeoForge=true) path bypasses getItemBySlot entirely
 * and relies solely on the GLIDING_FLIGHT attribute.
 * The attribute is now managed directly in {@link com.ogatamizuki.elytraslot.ElytraSlotMod}.
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
}
