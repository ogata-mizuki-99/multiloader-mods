package com.ogatamizuki.radialteleport;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerWaypointStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, List<PlayerWaypoint>> waypointsByPlayer = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;

    private PlayerWaypointStorage() {
    }

    public static PlayerWaypointStorage get(MinecraftServer server) {
        if (!loaded && server != null) {
            load(server);
        }
        return INSTANCE;
    }

    private static final PlayerWaypointStorage INSTANCE = new PlayerWaypointStorage();

    private static File getStorageFile(MinecraftServer server) {
        File dataDir = server.getWorldPath(LevelResource.ROOT).resolve("data").toFile();
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        return new File(dataDir, "radial_teleport_waypoints.json");
    }

    public static synchronized void load(MinecraftServer server) {
        waypointsByPlayer.clear();
        File file = getStorageFile(server);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, List<PlayerWaypointData>>>() {}.getType();
                Map<String, List<PlayerWaypointData>> rawMap = GSON.fromJson(reader, type);
                if (rawMap != null) {
                    for (Map.Entry<String, List<PlayerWaypointData>> entry : rawMap.entrySet()) {
                        try {
                            UUID playerId = UUID.fromString(entry.getKey());
                            List<PlayerWaypoint> list = new ArrayList<>();
                            if (entry.getValue() != null) {
                                for (PlayerWaypointData data : entry.getValue()) {
                                    if (data != null) {
                                        list.add(data.toWaypoint());
                                    }
                                }
                            }
                            waypointsByPlayer.put(playerId, List.copyOf(list));
                        } catch (IllegalArgumentException e) {
                            RadialTeleportCommon.LOGGER.error("Failed to parse UUID key in waypoints JSON: {}", entry.getKey());
                        }
                    }
                }
            } catch (IOException e) {
                RadialTeleportCommon.LOGGER.error("Failed to load waypoints JSON file", e);
            }
        }
        loaded = true;
    }

    public static synchronized void save(MinecraftServer server) {
        if (server == null) {
            return;
        }
        File file = getStorageFile(server);
        try (FileWriter writer = new FileWriter(file)) {
            Map<String, List<PlayerWaypointData>> rawMap = new LinkedHashMap<>();
            for (Map.Entry<UUID, List<PlayerWaypoint>> entry : waypointsByPlayer.entrySet()) {
                List<PlayerWaypointData> dataList = new ArrayList<>();
                for (PlayerWaypoint wp : entry.getValue()) {
                    dataList.add(PlayerWaypointData.fromWaypoint(wp));
                }
                rawMap.put(entry.getKey().toString(), dataList);
            }
            GSON.toJson(rawMap, writer);
        } catch (IOException e) {
            RadialTeleportCommon.LOGGER.error("Failed to save waypoints JSON file", e);
        }
    }

    public List<PlayerWaypoint> getWaypoints(UUID playerId) {
        return List.copyOf(waypointsByPlayer.getOrDefault(playerId, List.of()));
    }

    public boolean addWaypoint(UUID playerId, PlayerWaypoint waypoint, int maxWaypoints) {
        List<PlayerWaypoint> current = new ArrayList<>(getWaypoints(playerId));
        if (current.size() >= maxWaypoints) {
            return false;
        }
        if (current.stream().anyMatch(existing -> existing.name().equalsIgnoreCase(waypoint.name()))) {
            return false;
        }
        current.add(waypoint);
        waypointsByPlayer.put(playerId, List.copyOf(current));
        return true;
    }

    public Optional<PlayerWaypoint> removeWaypoint(UUID playerId, String name) {
        List<PlayerWaypoint> current = new ArrayList<>(getWaypoints(playerId));
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).name().equalsIgnoreCase(name)) {
                PlayerWaypoint removed = current.remove(i);
                if (current.isEmpty()) {
                    waypointsByPlayer.remove(playerId);
                } else {
                    waypointsByPlayer.put(playerId, List.copyOf(current));
                }
                return Optional.of(removed);
            }
        }
        return Optional.empty();
    }

    public Optional<PlayerWaypoint> removeWaypointById(UUID playerId, UUID waypointId) {
        List<PlayerWaypoint> current = new ArrayList<>(getWaypoints(playerId));
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).waypointId().equals(waypointId)) {
                PlayerWaypoint removed = current.remove(i);
                if (current.isEmpty()) {
                    waypointsByPlayer.remove(playerId);
                } else {
                    waypointsByPlayer.put(playerId, List.copyOf(current));
                }
                return Optional.of(removed);
            }
        }
        return Optional.empty();
    }

    public boolean renameWaypoint(UUID playerId, UUID waypointId, String newName) {
        List<PlayerWaypoint> current = new ArrayList<>(getWaypoints(playerId));
        if (current.stream().anyMatch(waypoint -> waypoint.name().equalsIgnoreCase(newName)
                && !waypoint.waypointId().equals(waypointId))) {
            return false;
        }

        for (int i = 0; i < current.size(); i++) {
            PlayerWaypoint existing = current.get(i);
            if (existing.waypointId().equals(waypointId)) {
                current.set(i, new PlayerWaypoint(
                        existing.waypointId(),
                        newName,
                        existing.dimension(),
                        existing.x(),
                        existing.y(),
                        existing.z(),
                        existing.yaw(),
                        existing.pitch()
                ));
                waypointsByPlayer.put(playerId, List.copyOf(current));
                return true;
            }
        }
        return false;
    }

    public boolean moveWaypoint(UUID playerId, UUID waypointId, int direction) {
        if (direction == 0) {
            return false;
        }

        List<PlayerWaypoint> current = new ArrayList<>(getWaypoints(playerId));
        int index = -1;
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).waypointId().equals(waypointId)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return false;
        }

        int targetIndex = index + direction;
        if (targetIndex < 0 || targetIndex >= current.size()) {
            return false;
        }

        PlayerWaypoint moving = current.remove(index);
        current.add(targetIndex, moving);
        waypointsByPlayer.put(playerId, List.copyOf(current));
        return true;
    }

    public Optional<PlayerWaypoint> findByDestinationId(UUID playerId, String destinationId) {
        if (!destinationId.startsWith(TeleportService.WAYPOINT_DESTINATION_PREFIX)) {
            return Optional.empty();
        }
        String suffix = destinationId.substring(TeleportService.WAYPOINT_DESTINATION_PREFIX.length());
        try {
            UUID waypointId = UUID.fromString(suffix);
            return getWaypoints(playerId).stream()
                    .filter(waypoint -> waypoint.waypointId().equals(waypointId))
                    .findFirst();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Map<UUID, List<PlayerWaypoint>> view() {
        return Collections.unmodifiableMap(waypointsByPlayer);
    }

    private static class PlayerWaypointData {
        String waypointId;
        String name;
        String dimension;
        double x;
        double y;
        double z;
        float yaw;
        float pitch;

        static PlayerWaypointData fromWaypoint(PlayerWaypoint wp) {
            PlayerWaypointData data = new PlayerWaypointData();
            data.waypointId = wp.waypointId().toString();
            data.name = wp.name();
            data.dimension = wp.dimension().identifier().toString();
            data.x = wp.x();
            data.y = wp.y();
            data.z = wp.z();
            data.yaw = wp.yaw();
            data.pitch = wp.pitch();
            return data;
        }

        PlayerWaypoint toWaypoint() {
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim =
                    net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION,
                            net.minecraft.resources.Identifier.parse(this.dimension)
                    );
            return new PlayerWaypoint(
                    UUID.fromString(this.waypointId),
                    this.name,
                    dim,
                    this.x,
                    this.y,
                    this.z,
                    this.yaw,
                    this.pitch
            );
        }
    }
}
