package com.ogatamizuki.lookalike.client;

import com.google.common.collect.LinkedHashMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.ogatamizuki.lookalike.NetworkPayloads.DisguiseSkinEntry;
import com.ogatamizuki.lookalike.ShadowSkinTextures;
import com.ogatamizuki.lookalike.SkinTextureHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class LookalikeClientSkins {
    private static final Map<UUID, PlayerSkin> disguiseSkins = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerSkin> profileSkins = new ConcurrentHashMap<>();
    private static final Set<UUID> pendingProfileLoads = ConcurrentHashMap.newKeySet();
    private static volatile PlayerSkin cachedShadowSkin;

    private LookalikeClientSkins() {
    }

    static PlayerSkin getShadowSkin() {
        PlayerSkin skin = cachedShadowSkin;
        if (skin == null) {
            skin = resolveSkinFromTextureValue(
                    UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    ShadowSkinTextures.textureValue(),
                    PlayerSkin.Patch.EMPTY);
            cachedShadowSkin = skin;
        }
        return skin;
    }

    static void applyDisguiseListSync(List<DisguiseSkinEntry> entries) {
        disguiseSkins.clear();
        for (DisguiseSkinEntry entry : entries) {
            if (entry.textureValue().isEmpty()) {
                continue;
            }
            UUID uuid = UUID.fromString(entry.uuidStr());
            disguiseSkins.put(uuid, resolveDisguiseSkin(entry));
        }
    }

    private static PlayerSkin resolveDisguiseSkin(DisguiseSkinEntry entry) {
        String targetUuidStr = entry.targetUuidStr();
        if (targetUuidStr != null && !targetUuidStr.isEmpty()) {
            try {
                PlayerSkin targetSkin = resolveOnlineSkin(UUID.fromString(targetUuidStr));
                if (targetSkin != null) {
                    return targetSkin;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        UUID wearerUuid = UUID.fromString(entry.uuidStr());
        PlayerSkin.Patch patch = entry.skinPatch();
        return resolveSkinFromTextureValue(wearerUuid, entry.textureValue(), patch);
    }

    static PlayerSkin getDisguiseSkin(UUID uuid) {
        return disguiseSkins.get(uuid);
    }

    static PlayerSkin resolveIcon(UUID uuid, String name, boolean resetEntry, UUID selfUuid) {
        if (resetEntry && uuid.equals(selfUuid) && LookalikeModClient.isDisguised(selfUuid)) {
            return resolveProfileSkin(uuid, name);
        }
        if (!LookalikeModClient.isDisguised(uuid)) {
            PlayerSkin onlineSkin = resolveOnlineSkin(uuid);
            if (onlineSkin != null) {
                return onlineSkin;
            }
        }
        return resolveProfileSkin(uuid, name);
    }

    private static PlayerSkin resolveOnlineSkin(UUID uuid) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) {
            return null;
        }
        PlayerInfo info = mc.getConnection().getPlayerInfo(uuid);
        return info != null ? info.getSkin() : null;
    }

    private static PlayerSkin resolveProfileSkin(UUID uuid, String name) {
        PlayerSkin cached = profileSkins.get(uuid);
        if (cached != null) {
            return cached;
        }
        requestProfileSkin(uuid, name);
        return DefaultPlayerSkin.get(new GameProfile(uuid, name));
    }

    private static void requestProfileSkin(UUID uuid, String name) {
        if (pendingProfileLoads.contains(uuid)) {
            return;
        }
        pendingProfileLoads.add(uuid);
        Minecraft mc = Minecraft.getInstance();
        GameProfile profile = new GameProfile(uuid, name);
        mc.getSkinManager().get(profile).thenAccept(optionalSkin -> mc.execute(() -> {
            pendingProfileLoads.remove(uuid);
            optionalSkin.ifPresent(skin -> profileSkins.put(uuid, skin));
        }));
    }

    private static PlayerSkin resolveSkinFromTextureValue(UUID uuid, String textureValue, PlayerSkin.Patch patch) {
        String rewritten = SkinTextureHelper.rewriteTextureValue(textureValue, uuid, "Unknown");
        LinkedHashMultimap<String, Property> properties = LinkedHashMultimap.create();
        properties.put("textures", new Property("textures", rewritten));
        GameProfile profile = new GameProfile(uuid, "Unknown", new PropertyMap(properties));
        Minecraft mc = Minecraft.getInstance();
        java.util.concurrent.CompletableFuture<java.util.Optional<PlayerSkin>> future = mc.getSkinManager()
                .get(profile);
        java.util.Optional<PlayerSkin> immediate = future.getNow(java.util.Optional.empty());
        if (immediate.isPresent()) {
            PlayerSkin skin = immediate.get();
            return patch != null && !patch.equals(PlayerSkin.Patch.EMPTY) ? skin.with(patch) : skin;
        }

        future.thenAccept(opt -> mc.execute(() -> {
            opt.ifPresent(skin -> {
                PlayerSkin patchedSkin = patch != null && !patch.equals(PlayerSkin.Patch.EMPTY) ? skin.with(patch)
                        : skin;
                disguiseSkins.put(uuid, patchedSkin);
            });
        }));

        PlayerSkin defaultSkin = DefaultPlayerSkin.get(profile);
        return patch != null && !patch.equals(PlayerSkin.Patch.EMPTY) ? defaultSkin.with(patch) : defaultSkin;
    }
}
