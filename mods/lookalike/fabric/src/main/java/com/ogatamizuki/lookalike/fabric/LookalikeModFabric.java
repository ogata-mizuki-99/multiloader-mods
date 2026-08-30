package com.ogatamizuki.lookalike.fabric;

import com.mojang.authlib.properties.PropertyMap;
import com.ogatamizuki.lookalike.*;
import com.ogatamizuki.lookalike.cast.CastEffectTemplate;
import com.ogatamizuki.lookalike.cast.CastManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LookalikeModFabric implements ModInitializer {
    public static DisguiseMirrorItem DISGUISE_MIRROR;
    public static CreativeModeTab TAB;

    private static final Map<UUID, LookalikeAttachments.ScanHistory> SCAN_HISTORIES = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> RECEIVED_GUIDES = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        LookalikeCommon.LOGGER.info("Lookalike Mod (Fabric) Initializing...");
        FabricRegistryHelper.prepare();

        ResourceKey<Item> mirrorKey = ResourceKey.create(Registries.ITEM, LookalikeCommon.id("disguise_mirror"));
        DISGUISE_MIRROR = FabricRegistryHelper.register(
                BuiltInRegistries.ITEM,
                mirrorKey,
                new DisguiseMirrorItem(new Item.Properties().setId(mirrorKey).stacksTo(1).durability(5))
        );

        LookalikeCommon.DISGUISE_MIRROR = () -> DISGUISE_MIRROR;
        LookalikeCommon.sendToPlayer = (player, payload) -> ServerPlayNetworking.send(player, payload);
        LookalikeCommon.sendToTrackingAndSelf = (player, payload) -> {
            ServerPlayNetworking.send(player, payload);
            for (ServerPlayer tracking : PlayerLookup.tracking(player)) {
                ServerPlayNetworking.send(tracking, payload);
            }
        };

        LookalikePlatform.getScanHistory = player -> SCAN_HISTORIES.computeIfAbsent(player.getUUID(), k -> new LookalikeAttachments.ScanHistory());
        LookalikePlatform.setScanHistory = (player, history) -> SCAN_HISTORIES.put(player.getUUID(), history);
        LookalikePlatform.hasReceivedGuide = player -> RECEIVED_GUIDES.getOrDefault(player.getUUID(), false);
        LookalikePlatform.setReceivedGuide = (player, val) -> RECEIVED_GUIDES.put(player.getUUID(), val);
        LookalikePlatform.isModLoadedCheck = modId ->
                net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(modId);

        ResourceKey<CreativeModeTab> tabKey = ResourceKey.create(Registries.CREATIVE_MODE_TAB, LookalikeCommon.id("lookalike_tab"));
        TAB = FabricRegistryHelper.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                tabKey,
                FabricRegistryHelper.createTabBuilder()
                        .title(Component.translatable("itemGroup.lookalike"))
                        .icon(() -> DISGUISE_MIRROR.getDefaultInstance())
                        .displayItems((parameters, output) -> output.accept(DISGUISE_MIRROR))
                        .build()
        );

        // Network Payloads S2C
        PayloadTypeRegistry.clientboundPlay().register(NetworkPayloads.ScanHistorySyncPayload.TYPE, NetworkPayloads.ScanHistorySyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NetworkPayloads.DisguiseListSyncPayload.TYPE, NetworkPayloads.DisguiseListSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NetworkPayloads.ShadowAppearanceSyncPayload.TYPE, NetworkPayloads.ShadowAppearanceSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LookalikeClientFlagsPayload.TYPE, LookalikeClientFlagsPayload.STREAM_CODEC);

        // Network Payloads C2S
        PayloadTypeRegistry.serverboundPlay().register(NetworkPayloads.DisguiseRequestPayload.TYPE, NetworkPayloads.DisguiseRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(NetworkPayloads.ScanHistoryActionPayload.TYPE, NetworkPayloads.ScanHistoryActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LookalikeCommonConfigPushPayload.TYPE, LookalikeCommonConfigPushPayload.STREAM_CODEC);

        // Packet Receivers
        ServerPlayNetworking.registerGlobalReceiver(NetworkPayloads.DisguiseRequestPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                handleDisguiseRequest(player, payload);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NetworkPayloads.ScanHistoryActionPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                handleScanHistoryAction(player, payload);
            });
        });

        // Lifecycle & Server Tick
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            DisguiseManager.getInstance().cacheAuthenticTextures(player);
            DisguiseManager.syncScanHistory(player);
            DisguiseManager.getInstance().syncDisguiseListTo(player);
            ShadowAppearanceManager.getInstance().syncAppearanceTo(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            CastManager.getInstance().onPlayerLogout(player.getUUID());
            ShadowAppearanceManager.getInstance().disableShadow(player);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            CastManager.getInstance().tick(server);
            DisguiseManager.getInstance().tick(server);
            ShadowAppearanceManager.getInstance().tick(server);
        });

        // Commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LookalikeCommand.register(dispatcher);
        });
    }

    private static void handleDisguiseRequest(ServerPlayer player, NetworkPayloads.DisguiseRequestPayload payload) {
        try {
            ItemStack mainMirror = player.getItemInHand(InteractionHand.MAIN_HAND);
            final ItemStack mirror = mainMirror.is(DISGUISE_MIRROR)
                    ? mainMirror
                    : player.getItemInHand(InteractionHand.OFF_HAND);
            if (!mirror.is(DISGUISE_MIRROR)) {
                return;
            }

            UUID targetUuid = UUID.fromString(payload.targetUuidStr());
            if (targetUuid.equals(player.getUUID())) {
                DisguiseManager.getInstance().undisguise(player);
                return;
            }

            LookalikeAttachments.ScanHistory scanHistory = LookalikePlatform.getHistory(player);
            MinecraftServer server = player.level().getServer();
            if (server == null) {
                return;
            }

            ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);
            if (target != null) {
                PropertyMap textures = DisguiseManager.getInstance().resolveAuthenticTextures(target);
                PlayerSkin.Patch skinPatch = target.getProfile().skinPatch();
                if (!textures.isEmpty()) {
                    applyDisguiseFromMirror(player, mirror, textures, skinPatch, targetUuid);
                    return;
                }
                DisguiseManager.getInstance().resolveAuthenticTexturesAsync(target, resolved -> {
                    if (resolved.isEmpty()) {
                        player.sendSystemMessage(Component.translatable(
                                "commands.lookalike.disguise.skin_fail",
                                DisguiseManager.getStoredGameProfile(target).name()));
                        return;
                    }
                    applyDisguiseFromMirror(player, mirror, resolved, skinPatch, targetUuid);
                });
                return;
            }

            scanHistory.findEntry(targetUuid).ifPresent(entry ->
                    net.minecraft.world.item.component.ResolvableProfile
                            .createUnresolved(entry.uuid())
                            .resolveProfile(server.services().profileResolver())
                            .thenAccept(loadedProfile -> server.execute(() -> {
                                if (loadedProfile == null
                                        || !loadedProfile.properties().containsKey("textures")) {
                                    player.sendSystemMessage(Component.translatable(
                                            "commands.lookalike.disguise.skin_fail", entry.name()));
                                    return;
                                }
                                applyDisguiseFromMirror(
                                        player,
                                        mirror,
                                        DisguiseManager.copyTextureProperties(loadedProfile),
                                        PlayerSkin.Patch.EMPTY,
                                        entry.uuid()
                                );
                            })));
        } catch (Exception e) {
            LookalikeCommon.LOGGER.error("Failed to process disguise request", e);
        }
    }

    private static void applyDisguiseFromMirror(
            ServerPlayer player,
            ItemStack mirror,
            PropertyMap textures,
            PlayerSkin.Patch skinPatch,
            UUID targetUuid
    ) {
        if (!player.isCreative()) {
            EquipmentSlot slot = player.getItemInHand(InteractionHand.MAIN_HAND) == mirror
                    ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            mirror.hurtAndBreak(1, player, slot);
        }
        CastManager.getInstance().startCast(
                player,
                textures,
                skinPatch,
                targetUuid,
                60,
                0,
                CastEffectTemplate.WITCH_SMOKE
        );
    }

    private static void handleScanHistoryAction(ServerPlayer player, NetworkPayloads.ScanHistoryActionPayload payload) {
        if (payload.action() == NetworkPayloads.ScanHistoryActionPayload.ACTION_DELETE) {
            try {
                UUID targetUuid = UUID.fromString(payload.argument());
                LookalikeAttachments.ScanHistory history = LookalikePlatform.getHistory(player);
                if (history.removeEntry(targetUuid)) {
                    LookalikePlatform.setHistory(player, history);
                    DisguiseManager.syncScanHistory(player);
                }
            } catch (IllegalArgumentException e) {
                LookalikeCommon.LOGGER.warn("Invalid scan history delete uuid: {}", payload.argument());
            }
        }
    }
}
