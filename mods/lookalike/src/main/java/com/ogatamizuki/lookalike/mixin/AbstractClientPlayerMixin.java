package com.ogatamizuki.lookalike.mixin;

import com.ogatamizuki.lookalike.client.ClientAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {
    @Inject(method = "getPlayerInfo", at = @At("HEAD"), cancellable = true)
    private void onGetPlayerInfo(CallbackInfoReturnable<PlayerInfo> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(((AbstractClientPlayer) (Object) this).getUUID());
            if (info != null) {
                cir.setReturnValue(info);
            }
        }
    }

    @Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
    private void lookalike$overrideDisguiseSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        UUID uuid = ((AbstractClientPlayer) (Object) this).getUUID();
        if (ClientAccess.isShadowPlayer(uuid)) {
            PlayerSkin shadowSkin = ClientAccess.getShadowSkin();
            if (shadowSkin != null) {
                cir.setReturnValue(shadowSkin);
                return;
            }
        }
        PlayerSkin disguiseSkin = ClientAccess.getDisguiseSkin(uuid);
        if (disguiseSkin != null) {
            cir.setReturnValue(disguiseSkin);
        }
    }
}
