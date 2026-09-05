package com.ogatamizuki.lookalike.client;

import com.ogatamizuki.lookalike.NetworkPayloads;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LookalikeClientShadows {
    private static final Set<UUID> shadowPlayers = ConcurrentHashMap.newKeySet();
    private static List<NetworkPayloads.ShadowPathEntry> paths = List.of();
    private static boolean pathVisualizationEnabled;

    private LookalikeClientShadows() {
    }

    public static void apply(NetworkPayloads.ShadowAppearanceSyncPayload payload) {
        shadowPlayers.clear();
        if (payload.shadowPlayerUuids() != null) {
            for (String uuidStr : payload.shadowPlayerUuids()) {
                shadowPlayers.add(UUID.fromString(uuidStr));
            }
        }
        paths = payload.paths() != null ? List.copyOf(payload.paths()) : List.of();
        pathVisualizationEnabled = payload.pathVisualizationEnabled();
    }

    public static Set<UUID> shadowPlayers() {
        return Collections.unmodifiableSet(shadowPlayers);
    }

    public static boolean isShadow(UUID uuid) {
        return shadowPlayers.contains(uuid);
    }

    public static List<NetworkPayloads.ShadowPathEntry> paths() {
        return paths;
    }

    public static boolean isPathVisualizationEnabled() {
        return pathVisualizationEnabled;
    }
}
