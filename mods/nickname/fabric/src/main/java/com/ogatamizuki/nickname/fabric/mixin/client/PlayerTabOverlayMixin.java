package com.ogatamizuki.nickname.fabric.mixin.client;

import com.ogatamizuki.nickname.NicknameStorage;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void nickname$overrideTabListName(PlayerInfo playerInfo, CallbackInfoReturnable<Component> cir) {
        if (playerInfo != null && playerInfo.getProfile() != null) {
            String nick = NicknameStorage.getNickname(playerInfo.getProfile().id());
            if (nick != null && !nick.isEmpty()) {
                cir.setReturnValue(Component.literal(nick));
            }
        }
    }
}
