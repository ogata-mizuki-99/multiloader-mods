package com.ogatamizuki.lookalike.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.UUID;

public class ClientAccess {
    public static void cancelRadialMenu() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> LookalikeRadialSession.closeWithoutSelecting(mc));
    }

    public static PlayerSkin getDisguiseSkin(UUID uuid) {
        return LookalikeClientSkins.getDisguiseSkin(uuid);
    }

    public static boolean isShadowPlayer(UUID uuid) {
        return LookalikeClientShadows.isShadow(uuid);
    }

    public static PlayerSkin getShadowSkin() {
        return LookalikeClientSkins.getShadowSkin();
    }
}
