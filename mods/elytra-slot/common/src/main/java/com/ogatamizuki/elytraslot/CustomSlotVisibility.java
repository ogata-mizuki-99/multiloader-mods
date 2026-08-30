package com.ogatamizuki.elytraslot;

import net.minecraft.world.entity.player.Player;

public final class CustomSlotVisibility {
    private static VisibilityCheck check = player -> true;

    private CustomSlotVisibility() {
    }

    public static void setCheck(VisibilityCheck visibilityCheck) {
        check = visibilityCheck;
    }

    public static boolean isActive(Player player) {
        return check.isActive(player);
    }

    @FunctionalInterface
    public interface VisibilityCheck {
        boolean isActive(Player player);
    }
}
