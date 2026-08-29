package com.ogatamizuki.elytraslot.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixin for {@link Player}.
 * Previously contained debug hooks for tryToStartFallFlying / canGlide.
 * These are no longer needed: gliding is now enabled by granting the
 * GLIDING_FLIGHT attribute modifier directly via
 * {@link com.ogatamizuki.elytraslot.ElytraSlotMod#applyGlidingAttribute}.
 */
@Mixin(Player.class)
public class PlayerMixin {
}
