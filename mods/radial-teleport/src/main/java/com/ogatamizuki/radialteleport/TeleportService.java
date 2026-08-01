package com.ogatamizuki.radialteleport;

import com.ogatamizuki.radialteleport.api.RadialTeleportAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class TeleportService {
    public static final String SPAWN_DESTINATION_ID = "spawn";
    public static final String WAYPOINT_DESTINATION_PREFIX = "waypoint:";

    private static final AtomicInteger REVISION = new AtomicInteger();

    private TeleportService() {
    }

    public static TeleportDestinationsPayload buildDestinations(MinecraftServer server, ServerPlayer viewer) {
        RadialTeleportAPI.ViewerDestinationPolicy policy = RadialTeleportAPI.resolvePolicy(viewer);
        boolean includeSpawn = policy == null || policy.includeSpawn();
        boolean includeWaypoints = policy == null || policy.includeWaypoints();
        java.util.function.Predicate<ServerPlayer> includePlayer =
                policy == null ? player -> true : policy.includePlayer();

        List<TeleportDestination> destinations = new ArrayList<>();
        if (includeSpawn) {
            destinations.add(new TeleportDestination(
                    SPAWN_DESTINATION_ID,
                    "radial_teleport.destination.spawn",
                    TeleportDestination.KIND_SPAWN
            ));
        }

        if (includeWaypoints && Config.ENABLE_WAYPOINTS.get()) {
            for (PlayerWaypoint waypoint : PlayerWaypointStorage.get(server).getWaypoints(viewer.getUUID())) {
                destinations.add(new TeleportDestination(
                        waypoint.destinationId(),
                        waypoint.name(),
                        TeleportDestination.KIND_WAYPOINT
                ));
            }
        }

        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            if (online.getUUID().equals(viewer.getUUID())) {
                continue;
            }
            if (!includePlayer.test(online)) {
                continue;
            }
            destinations.add(new TeleportDestination(
                    online.getUUID().toString(),
                    online.getGameProfile().name(),
                    TeleportDestination.KIND_PLAYER
            ));
        }

        return new TeleportDestinationsPayload(REVISION.incrementAndGet(), destinations);
    }

    public static TeleportResultPayload teleport(ServerPlayer player, String destinationId) {
        if (!TeleportCooldowns.isReady(player)) {
            int remaining = TeleportCooldowns.remainingTicks(player);
            return TeleportResultPayload.message(false, "radial_teleport.message.cooldown", (remaining + 19) / 20);
        }

        TeleportResultPayload result;
        if (SPAWN_DESTINATION_ID.equals(destinationId)) {
            result = teleportToSpawn(player);
        } else if (destinationId.startsWith(WAYPOINT_DESTINATION_PREFIX)) {
            result = teleportToWaypoint(player, destinationId);
        } else {
            try {
                UUID targetUuid = UUID.fromString(destinationId);
                result = teleportToPlayer(player, targetUuid);
            } catch (IllegalArgumentException e) {
                result = TeleportResultPayload.message(false, "radial_teleport.message.invalid_destination");
            }
        }

        if (result.success()) {
            TeleportCooldowns.markUsed(player);
        }
        return result;
    }

    private static TeleportResultPayload teleportToSpawn(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return TeleportResultPayload.message(false, "radial_teleport.message.teleport_failed");
        }

        ServerLevel overworld = server.overworld();
        LevelData.RespawnData respawnData = overworld.getRespawnData();
        ResourceKey<Level> dimension = respawnData.dimension();
        ServerLevel spawnLevel = server.getLevel(dimension);
        if (spawnLevel == null) {
            spawnLevel = overworld;
        }

        BlockPos spawnPos = respawnData.pos();
        float yaw = respawnData.yaw();
        SafeTeleportPosition.Resolved safe = SafeTeleportPosition.resolve(
                spawnLevel,
                spawnPos.getX() + 0.5D,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5D
        );

        boolean moved = player.teleportTo(
                spawnLevel,
                safe.x(),
                safe.y(),
                safe.z(),
                java.util.Set.of(),
                yaw,
                0.0F,
                true
        );

        if (!moved) {
            return TeleportResultPayload.message(false, "radial_teleport.message.teleport_failed");
        }

        return TeleportResultPayload.message(true, "radial_teleport.message.teleport_spawn");
    }

    private static TeleportResultPayload teleportToWaypoint(ServerPlayer player, String destinationId) {
        if (!Config.ENABLE_WAYPOINTS.get()) {
            return TeleportResultPayload.message(false, "radial_teleport.message.invalid_destination");
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return TeleportResultPayload.message(false, "radial_teleport.message.teleport_failed");
        }

        Optional<PlayerWaypoint> waypoint = PlayerWaypointStorage.get(server)
                .findByDestinationId(player.getUUID(), destinationId);
        if (waypoint.isEmpty()) {
            return TeleportResultPayload.message(false, "radial_teleport.message.waypoint_missing");
        }

        PlayerWaypoint target = waypoint.get();
        ServerLevel targetLevel = server.getLevel(target.dimension());
        if (targetLevel == null) {
            return TeleportResultPayload.message(false, "radial_teleport.message.teleport_failed");
        }

        SafeTeleportPosition.Resolved safe = SafeTeleportPosition.resolve(targetLevel, target.x(), target.y(), target.z());
        boolean moved = player.teleportTo(
                targetLevel,
                safe.x(),
                safe.y(),
                safe.z(),
                java.util.Set.of(),
                target.yaw(),
                target.pitch(),
                true
        );

        if (!moved) {
            return TeleportResultPayload.message(false, "radial_teleport.message.teleport_failed");
        }

        return TeleportResultPayload.message(true, "radial_teleport.message.teleport_waypoint", target.name());
    }

    private static TeleportResultPayload teleportToPlayer(ServerPlayer player, UUID targetUuid) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return TeleportResultPayload.message(false, "radial_teleport.message.teleport_failed");
        }

        ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);
        if (target == null || !target.isAlive()) {
            return TeleportResultPayload.message(false, "radial_teleport.message.player_offline");
        }

        ServerLevel targetLevel = (ServerLevel) target.level();
        SafeTeleportPosition.Resolved safe = SafeTeleportPosition.resolve(
                targetLevel,
                target.getX(),
                target.getY(),
                target.getZ()
        );
        boolean moved = player.teleportTo(
                targetLevel,
                safe.x(),
                safe.y(),
                safe.z(),
                java.util.Set.of(),
                target.getYRot(),
                target.getXRot(),
                true
        );

        if (!moved) {
            return TeleportResultPayload.message(false, "radial_teleport.message.teleport_failed");
        }

        return TeleportResultPayload.message(true, "radial_teleport.message.teleport_player", RadialTeleportNicknameBridge.resolvePlayerName(target));
    }
}
