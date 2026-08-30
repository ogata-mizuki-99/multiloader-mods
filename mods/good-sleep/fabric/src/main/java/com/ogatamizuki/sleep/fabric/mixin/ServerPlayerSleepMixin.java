package com.ogatamizuki.sleep.fabric.mixin;

import com.ogatamizuki.sleep.SleepCommon;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Inject(method = "tick", at = @At("TAIL"))
    private void goodSleep$onServerPlayerTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!SleepCommon.allowDaySleep) return;

        // ベッドで 100 tick (5秒) 寝たときに昼間なら夜に進めて起こす
        if (player.isSleeping() && player.isSleepingLongEnough()) {
            ServerLevel level = player.level();
            if (!level.isDarkOutside()) {
                MinecraftServer server = level.getServer();
                if (server != null) {
                    server.getCommands().performPrefixedCommand(
                            server.createCommandSourceStack().withSuppressedOutput(),
                            "time set night"
                    );
                }
                player.stopSleeping();
            }
        }
    }
}
