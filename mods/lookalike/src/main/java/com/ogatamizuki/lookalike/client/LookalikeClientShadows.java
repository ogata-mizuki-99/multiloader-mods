package com.ogatamizuki.lookalike.client;

import com.ogatamizuki.lookalike.NetworkPayloads;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class LookalikeClientShadows {
    private static final Set<UUID> shadowPlayers = ConcurrentHashMap.newKeySet();
    private static List<NetworkPayloads.ShadowPathEntry> paths = List.of();
    private static boolean pathVisualizationEnabled;

    private LookalikeClientShadows() {
    }

    static void apply(NetworkPayloads.ShadowAppearanceSyncPayload payload) {
        shadowPlayers.clear();
        if (payload.shadowPlayerUuids() != null) {
            for (String uuidStr : payload.shadowPlayerUuids()) {
                shadowPlayers.add(UUID.fromString(uuidStr));
            }
        }
        paths = payload.paths() != null ? List.copyOf(payload.paths()) : List.of();
        pathVisualizationEnabled = payload.pathVisualizationEnabled();
    }

    static Set<UUID> shadowPlayers() {
        return Collections.unmodifiableSet(shadowPlayers);
    }

    static boolean isShadow(UUID uuid) {
        return shadowPlayers.contains(uuid);
    }

    static List<NetworkPayloads.ShadowPathEntry> paths() {
        return paths;
    }

    static boolean isPathVisualizationEnabled() {
        return pathVisualizationEnabled;
    }
}
