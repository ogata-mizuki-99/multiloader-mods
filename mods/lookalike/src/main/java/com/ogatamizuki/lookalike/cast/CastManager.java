package com.ogatamizuki.lookalike.cast;

import com.mojang.authlib.properties.PropertyMap;
import com.ogatamizuki.lookalike.DisguiseManager;
import com.ogatamizuki.lookalike.LookalikeMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CastManager {
    private static final CastManager INSTANCE = new CastManager();
    private static final double MOVE_CANCEL_DISTANCE_SQ = 1.0;
    private static final int EFFECT_INTERVAL_TICKS = 10;

    private final Map<UUID, CastSession> activeSessions = new ConcurrentHashMap<>();

    private CastManager() {}

    public static CastManager getInstance() {
        return INSTANCE;
    }

    public boolean isCasting(UUID playerUuid) {
        return activeSessions.containsKey(playerUuid);
    }

    public void startCast(
            ServerPlayer player,
            ServerPlayer target,
            int durationSeconds,
            int castTimeSeconds,
            CastEffectTemplate effect
    ) {
        if (castTimeSeconds <= 0) {
            DisguiseManager.getInstance().disguise(player, target, durationSeconds);
            return;
        }

        PropertyMap textures = DisguiseManager.getInstance().resolveAuthenticTextures(target);
        if (!textures.isEmpty()) {
            beginSession(
                    player,
                    textures,
                    target.getProfile().skinPatch(),
                    target.getUUID(),
                    durationSeconds,
                    castTimeSeconds,
                    effect
            );
            return;
        }

        DisguiseManager.getInstance().resolveAuthenticTexturesAsync(target, resolved -> {
            if (resolved.isEmpty()) {
                player.sendSystemMessage(Component.translatable(
                        "commands.lookalike.disguise.skin_fail",
                        DisguiseManager.getStoredGameProfile(target).name()));
                return;
            }
            beginSession(
                    player,
                    resolved,
                    target.getProfile().skinPatch(),
                    target.getUUID(),
                    durationSeconds,
                    castTimeSeconds,
                    effect
            );
        });
    }

    public void startCast(
            ServerPlayer player,
            PropertyMap textures,
            PlayerSkin.Patch skinPatch,
            UUID targetUuid,
            int durationSeconds,
            int castTimeSeconds,
            CastEffectTemplate effect
    ) {
        if (textures.isEmpty()) {
            player.sendSystemMessage(Component.translatable("commands.lookalike.disguise.no_skin_data"));
            return;
        }
        if (castTimeSeconds <= 0) {
            DisguiseManager.getInstance().disguise(player, textures, skinPatch, targetUuid, durationSeconds);
            return;
        }
        beginSession(player, textures, skinPatch, targetUuid, durationSeconds, castTimeSeconds, effect);
    }

    public void startCastAsName(
            ServerPlayer player,
            String targetName,
            int durationSeconds,
            int castTimeSeconds,
            CastEffectTemplate effect
    ) {
        if (castTimeSeconds <= 0) {
            DisguiseManager.getInstance().disguiseAsName(player, targetName, durationSeconds);
            return;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }

        UUID targetUuid = resolveNicknameUuid(targetName);
        net.minecraft.world.item.component.ResolvableProfile resolvable = targetUuid != null
                ? net.minecraft.world.item.component.ResolvableProfile.createUnresolved(targetUuid)
                : net.minecraft.world.item.component.ResolvableProfile.createUnresolved(targetName);

        resolvable.resolveProfile(server.services().profileResolver()).thenAccept(loadedProfile -> {
            server.execute(() -> {
                if (loadedProfile == null || !loadedProfile.properties().containsKey("textures")) {
                    player.sendSystemMessage(Component.translatable(
                            "commands.lookalike.disguise.skin_fail", targetName));
                    return;
                }
                beginSession(
                        player,
                        DisguiseManager.copyTexturePropertiesUnsigned(loadedProfile),
                        PlayerSkin.Patch.EMPTY,
                        loadedProfile.id(),
                        durationSeconds,
                        castTimeSeconds,
                        effect
                );
            });
        }).exceptionally(error -> {
            server.execute(() -> player.sendSystemMessage(Component.translatable(
                    "commands.lookalike.disguise.error", targetName, error.getMessage())));
            return null;
        });
    }

    public boolean cancelCastIfActive(ServerPlayer player) {
        return activeSessions.remove(player.getUUID()) != null;
    }

    public void cancelCast(ServerPlayer player, String reasonKey) {
        if (activeSessions.remove(player.getUUID()) != null) {
            player.sendSystemMessage(Component.translatable("lookalike.cast.cancelled." + reasonKey));
        }
    }

    public void onPlayerLogout(UUID playerUuid) {
        activeSessions.remove(playerUuid);
    }

    public void onPlayerDamaged(ServerPlayer player) {
        if (isCasting(player.getUUID())) {
            cancelCast(player, "damage");
        }
    }

    public void tick() {
        if (activeSessions.isEmpty()) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        Iterator<Map.Entry<UUID, CastSession>> iterator = activeSessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, CastSession> entry = iterator.next();
            CastSession session = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }

            if (shouldCancelForMovement(player, session)) {
                iterator.remove();
                player.sendSystemMessage(Component.translatable("lookalike.cast.cancelled.movement"));
                continue;
            }

            session.ticksElapsed++;
            if (session.ticksElapsed % EFFECT_INTERVAL_TICKS == 0) {
                session.effect.play(player, session.ticksElapsed == EFFECT_INTERVAL_TICKS);
            }

            session.remainingCastTicks--;
            if (session.remainingCastTicks <= 0) {
                iterator.remove();
                completeCast(player, session);
            }
        }
    }

    private void beginSession(
            ServerPlayer player,
            PropertyMap textures,
            PlayerSkin.Patch skinPatch,
            UUID targetUuid,
            int durationSeconds,
            int castTimeSeconds,
            CastEffectTemplate effect
    ) {
        activeSessions.put(player.getUUID(), new CastSession(
                textures,
                skinPatch != null ? skinPatch : PlayerSkin.Patch.EMPTY,
                targetUuid,
                durationSeconds,
                castTimeSeconds * 20,
                effect,
                player.getX(),
                player.getZ()
        ));
        if (effect != CastEffectTemplate.NONE) {
            effect.play(player, true);
        }
    }

    private static boolean shouldCancelForMovement(ServerPlayer player, CastSession session) {
        if (player.isSprinting()) {
            return true;
        }
        double dx = player.getX() - session.startX;
        double dz = player.getZ() - session.startZ;
        return dx * dx + dz * dz > MOVE_CANCEL_DISTANCE_SQ;
    }

    private static void completeCast(ServerPlayer player, CastSession session) {
        DisguiseManager.getInstance().disguise(
                player,
                session.textures,
                session.skinPatch,
                session.targetUuid,
                session.durationSeconds
        );
    }

    private static UUID resolveNicknameUuid(String name) {
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
            // nickname MOD が未導入
        } catch (Exception e) {
            LookalikeMod.LOGGER.warn("[lookalike] Error while integrating with nickname MOD", e);
        }
        return null;
    }

    private static final class CastSession {
        private final PropertyMap textures;
        private final PlayerSkin.Patch skinPatch;
        private final UUID targetUuid;
        private final int durationSeconds;
        private int remainingCastTicks;
        private final CastEffectTemplate effect;
        private final double startX;
        private final double startZ;
        private int ticksElapsed;

        private CastSession(
                PropertyMap textures,
                PlayerSkin.Patch skinPatch,
                UUID targetUuid,
                int durationSeconds,
                int remainingCastTicks,
                CastEffectTemplate effect,
                double startX,
                double startZ
        ) {
            this.textures = textures;
            this.skinPatch = skinPatch;
            this.targetUuid = targetUuid;
            this.durationSeconds = durationSeconds;
            this.remainingCastTicks = remainingCastTicks;
            this.effect = effect;
            this.startX = startX;
            this.startZ = startZ;
        }
    }
}
