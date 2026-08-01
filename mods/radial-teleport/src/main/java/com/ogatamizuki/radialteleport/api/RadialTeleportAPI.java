package com.ogatamizuki.radialteleport.api;

import com.ogatamizuki.radialteleport.RadialTeleportMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * 他 MOD からテレポートコンパス連携するための公開 API。
 */
public final class RadialTeleportAPI {
    private static DestinationPolicyProvider destinationPolicyProvider;

    private RadialTeleportAPI() {
    }

    public record ViewerDestinationPolicy(
            boolean includeSpawn,
            boolean includeWaypoints,
            Predicate<ServerPlayer> includePlayer
    ) {
    }

    @FunctionalInterface
    public interface DestinationPolicyProvider {
        @Nullable
        ViewerDestinationPolicy getPolicy(ServerPlayer viewer);
    }

    public static ItemStack createCompassStack() {
        return RadialTeleportMod.TELEPORT_COMPASS.get().getDefaultInstance();
    }

    public static void setDestinationPolicyProvider(@Nullable DestinationPolicyProvider provider) {
        destinationPolicyProvider = provider;
    }

    @Nullable
    public static ViewerDestinationPolicy resolvePolicy(ServerPlayer viewer) {
        if (destinationPolicyProvider == null) {
            return null;
        }
        return destinationPolicyProvider.getPolicy(viewer);
    }
}
