package com.ogatamizuki.lookalike;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.ogatamizuki.lookalike.mixin.PlayerAccessor;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DisguiseManager {
    private static final DisguiseManager INSTANCE = new DisguiseManager();

    private final Map<UUID, GameProfile> activeProfiles = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerSkin.Patch> activeSkinPatches = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> activeTargetUuids = new ConcurrentHashMap<>();
    private final Map<UUID, Byte> originalModelCustomizations = new ConcurrentHashMap<>();
    private final Map<UUID, PropertyMap> originalTextureProperties = new ConcurrentHashMap<>();
    private final Map<UUID, PropertyMap> authenticTextureCache = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> remainingTicks = new ConcurrentHashMap<>();

    private DisguiseManager() {}

    public static DisguiseManager getInstance() {
        return INSTANCE;
    }

    public static GameProfile getStoredGameProfile(ServerPlayer player) {
        return ((PlayerAccessor) player).lookalike$getStoredGameProfile();
    }

    public void cacheAuthenticTextures(ServerPlayer player) {
        PropertyMap textures = copyTextureProperties(getStoredGameProfile(player));
        if (!textures.isEmpty()) {
            authenticTextureCache.put(player.getUUID(), textures);
        }
    }

    public PropertyMap getAuthenticTextureProperties(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (isDisguised(uuid)) {
            PropertyMap original = originalTextureProperties.get(uuid);
            if (original != null && !original.isEmpty()) {
                return original;
            }
        }

        PropertyMap cached = authenticTextureCache.get(uuid);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        PropertyMap fromProfile = copyTextureProperties(getStoredGameProfile(player));
        if (!fromProfile.isEmpty()) {
            authenticTextureCache.put(uuid, fromProfile);
        }
        return fromProfile;
    }

    public synchronized void disguise(ServerPlayer player, ServerPlayer target, int durationSeconds) {
        disguise(player, target, durationSeconds, true);
    }

    public synchronized void disguise(ServerPlayer player, ServerPlayer target) {
        disguise(player, target, 0, true);
    }

    public synchronized void disguise(ServerPlayer player, GameProfile targetProfile, int durationSeconds) {
        disguise(player, targetProfile);
        if (durationSeconds > 0) {
            remainingTicks.put(player.getUUID(), durationSeconds * 20);
        }
    }

    public synchronized void disguise(ServerPlayer player, ServerPlayer target, int durationSeconds, boolean useAuthenticSkin) {
        PropertyMap textures = useAuthenticSkin
                ? getAuthenticTextureProperties(target)
                : copyTextureProperties(getStoredGameProfile(target));
        disguise(player, textures, target.getProfile().skinPatch(), target.getUUID(), durationSeconds);
    }

    public synchronized void disguise(ServerPlayer player, GameProfile targetProfile) {
        disguise(player, copyTexturePropertiesUnsigned(targetProfile), PlayerSkin.Patch.EMPTY, targetProfile.id(), 0);
    }

    public synchronized void disguise(ServerPlayer player, PropertyMap textureProperties, int durationSeconds) {
        disguise(player, textureProperties, PlayerSkin.Patch.EMPTY, null, durationSeconds);
    }

    public synchronized void disguise(
            ServerPlayer player,
            PropertyMap textureProperties,
            PlayerSkin.Patch skinPatch,
            UUID targetUuid,
            int durationSeconds
    ) {
        if (textureProperties.isEmpty()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "commands.lookalike.disguise.no_skin_data"));
            return;
        }

        ShadowAppearanceManager.getInstance().prepareForIdentityDisguise(player);

        UUID playerUuid = player.getUUID();
        String wearerName = getStoredGameProfile(player).name();
        captureOriginalTexturesIfNeeded(player);

        GameProfile disguiseProfile = new GameProfile(
                playerUuid,
                wearerName,
                copyTexturePropertiesUnsignedForWearer(textureProperties, playerUuid, wearerName)
        );

        activeProfiles.put(playerUuid, disguiseProfile);
        activeSkinPatches.put(playerUuid, skinPatch != null ? skinPatch : PlayerSkin.Patch.EMPTY);
        if (targetUuid != null) {
            activeTargetUuids.put(playerUuid, targetUuid);
        } else {
            activeTargetUuids.remove(playerUuid);
        }

        captureOriginalModelCustomizationIfNeeded(player);
        applyDisguiseModelCustomization(player, targetUuid);

        player.refreshDisplayName();
        player.refreshTabListName();
        syncPlayerToAll(player);

        MinecraftServer server = player.level().getServer();
        if (server != null) {
            broadcastDisguiseList(server);
        }

        if (durationSeconds > 0) {
            remainingTicks.put(playerUuid, durationSeconds * 20);
        }
    }

    private void captureOriginalTexturesIfNeeded(ServerPlayer player) {
        UUID playerUuid = player.getUUID();
        if (!originalTextureProperties.containsKey(playerUuid)) {
            PropertyMap textures = getAuthenticTextureProperties(player);
            if (!textures.isEmpty()) {
                originalTextureProperties.put(playerUuid, textures);
            }
        }
    }

    public static PropertyMap copyTexturePropertiesUnsigned(GameProfile source) {
        return copyTexturePropertiesUnsigned(copyTextureProperties(source));
    }

    public static PropertyMap copyTexturePropertiesUnsigned(PropertyMap source) {
        com.google.common.collect.Multimap<String, Property> properties =
                com.google.common.collect.LinkedHashMultimap.create();
        for (Property texture : source.get("textures")) {
            properties.put("textures", new Property(texture.name(), texture.value()));
        }
        return new PropertyMap(properties);
    }

    static PropertyMap copyTexturePropertiesUnsignedForWearer(PropertyMap source, UUID wearerUuid, String wearerName) {
        com.google.common.collect.Multimap<String, Property> properties =
                com.google.common.collect.LinkedHashMultimap.create();
        for (Property texture : source.get("textures")) {
            properties.put("textures", SkinTextureHelper.toUnsignedDisguiseProperty(texture, wearerUuid, wearerName));
        }
        return new PropertyMap(properties);
    }

    public static PropertyMap copyTextureProperties(GameProfile source) {
        com.google.common.collect.Multimap<String, Property> properties =
                com.google.common.collect.LinkedHashMultimap.create();
        for (Property texture : source.properties().get("textures")) {
            if (texture.hasSignature()) {
                properties.put("textures", new Property(texture.name(), texture.value(), texture.signature()));
            } else {
                properties.put("textures", new Property(texture.name(), texture.value()));
            }
        }
        return new PropertyMap(properties);
    }

    public static PropertyMap emptyTextureProperties() {
        return new PropertyMap(com.google.common.collect.LinkedHashMultimap.create());
    }

    private static UUID getUuidByNickname(String name) {
        try {
            Class<?> storageClass = Class.forName("com.ogatamizuki.nickname.NicknameStorage");
            java.lang.reflect.Method getNicknamesMethod = storageClass.getMethod("getNicknames");
            @SuppressWarnings("unchecked")
            Map<UUID, String> nicknames = (Map<UUID, String>) getNicknamesMethod.invoke(null);
            if (nicknames != null) {
                for (Map.Entry<UUID, String> entry : nicknames.entrySet()) {
                    String cleanNickname = net.minecraft.ChatFormatting.stripFormatting(entry.getValue());
                    if (cleanNickname != null && cleanNickname.equalsIgnoreCase(name)) {
                        return entry.getKey();
                    }
                }
            }
        } catch (ClassNotFoundException ignored) {
            // nickname MOD が導入されていない環境では正常
        } catch (Exception e) {
            LookalikeMod.LOGGER.warn("[lookalike] Error while integrating with nickname MOD", e);
        }
        return null;
    }

    public void disguiseAsName(ServerPlayer player, String targetName, int durationSeconds) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        UUID targetUuid = getUuidByNickname(targetName);
        ResolvableProfile resolvable;
        if (targetUuid != null) {
            resolvable = ResolvableProfile.createUnresolved(targetUuid);
        } else {
            resolvable = ResolvableProfile.createUnresolved(targetName);
        }

        resolvable.resolveProfile(server.services().profileResolver()).thenAccept(loadedProfile -> {
            server.execute(() -> {
                if (loadedProfile != null && loadedProfile.properties().containsKey("textures")) {
                    disguise(player, copyTexturePropertiesUnsigned(loadedProfile), PlayerSkin.Patch.EMPTY, loadedProfile.id(), durationSeconds);
                } else {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("commands.lookalike.disguise.skin_fail", targetName));
                }
            });
        }).exceptionally(e -> {
            server.execute(() -> {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("commands.lookalike.disguise.error", targetName, e.getMessage()));
            });
            return null;
        });
    }

    public synchronized boolean undisguise(ServerPlayer player) {
        UUID playerUuid = player.getUUID();
        if (ShadowAppearanceManager.getInstance().isShadow(playerUuid)) {
            return ShadowAppearanceManager.getInstance().disableShadow(player);
        }
        if (!activeProfiles.containsKey(playerUuid)) {
            return false;
        }

        activeProfiles.remove(playerUuid);
        activeSkinPatches.remove(playerUuid);
        activeTargetUuids.remove(playerUuid);
        remainingTicks.remove(playerUuid);
        originalTextureProperties.remove(playerUuid);

        Byte originalCustomization = originalModelCustomizations.remove(playerUuid);
        if (originalCustomization != null) {
            ModelCustomizationHelper.set(player, originalCustomization);
        }

        player.refreshDisplayName();
        player.refreshTabListName();
        syncPlayerToAll(player);

        MinecraftServer server = player.level().getServer();
        if (server != null) {
            broadcastDisguiseList(server);
        }
        return true;
    }

    public boolean isDisguised(UUID uuid) {
        return activeProfiles.containsKey(uuid);
    }

    public GameProfile getDisguisedProfile(UUID uuid) {
        return activeProfiles.get(uuid);
    }

    public Map<UUID, GameProfile> getDisguisedProfileMap() {
        return activeProfiles;
    }

    public void syncDisguiseListTo(ServerPlayer player) {
        List<NetworkPayloads.DisguiseSkinEntry> list = buildDisguiseSkinEntries();
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player, new NetworkPayloads.DisguiseListSyncPayload(list));
    }

    private List<NetworkPayloads.DisguiseSkinEntry> buildDisguiseSkinEntries() {
        List<NetworkPayloads.DisguiseSkinEntry> list = new ArrayList<>();
        for (Map.Entry<UUID, GameProfile> entry : activeProfiles.entrySet()) {
            Property texture = entry.getValue().properties().get("textures").stream().findFirst().orElse(null);
            if (texture == null) {
                continue;
            }
            UUID targetUuid = activeTargetUuids.get(entry.getKey());
            list.add(new NetworkPayloads.DisguiseSkinEntry(
                    entry.getKey().toString(),
                    texture.value(),
                    activeSkinPatches.getOrDefault(entry.getKey(), PlayerSkin.Patch.EMPTY),
                    targetUuid != null ? targetUuid.toString() : ""
            ));
        }
        return list;
    }

    public void broadcastDisguiseList(MinecraftServer server) {
        if (server == null) {
            return;
        }
        NetworkPayloads.DisguiseListSyncPayload payload =
                new NetworkPayloads.DisguiseListSyncPayload(buildDisguiseSkinEntries());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public PropertyMap resolveAuthenticTextures(ServerPlayer player) {
        PropertyMap textures = getAuthenticTextureProperties(player);
        if (!textures.isEmpty()) {
            return textures;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return emptyTextureProperties();
        }

        java.util.Optional<GameProfile> fetched = server.services().profileResolver()
                .fetchByNameOrId(com.mojang.datafixers.util.Either.right(player.getUUID()));
        if (fetched.isPresent()) {
            PropertyMap resolved = copyTextureProperties(fetched.get());
            if (!resolved.isEmpty()) {
                authenticTextureCache.put(player.getUUID(), resolved);
                return resolved;
            }
        }
        return emptyTextureProperties();
    }

    public void resolveAuthenticTexturesAsync(ServerPlayer player, java.util.function.Consumer<PropertyMap> callback) {
        PropertyMap textures = resolveAuthenticTextures(player);
        if (!textures.isEmpty()) {
            callback.accept(textures);
            return;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            callback.accept(emptyTextureProperties());
            return;
        }

        player.getProfile().resolveProfile(server.services().profileResolver()).thenAccept(profile -> {
            server.execute(() -> {
                PropertyMap resolved = copyTextureProperties(profile);
                if (resolved.isEmpty()) {
                    // プロファイル解決後も空の場合は同期取得を再試行する（キャッシュに積まれた可能性あり）。
                    resolved = resolveAuthenticTextures(player);
                } else {
                    authenticTextureCache.put(player.getUUID(), resolved);
                }
                callback.accept(resolved);
            });
        });
    }

    public synchronized void tick() {
        Iterator<Map.Entry<UUID, Integer>> iterator = remainingTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int ticks = entry.getValue() - 1;
            if (ticks <= 0) {
                // remainingTicks からは先にここで除去済み。
                // undisguise() 内でも remove() を呼ぶが no-op になる（二重除去は無害）。
                iterator.remove();
                MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                    if (player != null) {
                        undisguise(player);
                    }
                }
            } else {
                entry.setValue(ticks);
            }
        }
    }

    private void captureOriginalModelCustomizationIfNeeded(ServerPlayer player) {
        UUID playerUuid = player.getUUID();
        if (!originalModelCustomizations.containsKey(playerUuid)) {
            originalModelCustomizations.put(playerUuid, ModelCustomizationHelper.get(player));
        }
    }

    private void applyDisguiseModelCustomization(ServerPlayer player, UUID targetUuid) {
        byte customization = ModelCustomizationHelper.ALL_PARTS;
        if (targetUuid != null) {
            MinecraftServer server = player.level().getServer();
            if (server != null) {
                ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);
                if (target != null) {
                    customization = ModelCustomizationHelper.get(target);
                }
            }
        }
        ModelCustomizationHelper.set(player, customization);
    }

    private void syncPlayerToAll(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        ClientboundPlayerInfoRemovePacket removeInfoPacket = new ClientboundPlayerInfoRemovePacket(
                List.of(player.getUUID())
        );
        ClientboundPlayerInfoUpdatePacket addInfoPacket =
                ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player));
        ClientboundSetEntityDataPacket modelCustomizationPacket = ModelCustomizationHelper.createSyncPacket(player);

        for (ServerPlayer other : level.players()) {
            other.connection.send(removeInfoPacket);
            other.connection.send(addInfoPacket);
            other.connection.send(modelCustomizationPacket);
        }
    }
}
