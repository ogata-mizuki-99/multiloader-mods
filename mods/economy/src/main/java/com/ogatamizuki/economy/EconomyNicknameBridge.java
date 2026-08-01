package com.ogatamizuki.economy;

import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

/**
 * nickname MOD 未導入時にクラスをロードしないための反射ブリッジ。
 * ランキング表示名・API 同期の username にニックネームを反映する。
 */
public final class EconomyNicknameBridge {
    private static final String NICKNAME_MOD_ID = "nickname";
    private static final String NICKNAME_STORAGE_CLASS = "com.ogatamizuki.nickname.NicknameStorage";

    private static Method cachedGetNickname;
    private static boolean nicknameLookupInitialized;

    private EconomyNicknameBridge() {}

    public static boolean isNicknameModLoaded() {
        return ModList.get().isLoaded(NICKNAME_MOD_ID);
    }

    /**
     * プレイヤーの表示用名前。nickname 設定時はニックネーム、未設定時はプロファイル名。
     */
    public static String resolvePlayerName(Player player) {
        if (player == null) {
            return "Unknown";
        }

        String profileName = player.getName().getString();
        String displayName = player.getDisplayName().getString();
        if (!displayName.isEmpty() && !displayName.equals(profileName)) {
            return displayName;
        }

        String nickname = lookupNickname(player.getUUID());
        if (nickname != null) {
            return nickname;
        }

        return profileName;
    }

    /**
     * ランキング集計用。オンライン名 → nickname ストレージ → Unknown。
     */
    public static String resolveUsernameForRanking(String uuidStr, Map<String, String> onlinePlayerNames) {
        String onlineName = onlinePlayerNames.get(uuidStr);
        if (onlineName != null) {
            return onlineName;
        }

        try {
            String nickname = lookupNickname(UUID.fromString(uuidStr));
            if (nickname != null) {
                return nickname;
            }
        } catch (IllegalArgumentException ignored) {
        }

        return "Unknown";
    }

    private static String lookupNickname(UUID playerId) {
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
