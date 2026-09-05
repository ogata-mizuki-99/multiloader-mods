package com.ogatamizuki.radialteleport.client;

import com.ogatamizuki.radialteleport.RadialTeleportNicknameBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

final class RadialTeleportDisplayNames {
    private RadialTeleportDisplayNames() {
    }

    static String resolvePlayerName(Minecraft mc, PlayerInfo info) {
        UUID playerId = info.getProfile().id();
        String profileName = info.getProfile().name();

        Component tabName = info.getTabListDisplayName();
        if (tabName != null) {
            String text = tabName.getString();
            if (!text.isEmpty() && !text.equals(profileName)) {
                return text;
            }
        }

        if (mc.level != null) {
            Player player = mc.level.getPlayerByUUID(playerId);
            if (player != null) {
                String text = player.getDisplayName().getString();
                if (!text.isEmpty() && !text.equals(profileName)) {
                    return text;
                }
            }
        }

        String nickname = RadialTeleportNicknameBridge.lookupNickname(playerId);
        if (nickname != null) {
            return nickname;
        }

        if (tabName != null) {
            String text = tabName.getString();
            if (!text.isEmpty()) {
                return text;
            }
        }

        return profileName;
    }
}
