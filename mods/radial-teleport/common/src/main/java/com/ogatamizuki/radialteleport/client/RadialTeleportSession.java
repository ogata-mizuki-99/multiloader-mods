package com.ogatamizuki.radialteleport.client;

import com.ogatamizuki.radialteleport.RadialTeleportCommon;
import com.ogatamizuki.radialteleport.RequestDestinationsPayload;
import com.ogatamizuki.radialteleport.TeleportDestination;
import com.ogatamizuki.radialteleport.TeleportDestinationsPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RadialTeleportSession {
    private static final int REFRESH_INTERVAL_TICKS = 10;

    private static boolean active;
    private static List<TeleportDestination> destinations = List.of();
    private static int hoveredIndex = -1;
    private static int clientTick;
    private static int lastRefreshTick = Integer.MIN_VALUE;

    private RadialTeleportSession() {
    }

    public static void begin(Minecraft mc) {
        active = true;
        hoveredIndex = -1;
        clientTick = 0;
        lastRefreshTick = Integer.MIN_VALUE;
        RadialTeleportMouseCapture.captureForRadialMenu(mc);
        refreshDestinationsLocal(mc);
        requestDestinationsFromServer(mc);
    }

    public static void requestDestinationsFromServer(Minecraft mc) {
        if (mc.getConnection() != null) {
            RadialTeleportCommon.sendToServer.accept(RequestDestinationsPayload.INSTANCE);
        } else {
            refreshDestinationsLocal(mc);
        }
        lastRefreshTick = clientTick;
    }

    public static void clear() {
        active = false;
        destinations = List.of();
        hoveredIndex = -1;
        RadialTeleportLabelCache.clear();
        RadialTeleportFaceRenderer.clearCache();
    }

    public static void end(Minecraft mc) {
        if (active) {
            RadialTeleportMouseCapture.restore(mc);
        }
        RadialMenuTextureCache.release(mc);
        clear();
    }

    public static boolean isActive() {
        return active;
    }

    public static List<TeleportDestination> getDestinations() {
        return destinations;
    }

    public static int getHoveredIndex() {
        return hoveredIndex;
    }

    public static void updateDestinations(TeleportDestinationsPayload payload) {
        if (!active) {
            return;
        }
        applyDestinations(Minecraft.getInstance(), enrichDisplayNames(Minecraft.getInstance(), payload.destinations()));
    }

    public static void refreshDestinationsLocal(Minecraft mc) {
        if (!active) {
            return;
        }
        applyDestinations(mc, RadialTeleportDestinationProvider.buildLocal(mc));
        lastRefreshTick = clientTick;
    }

    public static void tick(LocalPlayer player) {
        clientTick++;
        updateHoveredIndex();
    }

    public static boolean shouldRefreshLocal(Minecraft mc) {
        return active && clientTick - lastRefreshTick >= REFRESH_INTERVAL_TICKS;
    }

    public static void refreshDisplayNames(Minecraft mc) {
        if (!active || destinations.isEmpty()) {
            return;
        }
        applyDestinations(mc, enrichDisplayNames(mc, destinations));
        lastRefreshTick = clientTick;
    }

    private static List<TeleportDestination> enrichDisplayNames(Minecraft mc, List<TeleportDestination> destinations) {
        if (mc.player == null || mc.getConnection() == null) {
            return Collections.unmodifiableList(destinations);
        }

        List<TeleportDestination> enriched = new ArrayList<>(destinations.size());
        for (TeleportDestination destination : destinations) {
            if (destination.kind() != TeleportDestination.KIND_PLAYER) {
                enriched.add(destination);
                continue;
            }
            try {
                var uuid = java.util.UUID.fromString(destination.id());
                var info = mc.getConnection().getPlayerInfo(uuid);
                if (info != null) {
                    enriched.add(new TeleportDestination(
                            destination.id(),
                            RadialTeleportDisplayNames.resolvePlayerName(mc, info),
                            destination.kind()
                    ));
                    continue;
                }
            } catch (IllegalArgumentException ignored) {
            }
            enriched.add(destination);
        }
        return Collections.unmodifiableList(enriched);
    }

    private static void applyDestinations(Minecraft mc, List<TeleportDestination> nextDestinations) {
        destinations = nextDestinations;
        RadialMenuTextureCache.markDestinationsDirty();
        RadialTeleportLabelCache.rebuild(mc, nextDestinations);
        RadialTeleportFaceRenderer.onDestinationsChanged(mc, nextDestinations);
        updateHoveredIndex();
    }

    private static void updateHoveredIndex() {
        int nextHoveredIndex = RadialTeleportOverlay.resolveHoveredSlice(
                destinations.size(),
                RadialTeleportOverlay.getMouseAngleDegrees()
        );
        if (nextHoveredIndex != hoveredIndex) {
            hoveredIndex = nextHoveredIndex;
            RadialMenuTextureCache.markHoverDirty();
        }
    }
}
