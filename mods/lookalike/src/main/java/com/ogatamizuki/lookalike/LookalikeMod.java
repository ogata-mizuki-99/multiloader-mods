package com.ogatamizuki.lookalike;

import com.mojang.authlib.properties.PropertyMap;
import com.ogatamizuki.lookalike.cast.CastEffectTemplate;
import com.ogatamizuki.lookalike.cast.CastManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

@Mod(LookalikeMod.MODID)
public class LookalikeMod {
    public static final String MODID = "lookalike";
    public static final Logger LOGGER = LogManager.getLogger(LookalikeMod.class);

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, MODID);

    public static final DeferredItem<DisguiseMirrorItem> DISGUISE_MIRROR = ITEMS.registerItem(
            "disguise_mirror",
            props -> new DisguiseMirrorItem(props.stacksTo(1).durability(5))
    );

    public LookalikeMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Lookalike Mod Initializing...");
        LookalikeConfigMigration.migrateConfigFile();
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        ITEMS.register(modEventBus);
        registerCreativeTabIfStandalone(modEventBus);
        CONDITION_CODECS.register(modEventBus);

        CONDITION_CODECS.register("crafting_recipe_enabled", () -> CraftingRecipeEnabledCondition.CODEC);

        LookalikeAttachments.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::onConfigReload);

        NeoForge.EVENT_BUS.register(this);
    }

    private static void registerCreativeTabIfStandalone(IEventBus modEventBus) {
        if (WerewolfBundleDetection.isBundled()) {
            LOGGER.info("Werewolf bundled; lookalike creative tab is omitted (use werewolf tab).");
            return;
        }
        CREATIVE_MODE_TABS.register(
                "lookalike_tab",
                () -> CreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.lookalike"))
                        .withTabsBefore(CreativeModeTabs.TOOLS_AND_UTILITIES)
                        .icon(() -> DISGUISE_MIRROR.get().getDefaultInstance())
                        .displayItems((parameters, output) -> output.accept(DISGUISE_MIRROR.get()))
                        .build()
        );
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != Config.SPEC) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        server.execute(() -> {
            syncClientFlagsToAllPlayers();
            server.reloadResources(server.getPackRepository().getSelectedIds())
                    .thenRun(() -> LOGGER.info("Reloaded datapacks after lookalike config change"));
        });
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(NetworkPayloads.ScanHistorySyncPayload.TYPE, NetworkPayloads.ScanHistorySyncPayload.STREAM_CODEC);
        registrar.playToClient(NetworkPayloads.DisguiseListSyncPayload.TYPE, NetworkPayloads.DisguiseListSyncPayload.STREAM_CODEC);
        registrar.playToClient(
                NetworkPayloads.ShadowAppearanceSyncPayload.TYPE,
                NetworkPayloads.ShadowAppearanceSyncPayload.STREAM_CODEC
        );
        registrar.playToClient(LookalikeClientFlagsPayload.TYPE, LookalikeClientFlagsPayload.STREAM_CODEC);

        registrar.playToServer(
                NetworkPayloads.DisguiseRequestPayload.TYPE,
                NetworkPayloads.DisguiseRequestPayload.STREAM_CODEC,
                this::handleDisguiseRequest
        );

        registrar.playToServer(
                NetworkPayloads.ScanHistoryActionPayload.TYPE,
                NetworkPayloads.ScanHistoryActionPayload.STREAM_CODEC,
                this::handleScanHistoryAction
        );

        registrar.playToServer(
                LookalikeCommonConfigPushPayload.TYPE,
                LookalikeCommonConfigPushPayload.STREAM_CODEC,
                this::handleCommonConfigPush
        );
    }

    private void handleCommonConfigPush(LookalikeCommonConfigPushPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (!serverPlayer.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                serverPlayer.sendSystemMessage(
                        Component.translatable("lookalike.configuration.push_denied")
                                .withStyle(ChatFormatting.RED));
                return;
            }

            int disguiseDuration = Math.max(1, Math.min(86400, payload.disguiseDurationSeconds()));
            int castTime = Math.max(0, Math.min(60, payload.defaultCastTimeSeconds()));
            String effectTemplate = com.ogatamizuki.lookalike.cast.CastEffectTemplate
                    .fromName(payload.defaultEffectTemplate())
                    .name();

            Config.disguiseDurationSeconds.set(disguiseDuration);
            Config.disguiseDurationSeconds.save();
            Config.allowDefaultPlayerList.set(payload.allowDefaultPlayerList());
            Config.allowDefaultPlayerList.save();
            Config.hideAllNametags.set(payload.hideAllNametags());
            Config.hideAllNametags.save();
            Config.enableMirrorCrafting.set(payload.enableMirrorCrafting());
            Config.enableMirrorCrafting.save();
            Config.defaultCastTimeSeconds.set(castTime);
            Config.defaultCastTimeSeconds.save();
            Config.defaultEffectTemplate.set(effectTemplate);
            Config.defaultEffectTemplate.save();

            syncClientFlagsToAllPlayers();

            LOGGER.info(
                    "Lookalike common config pushed by {}: disguiseDurationSeconds={}, allowDefaultPlayerList={}, hideAllNametags={}, enableMirrorCrafting={}, defaultCastTimeSeconds={}, defaultEffectTemplate={}",
                    serverPlayer.getGameProfile().name(),
                    Config.disguiseDurationSeconds.get(),
                    Config.allowDefaultPlayerList.get(),
                    Config.hideAllNametags.get(),
                    Config.enableMirrorCrafting.get(),
                    Config.defaultCastTimeSeconds.get(),
                    Config.defaultEffectTemplate.get()
            );
            serverPlayer.sendSystemMessage(
                    Component.translatable("lookalike.configuration.push_ok")
                            .withStyle(ChatFormatting.GREEN));
        });
    }

    public static void syncClientFlagsToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, LookalikeClientFlagsPayload.fromConfig());
    }

    public static void syncClientFlagsToAllPlayers() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        PacketDistributor.sendToAllPlayers(LookalikeClientFlagsPayload.fromConfig());
    }

    private void handleScanHistoryAction(NetworkPayloads.ScanHistoryActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (payload.action() == NetworkPayloads.ScanHistoryActionPayload.ACTION_DELETE) {
                try {
                    java.util.UUID targetUuid = java.util.UUID.fromString(payload.argument());
                    LookalikeAttachments.ScanHistory history = player.getData(LookalikeAttachments.SCAN_HISTORY);
                    if (history.removeEntry(targetUuid)) {
                        player.setData(LookalikeAttachments.SCAN_HISTORY, history);
                        syncScanHistory(player);
                    }
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Invalid scan history delete uuid: {}", payload.argument());
                }
            }
        });
    }

    private void handleDisguiseRequest(NetworkPayloads.DisguiseRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            try {
                ItemStack mainMirror = player.getItemInHand(InteractionHand.MAIN_HAND);
                final ItemStack mirror = mainMirror.is(DISGUISE_MIRROR.get())
                        ? mainMirror
                        : player.getItemInHand(InteractionHand.OFF_HAND);
                if (!mirror.is(DISGUISE_MIRROR.get())) {
                    return;
                }

                UUID targetUuid = UUID.fromString(payload.targetUuidStr());
                if (targetUuid.equals(player.getUUID())) {
                    DisguiseManager.getInstance().undisguise(player);
                    return;
                }

                LookalikeAttachments.ScanHistory scanHistory = player.getData(LookalikeAttachments.SCAN_HISTORY);
                // allowDefaultPlayerList=false の場合、スキャン済みプレイヤーのみ変装を許可する。
                // allowDefaultPlayerList=true の場合は任意 UUID への変装を許可する設計
                // （アイテム所持チェックはサーバー側で行っており、クライアント改ざんへの緩和策は設定で管理）。
                if (!Config.allowDefaultPlayerList.get()
                        && scanHistory.findEntry(targetUuid).isEmpty()) {
                    return;
                }

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
                LOGGER.error("Failed to process disguise request", e);
            }
        });
    }

    private static void applyDisguiseFromMirror(
            ServerPlayer player,
            ItemStack mirror,
            com.mojang.authlib.properties.PropertyMap textures,
            PlayerSkin.Patch skinPatch,
            UUID targetUuid
    ) {
        if (!player.isCreative()) {
            EquipmentSlot slot = player.getItemInHand(InteractionHand.MAIN_HAND).is(DISGUISE_MIRROR.get())
                    ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            mirror.hurtAndBreak(1, player, slot);
        }
        CastManager.getInstance().startCast(
                player,
                textures,
                skinPatch,
                targetUuid,
                Config.disguiseDurationSeconds.get(),
                Config.defaultCastTimeSeconds.get(),
                CastEffectTemplate.fromName(Config.defaultEffectTemplate.get())
        );
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DisguiseManager.getInstance().cacheAuthenticTextures(player);
            syncScanHistory(player);

            // ログインしたプレイヤーに現在の変装リストを同期
            MinecraftServer server = player.level().getServer();
            if (server != null) {
                syncClientFlagsToPlayer(player);
                DisguiseManager.getInstance().syncDisguiseListTo(player);
                ShadowAppearanceManager.getInstance().syncAppearanceTo(player);
            }

            if (GuideLibIntegration.isAvailable()) {
                boolean received = player.getData(LookalikeAttachments.RECEIVED_GUIDE);
                if (!received) {
                    ItemStack book = GuideLibIntegration.createGuideBook();
                    if (!book.isEmpty()) {
                        if (!player.getInventory().add(book)) {
                            player.drop(book, false);
                        } else {
                            player.inventoryMenu.broadcastChanges();
                        }
                        player.setData(LookalikeAttachments.RECEIVED_GUIDE, true);
                    }
                }
            }
        }
    }

    public static void syncScanHistory(ServerPlayer player) {
        LookalikeAttachments.ScanHistory history = player.getData(LookalikeAttachments.SCAN_HISTORY);
        PacketDistributor.sendToPlayer(player, new NetworkPayloads.ScanHistorySyncPayload(history.getEntries()));
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CastManager.getInstance().onPlayerLogout(player.getUUID());
            ShadowAppearanceManager.getInstance().disableShadow(player);
        }
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CastManager.getInstance().onPlayerDamaged(player);
        }
    }

    // 名前とリストの偽装イベントハンドラーはなりすまし防止のため削除されました

    @SubscribeEvent
    public void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        LookalikeCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        CastManager.getInstance().tick();
        DisguiseManager.getInstance().tick();
        ShadowAppearanceManager.getInstance().tick();
    }
}

