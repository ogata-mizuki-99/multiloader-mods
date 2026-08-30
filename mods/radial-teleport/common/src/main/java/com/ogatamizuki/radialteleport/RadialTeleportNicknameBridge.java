package com.ogatamizuki.radialteleport;

import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.UUID;

public final class RadialTeleportNicknameBridge {
    private static final String NICKNAME_MOD_ID = "nickname";
    private static final String NICKNAME_STORAGE_CLASS = "com.ogatamizuki.nickname.NicknameStorage";

    private static Method cachedGetNickname;
    private static boolean nicknameLookupInitialized;

    private RadialTeleportNicknameBridge() {}

    public static boolean isNicknameModLoaded() {
        return RadialTeleportCommon.isModLoaded(NICKNAME_MOD_ID);
    }

    public static String resolvePlayerName(Player player) {
        if (player == null) {
            return "Unknown";
        }

        String nickname = lookupNickname(player.getUUID());
        if (nickname != null) {
            return nickname;
        }

        return player.getGameProfile().name();
    }

    public static String lookupNickname(UUID playerId) {
        if (!isNicknameModLoaded()) {
            return null;
        }

        Method getNickname = nicknameLookupMethod();
        if (getNickname == null) {
            return null;
        }

        try {
            Object value = getNickname.invoke(null, playerId);
            if (value instanceof String nickname && !nickname.isEmpty()) {
                return nickname;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }

    private static Method nicknameLookupMethod() {
        if (nicknameLookupInitialized) {
            return cachedGetNickname;
        }

        nicknameLookupInitialized = true;
        try {
            Class<?> storageClass = Class.forName(NICKNAME_STORAGE_CLASS);
            cachedGetNickname = storageClass.getMethod("getNickname", UUID.class);
        } catch (ReflectiveOperationException ignored) {
            cachedGetNickname = null;
        }
        return cachedGetNickname;
    }
}
