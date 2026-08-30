package com.ogatamizuki.radialteleport;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportCooldowns {
    private static final Map<UUID, Long> LAST_TELEPORT_TICK = new ConcurrentHashMap<>();

    private TeleportCooldowns() {
    }

    public static boolean isReady(ServerPlayer player) {
        int cooldownTicks = Config.getTeleportCooldownTicks();
        if (cooldownTicks <= 0) {
            return true;
        }
        Long lastTick = LAST_TELEPORT_TICK.get(player.getUUID());
        if (lastTick == null) {
            return true;
        }
        return player.level().getGameTime() - lastTick >= cooldownTicks;
    }

    public static int remainingTicks(ServerPlayer player) {
        int cooldownTicks = Config.getTeleportCooldownTicks();
        if (cooldownTicks <= 0) {
            return 0;
        }
        Long lastTick = LAST_TELEPORT_TICK.get(player.getUUID());
        if (lastTick == null) {
            return 0;
        }
        long elapsed = player.level().getGameTime() - lastTick;
        return (int) Math.max(0, cooldownTicks - elapsed);
    }

    public static void markUsed(ServerPlayer player) {
        if (Config.getTeleportCooldownTicks() <= 0) {
            return;
        }
        LAST_TELEPORT_TICK.put(player.getUUID(), player.level().getGameTime());
    }
}
