package com.ogatamizuki.lookalike.mixin;

import com.mojang.authlib.GameProfile;
import com.ogatamizuki.lookalike.DisguiseManager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Player.class)
public class PlayerMixin {
    @Inject(method = "getGameProfile", at = @At("HEAD"), cancellable = true)
    private void onGetGameProfile(CallbackInfoReturnable<GameProfile> cir) {
        GameProfile disguiseProfile = getDisguiseProfile((Player) (Object) this);
        if (disguiseProfile != null) {
            cir.setReturnValue(disguiseProfile);
        }
    }

    private static GameProfile getDisguiseProfile(Player player) {
        if (player.level() != null && player.level().isClientSide()) {
            return null;
        }
        UUID uuid = player.getUUID();
        if (DisguiseManager.getInstance().isDisguised(uuid)) {
            return DisguiseManager.getInstance().getDisguisedProfile(uuid);
        }
        return null;
    }
}
