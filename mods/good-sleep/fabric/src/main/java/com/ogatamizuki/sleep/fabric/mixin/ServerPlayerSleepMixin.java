package com.ogatamizuki.sleep.fabric.mixin;

import com.ogatamizuki.sleep.SleepCommon;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.server.level.ServerPlayer;

/**
 * 昼間でもベッドに入れるよう BedRule を上書きする。
 * 時刻スキップ自体は {@link com.ogatamizuki.sleep.fabric.SleepModFabric} の tick カウンタで行う
 *（isSleepingLongEnough 到達前に起床してしまうケースがあるため）。
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerSleepMixin {

    @Redirect(
            method = "startSleepInBed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/attribute/BedRule;canSleep(Lnet/minecraft/world/level/Level;)Z"
            )
    )
    private boolean goodSleep$overrideCanSleep(BedRule instance, Level level) {
        if (SleepCommon.allowDaySleep) {
            return true;
        }
        return instance.canSleep(level);
    }
}
