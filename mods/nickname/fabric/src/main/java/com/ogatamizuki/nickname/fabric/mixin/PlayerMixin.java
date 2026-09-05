package com.ogatamizuki.nickname.fabric.mixin;

import com.ogatamizuki.nickname.NicknameStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void nickname$overrideDisplayName(CallbackInfoReturnable<Component> cir) {
        Player self = (Player) (Object) this;
        String nick = NicknameStorage.getNickname(self.getUUID());
        if (nick != null && !nick.isEmpty()) {
            cir.setReturnValue(Component.literal(nick));
        }
    }
}
