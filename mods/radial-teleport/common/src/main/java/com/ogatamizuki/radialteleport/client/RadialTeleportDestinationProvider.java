package com.ogatamizuki.radialteleport.client;

import com.ogatamizuki.radialteleport.TeleportDestination;
import com.ogatamizuki.radialteleport.TeleportService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class RadialTeleportDestinationProvider {
    private RadialTeleportDestinationProvider() {
    }

    static List<TeleportDestination> buildLocal(Minecraft mc) {
        List<TeleportDestination> destinations = new ArrayList<>();
        destinations.add(new TeleportDestination(
                TeleportService.SPAWN_DESTINATION_ID,
                Component.translatable("radial_teleport.slice.spawn").getString(),
                TeleportDestination.KIND_SPAWN
        ));

        if (mc.player == null || mc.getConnection() == null) {
            return Collections.unmodifiableList(destinations);
        }

        Set<String> seenIds = new HashSet<>();
        seenIds.add(TeleportService.SPAWN_DESTINATION_ID);
        addPlayersFrom(mc, destinations, seenIds, mc.getConnection().getListedOnlinePlayers());
        addPlayersFrom(mc, destinations, seenIds, mc.getConnection().getSeenPlayers().values());

        return Collections.unmodifiableList(destinations);
    }

    private static void addPlayersFrom(
            Minecraft mc,
            List<TeleportDestination> destinations,
            Set<String> seenIds,
            Iterable<PlayerInfo> players
    ) {
        for (PlayerInfo info : players) {
            if (info.getProfile().id().equals(mc.player.getUUID())) {
                continue;
            }
            String id = info.getProfile().id().toString();
            if (!seenIds.add(id)) {
                continue;
            }
            destinations.add(new TeleportDestination(
                    id,
                    RadialTeleportDisplayNames.resolvePlayerName(mc, info),
                    TeleportDestination.KIND_PLAYER
            ));
        }
    }
}
