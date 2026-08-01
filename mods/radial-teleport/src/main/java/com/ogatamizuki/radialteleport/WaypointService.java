package com.ogatamizuki.radialteleport;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

public final class WaypointService {
    private static final int MAX_NAME_LENGTH = 32;

    private WaypointService() {
    }

    public static boolean saveAtPlayer(ServerPlayer player, String rawName) {
        if (!Config.ENABLE_WAYPOINTS.get()) {
            player.sendSystemMessage(Component.translatable("radial_teleport.message.waypoints_disabled"));
            return false;
        }

        String name = normalizeName(rawName);
        if (name == null) {
            player.sendSystemMessage(Component.translatable("radial_teleport.message.waypoint_invalid_name"));
            return false;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return false;
        }

        PlayerWaypointStorage storage = PlayerWaypointStorage.get(server);
        PlayerWaypoint waypoint = PlayerWaypoint.fromPlayer(name, player);
        if (!storage.addWaypoint(player.getUUID(), waypoint, Config.MAX_WAYPOINTS_PER_PLAYER.get())) {
            player.sendSystemMessage(Component.translatable("radial_teleport.message.waypoint_save_failed"));
            return false;
        }

        player.sendSystemMessage(Component.translatable("radial_teleport.message.waypoint_saved", name));
        refreshDestinationsIfUsingCompass(player, server);
        return true;
    }

    public static boolean deleteByDestinationId(ServerPlayer player, String destinationId) {
        if (!Config.ENABLE_WAYPOINTS.get()) {
            player.sendSystemMessage(Component.translatable("radial_teleport.message.waypoints_disabled"));
            return false;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return false;
        }

        Optional<PlayerWaypoint> waypoint = findWaypoint(player, destinationId);
        if (waypoint.isEmpty()) {
            player.sendSystemMessage(Component.translatable("radial_teleport.message.waypoint_missing"));
            return false;
        }

        PlayerWaypointStorage.get(server).removeWaypointById(player.getUUID(), waypoint.get().waypointId());
        player.sendSystemMessage(Component.translatable("radial_teleport.message.waypoint_deleted", waypoint.get().name()));
        refreshDestinationsIfUsingCompass(player, server);
        return true;
    }

    public static boolean renameByDestinationId(ServerPlayer player, String destinationId, String rawName) {
        if (!Config.ENABLE_WAYPOINTS.get()) {
            player.sendSystemMessage(Component.translatable("radial_teleport.message.waypoints_disabled"));
            return false;
        }

        String name = normalizeName(rawName);
        if (name == null) {
            player.sendSystemMessage(Component.translatable("radial_teleport.message.waypoint_invalid_name"));
            return false;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return false;
        }

        Optional<PlayerWaypoint> waypoint = findWaypoint(player, destinationId);
        if (waypoint.isEmpty()) {
            player.sendSystemMessage(Component.translatable("radial_teleport.message.waypoint_missing"));
            return false;
        }

        if (!PlayerWaypointStorage.get(server).renameWaypoint(player.getUUID(), waypoint.get().waypointId(), name)) {
            player.sendSystemMessage(Component.translatable("radial_teleport.message.waypoint_rename_failed"));
            return false;
        }

        player.sendSystemMessage(Component.translatable("radial_teleport.message.waypoint_renamed", name));
        refreshDestinationsIfUsingCompass(player, server);
        return true;
    }

    public static boolean moveByDestinationId(ServerPlayer player, String destinationId, boolean up) {
        if (!Config.ENABLE_WAYPOINTS.get()) {
            player.sendSystemMessage(Component.translatable("radial_teleport.message.waypoints_disabled"));
            return false;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return false;
        }

        Optional<PlayerWaypoint> waypoint = findWaypoint(player, destinationId);
        if (waypoint.isEmpty()) {
            player.sendSystemMessage(Component.translatable("radial_teleport.message.waypoint_missing"));
            return false;
        }

        int direction = up ? -1 : 1;
        if (!PlayerWaypointStorage.get(server).moveWaypoint(player.getUUID(), waypoint.get().waypointId(), direction)) {
            return false;
        }

        refreshDestinationsIfUsingCompass(player, server);
        return true;
    }

    public static WaypointListPayload buildListPayload(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return new WaypointListPayload(List.of());
        }

        List<WaypointListEntry> entries = PlayerWaypointStorage.get(server).getWaypoints(player.getUUID()).stream()
                .map(WaypointListEntry::from)
                .toList();
        return new WaypointListPayload(entries);
    }

    public static String defaultNameForPlayer(ServerPlayer player) {
        return (int) player.getX() + ", " + (int) player.getY() + ", " + (int) player.getZ();
    }

    private static Optional<PlayerWaypoint> findWaypoint(ServerPlayer player, String destinationId) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return Optional.empty();
        }
        return PlayerWaypointStorage.get(server).findByDestinationId(player.getUUID(), destinationId);
    }

    private static String normalizeName(String rawName) {
        if (rawName == null) {
            return null;
        }
        String trimmed = rawName.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
            return null;
        }
        return trimmed;
    }

    private static void refreshDestinationsIfUsingCompass(ServerPlayer player, MinecraftServer server) {
        boolean using = player.isUsingItem()
                && player.getUseItem().is(RadialTeleportMod.TELEPORT_COMPASS.get());
        if (!using && !player.isSpectator()) {
            return;
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player,
                TeleportService.buildDestinations(server, player)
        );
    }
}
