package com.ogatamizuki.sleep.fabric.mixin;

import com.ogatamizuki.sleep.SleepCommon;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public class PlayerSleepMixin {

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/attribute/BedRule;canSleep(Lnet/minecraft/world/level/Level;)Z"
            )
    )
    private boolean goodSleep$overrideCanSleepInTick(BedRule instance, Level level) {
        if (SleepCommon.allowDaySleep) {
            return true;
        }
        return instance.canSleep(level);
    }
}
