package com.ogatamizuki.radialteleport.client;

import com.ogatamizuki.radialteleport.TeleportDestination;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class RadialTeleportFaceRenderer {
    private static final Map<UUID, PlayerSkin> cachedSkins = new HashMap<>();

    private RadialTeleportFaceRenderer() {
    }

    static void onDestinationsChanged(Minecraft mc, List<TeleportDestination> destinations) {
        cachedSkins.clear();
        if (mc.getConnection() == null) {
            return;
        }

        for (TeleportDestination destination : destinations) {
            if (destination.kind() != TeleportDestination.KIND_PLAYER) {
                continue;
            }
            try {
                UUID playerUuid = UUID.fromString(destination.id());
                PlayerInfo info = mc.getConnection().getPlayerInfo(playerUuid);
                if (info == null) {
                    info = mc.getConnection().getSeenPlayers().get(playerUuid);
                }
                if (info != null) {
                    cachedSkins.put(playerUuid, info.getSkin());
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    static void clearCache() {
        cachedSkins.clear();
    }

    static void drawPlayerFace(GuiGraphicsExtractor graphics, UUID playerUuid, int x, int y, int size) {
        PlayerSkin skin = cachedSkins.get(playerUuid);
        if (skin == null) {
            drawPlaceholder(graphics, x, y, size);
            return;
        }

        graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF101820);
        net.minecraft.client.gui.components.PlayerFaceExtractor.extractRenderState(graphics, skin, x, y, size);
    }

    private static void drawPlaceholder(GuiGraphicsExtractor graphics, int x, int y, int size) {
        graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF101820);
        graphics.fill(x, y, x + size, y + size, 0xFF546E7A);
    }
}
