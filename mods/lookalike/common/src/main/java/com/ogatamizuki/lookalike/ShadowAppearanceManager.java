package com.ogatamizuki.lookalike;

import com.ogatamizuki.lookalike.cast.CastManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShadowAppearanceManager {
    private static final ShadowAppearanceManager INSTANCE = new ShadowAppearanceManager();

    private final Set<UUID> shadowPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> remainingTicks = new ConcurrentHashMap<>();
    private volatile List<NetworkPayloads.ShadowPathEntry> activePaths = List.of();
    private volatile boolean pathVisualizationEnabled;

    private ShadowAppearanceManager() {
    }

    public static ShadowAppearanceManager getInstance() {
        return INSTANCE;
    }

    public synchronized void enableShadow(ServerPlayer player) {
        enableShadow(player, 0);
    }

    public synchronized void enableShadow(ServerPlayer player, int durationSeconds) {
        if (player == null) {
            return;
        }

        UUID playerUuid = player.getUUID();
        CastManager.getInstance().cancelCastIfActive(player);

        ModelCustomizationHelper.set(player, ModelCustomizationHelper.ALL_PARTS);
        shadowPlayers.add(playerUuid);
        player.setCustomName(Component.translatable("lookalike.shadow.name"));
        // ネームタグで「謎の影」と出ると位置は分かるが、werewolf 側で夜はネームタグ非表示にする。
        // ここでも visible=false にして他MODとの二重表示を避ける。
        player.setCustomNameVisible(false);
        LookalikeCommon.refreshPlayerNames.accept(player);

        if (durationSeconds > 0) {
            remainingTicks.put(playerUuid, durationSeconds * 20);
        } else {
            remainingTicks.remove(playerUuid);
        }

        broadcastAppearanceSync(player.level().getServer());
    }

    public synchronized boolean disableShadow(ServerPlayer player) {
        if (player == null) {
            return false;
        }

        UUID playerUuid = player.getUUID();
        if (!shadowPlayers.remove(playerUuid)) {
            return false;
        }

        remainingTicks.remove(playerUuid);
        player.setCustomName(null);
        player.setCustomNameVisible(false);
        LookalikeCommon.refreshPlayerNames.accept(player);

        MinecraftServer server = player.level().getServer();
        if (server != null) {
            broadcastAppearanceSync(server);
        }
        return true;
    }

    public synchronized void disableAllShadows(MinecraftServer server) {
        if (server == null) {
            shadowPlayers.clear();
            remainingTicks.clear();
            activePaths = List.of();
            pathVisualizationEnabled = false;
            return;
        }

        List<UUID> snapshot = new ArrayList<>(shadowPlayers);
        for (UUID uuid : snapshot) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                disableShadow(player);
            } else {
                shadowPlayers.remove(uuid);
                remainingTicks.remove(uuid);
            }
        }
        activePaths = List.of();
        pathVisualizationEnabled = false;
        broadcastAppearanceSync(server);
    }

    public synchronized void setPathVisualization(
            MinecraftServer server,
            List<NetworkPayloads.ShadowPathEntry> paths,
            boolean enabled
    ) {
        activePaths = paths != null ? List.copyOf(paths) : List.of();
        pathVisualizationEnabled = enabled;
        broadcastAppearanceSync(server);
    }

    public synchronized void syncAppearanceTo(ServerPlayer player) {
        if (player == null) {
            return;
        }
        LookalikeCommon.sendToPlayer.accept(
                player,
                buildSyncPayload()
        );
    }

    public boolean isShadow(UUID uuid) {
        return shadowPlayers.contains(uuid);
    }

    public synchronized void prepareForIdentityDisguise(ServerPlayer player) {
        if (player != null && isShadow(player.getUUID())) {
            disableShadow(player);
        }
    }

    public synchronized void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Integer>> iterator = remainingTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int ticks = entry.getValue() - 1;
            if (ticks <= 0) {
                iterator.remove();
                if (server != null) {
                    ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                    if (player != null) {
                        disableShadow(player);
                    } else {
                        shadowPlayers.remove(entry.getKey());
                    }
                }
            } else {
                entry.setValue(ticks);
            }
        }
    }

    public synchronized void tick() {
    }

    private void broadcastAppearanceSync(MinecraftServer server) {
        if (server == null) {
            return;
        }
        NetworkPayloads.ShadowAppearanceSyncPayload payload = buildSyncPayload();
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            LookalikeCommon.sendToPlayer.accept(online, payload);
        }
    }

    private NetworkPayloads.ShadowAppearanceSyncPayload buildSyncPayload() {
        List<String> players = shadowPlayers.stream().map(UUID::toString).toList();
        return new NetworkPayloads.ShadowAppearanceSyncPayload(
                players,
                activePaths,
                pathVisualizationEnabled
        );
    }
}
