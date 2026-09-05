package com.ogatamizuki.privatechest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class OwnerPlayerProfileUtil {
    private static final UUID DEFAULT_PROFILE_ID = new UUID(0, 0);

    private OwnerPlayerProfileUtil() {
    }

    public static void applyOwnerProfile(ItemStack stack, Player player) {
        stack.set(DataComponents.PROFILE, player.getProfile());
    }

    public static void applyOwnerProfile(ItemStack stack, ResolvableProfile profile) {
        if (profile != null) {
            stack.set(DataComponents.PROFILE, profile);
        }
    }

    public static void applyOwnerProfile(ItemStack stack, UUID uuid, String name) {
        if (uuid != null && !uuid.equals(DEFAULT_PROFILE_ID)) {
            stack.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(uuid));
        } else if (name != null && !name.isEmpty()) {
            stack.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(name));
        }
    }

    public static void ensureProfile(ItemStack stack, @Nullable Player player) {
        if (player != null) {
            if (needsProfileUpdate(stack, player)) {
                applyOwnerProfile(stack, player);
            }
        } else if (!stack.has(DataComponents.PROFILE)) {
            stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(
                    new GameProfile(DEFAULT_PROFILE_ID, "Steve")));
        }
    }

    private static boolean needsProfileUpdate(ItemStack stack, Player player) {
        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null) {
            return true;
        }
        UUID playerId = player.getUUID();
        UUID profileId = profile.partialProfile().id();
        String profileName = profile.partialProfile().name();

        if (DEFAULT_PROFILE_ID.equals(profileId) || (profileId == null && "Steve".equals(profileName))) {
            return true;
        }

        if (playerId.equals(profileId)) {
            return !hasTextureProperty(profile.partialProfile());
        }

        return false;
    }

    private static boolean hasTextureProperty(GameProfile profile) {
        return profile.properties().containsKey("textures");
    }
}
