package com.ogatamizuki.radialteleport.client;

import com.ogatamizuki.radialteleport.TeleportDestination;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

final class RadialTeleportLabelCache {
    record LabelEntry(
            TeleportDestination destination,
            UUID playerUuid,
            String displayName,
            String truncatedName,
            float midDegrees,
            float sliceAngle,
            int faceSize
    ) {
    }

    private static List<LabelEntry> entries = List.of();

    private RadialTeleportLabelCache() {
    }

    static List<LabelEntry> entries() {
        return entries;
    }

    static void rebuild(Minecraft mc, List<TeleportDestination> destinations) {
        if (destinations.isEmpty()) {
            entries = List.of();
            return;
        }

        Font font = mc.font;
        float sliceAngle = 360.0F / destinations.size();
        List<LabelEntry> rebuilt = new ArrayList<>(destinations.size());

        for (int i = 0; i < destinations.size(); i++) {
            TeleportDestination destination = destinations.get(i);
            float start = -90.0F + sliceAngle * i;
            float end = start + sliceAngle;
            float midDegrees = (start + end) * 0.5F;

            UUID playerUuid = null;
            String displayName = resolveDisplayName(destination);
            String truncatedName = null;
            int faceSize = sliceAngle >= 18.0F ? RadialMenuLayout.FACE_SIZE : 11;
            int maxTextWidth = Math.max(
                    24,
                    (int) (sliceAngle / 360.0F * (2.0F * (float) Math.PI * RadialMenuLayout.LABEL_RADIUS) * 0.55F)
            );

            if (destination.kind() == TeleportDestination.KIND_WAYPOINT) {
                truncatedName = truncate(font, displayName, maxTextWidth);
            } else if (destination.kind() == TeleportDestination.KIND_PLAYER && sliceAngle >= 14.0F) {
                try {
                    playerUuid = UUID.fromString(destination.id());
                } catch (IllegalArgumentException ignored) {
                }
                truncatedName = truncate(font, displayName, maxTextWidth);
            } else if (destination.kind() == TeleportDestination.KIND_SPAWN) {
                truncatedName = truncate(font, displayName, maxTextWidth);
            }

            rebuilt.add(new LabelEntry(destination, playerUuid, displayName, truncatedName, midDegrees, sliceAngle, faceSize));
        }

        entries = Collections.unmodifiableList(rebuilt);
    }

    static void clear() {
        entries = List.of();
    }

    private static String resolveDisplayName(TeleportDestination destination) {
        if (destination.kind() == TeleportDestination.KIND_SPAWN) {
            return Component.translatable("radial_teleport.slice.spawn").getString();
        }
        return destination.displayName();
    }

    private static String truncate(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int target = maxWidth - font.width(ellipsis);
        if (target <= 0) {
            return ellipsis;
        }

        for (int length = text.length() - 1; length > 0; length--) {
            String candidate = text.substring(0, length);
            if (font.width(candidate) <= target) {
                return candidate + ellipsis;
            }
        }

        return ellipsis;
    }
}
