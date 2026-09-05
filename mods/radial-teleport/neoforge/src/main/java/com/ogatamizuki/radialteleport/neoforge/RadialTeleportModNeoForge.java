package com.ogatamizuki.radialteleport.neoforge;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.ogatamizuki.radialteleport.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

@Mod(RadialTeleportModNeoForge.MODID)
public class RadialTeleportModNeoForge {
    public static final String MODID = "radial_teleport";
    public static final Logger LOGGER = LogManager.getLogger(RadialTeleportModNeoForge.class);

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, MODID);

    public static final DeferredItem<TeleportCompassItem> TELEPORT_COMPASS = ITEMS.registerItem(
            "teleport_compass",
            props -> new TeleportCompassItem(props.stacksTo(1))
    );

    public RadialTeleportModNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Radial Teleport Mod (NeoForge) Initializing...");

        RadialTeleportCommon.TELEPORT_COMPASS = TELEPORT_COMPASS;
        RadialTeleportCommon.sendToPlayer = PacketDistributor::sendToPlayer;
        RadialTeleportCommon.isModLoadedCheck = modId -> net.neoforged.fml.ModList.get().isLoaded(modId);

        ITEMS.register(modEventBus);
        registerCreativeTabIfStandalone(modEventBus);
        CONDITION_CODECS.register("crafting_recipe_enabled", () -> CraftingRecipeEnabledCondition.CODEC);
        CONDITION_CODECS.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::onConfigReload);

        modContainer.registerConfig(ModConfig.Type.COMMON, ConfigNeoForge.SPEC);
        modEventBus.addListener((ModConfigEvent.Loading e) -> ConfigNeoForge.sync());

        NeoForge.EVENT_BUS.register(this);
    }

    private static void registerCreativeTabIfStandalone(IEventBus modEventBus) {
        if (WerewolfBundleDetection.isBundled()) {
            LOGGER.info("Werewolf bundled; radial_teleport creative tab is omitted (use werewolf tab).");
            return;
        }
        CREATIVE_MODE_TABS.register(
                "teleport_tab",
                () -> CreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.radial_teleport"))
                        .withTabsBefore(CreativeModeTabs.TOOLS_AND_UTILITIES)
                        .icon(() -> TELEPORT_COMPASS.get().getDefaultInstance())
                        .displayItems((parameters, output) -> output.accept(TELEPORT_COMPASS.get()))
                        .build()
        );
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != ConfigNeoForge.SPEC) {
            return;
        }
        ConfigNeoForge.sync();

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        server.execute(() -> {
            syncClientFlagsToAllPlayers();
            server.reloadResources(server.getPackRepository().getSelectedIds())
                    .thenRun(() -> LOGGER.info("Reloaded datapacks after radial_teleport config change"));
        });
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(TeleportDestinationsPayload.TYPE, TeleportDestinationsPayload.STREAM_CODEC);
        registrar.playToClient(TeleportResultPayload.TYPE, TeleportResultPayload.STREAM_CODEC);
        registrar.playToClient(WaypointListPayload.TYPE, WaypointListPayload.STREAM_CODEC);
        registrar.playToClient(RadialTeleportClientFlagsPayload.TYPE, RadialTeleportClientFlagsPayload.STREAM_CODEC);

        registrar.playToServer(
                RequestDestinationsPayload.TYPE,
                RequestDestinationsPayload.STREAM_CODEC,
                this::handleRequestDestinations
        );

        registrar.playToServer(
                TeleportRequestPayload.TYPE,
                TeleportRequestPayload.STREAM_CODEC,
                this::handleTeleportRequest
        );

        registrar.playToServer(
                WaypointActionPayload.TYPE,
                WaypointActionPayload.STREAM_CODEC,
                this::handleWaypointAction
        );

        registrar.playToServer(
                RadialTeleportCommonConfigPushPayload.TYPE,
                RadialTeleportCommonConfigPushPayload.STREAM_CODEC,
                this::handleCommonConfigPush
        );
    }

    private void handleCommonConfigPush(RadialTeleportCommonConfigPushPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (!serverPlayer.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                serverPlayer.sendSystemMessage(
                        Component.translatable("radial_teleport.configuration.push_denied")
                                .withStyle(net.minecraft.ChatFormatting.RED));
                return;
            }

            int maxWaypoints = Math.max(1, Math.min(32, payload.maxWaypointsPerPlayer()));
            int cooldownTicks = Math.max(0, Math.min(72000, payload.teleportCooldownTicks()));
            ConfigNeoForge.updateFromPush(
                    payload.enableCraftingRecipe(),
                    payload.enableWaypoints(),
                    maxWaypoints,
                    cooldownTicks
            );

            MinecraftServer server = serverPlayer.level().getServer();
            if (server != null) {
                syncClientFlagsToAllPlayers();
                server.execute(() -> server.reloadResources(server.getPackRepository().getSelectedIds())
                        .thenRun(() -> LOGGER.info("Reloaded datapacks after radial_teleport config push")));
            }

            LOGGER.info(
                    "Radial Teleport common config pushed by {}: enableCraftingRecipe={}, enableWaypoints={}, maxWaypointsPerPlayer={}, teleportCooldownTicks={}",
                    serverPlayer.getGameProfile().name(),
                    Config.isEnableCraftingRecipe(),
                    Config.isEnableWaypoints(),
                    Config.getMaxWaypointsPerPlayer(),
                    Config.getTeleportCooldownTicks());
            serverPlayer.sendSystemMessage(
                    Component.translatable("radial_teleport.configuration.push_ok")
                            .withStyle(net.minecraft.ChatFormatting.GREEN));
        });
    }

    public static void syncClientFlagsToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, RadialTeleportClientFlagsPayload.fromConfig());
    }

    public static void syncClientFlagsToAllPlayers() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        PacketDistributor.sendToAllPlayers(RadialTeleportClientFlagsPayload.fromConfig());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncClientFlagsToPlayer(player);
        }
    }

    private void handleRequestDestinations(RequestDestinationsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!isHoldingCompass(player) && !player.isSpectator()) {
                return;
            }

            var server = player.level().getServer();
            if (server == null) {
                return;
            }

            PacketDistributor.sendToPlayer(
                    player,
                    TeleportService.buildDestinations(server, player)
            );
        });
    }

    private void handleWaypointAction(WaypointActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            switch (payload.action()) {
                case WaypointActionPayload.ACTION_SAVE -> {
                    if (!isHoldingCompass(player)) {
                        return;
                    }
                    WaypointService.saveAtPlayer(player, payload.argument());
                }
                case WaypointActionPayload.ACTION_DELETE -> {
                    if (!isHoldingCompass(player)) {
                        return;
                    }
                    if (WaypointService.deleteByDestinationId(player, payload.argument())) {
                        sendWaypointList(player);
                    }
                }
                case WaypointActionPayload.ACTION_OPEN_EDIT -> {
                    if (!canUseCompassMenu(player)) {
                        return;
                    }
                    sendWaypointList(player);
                }
                case WaypointActionPayload.ACTION_RENAME -> {
                    if (!isHoldingCompass(player)) {
                        return;
                    }
                    String[] renameParts = splitWaypointArgument(payload.argument());
                    if (renameParts.length == 2
                            && WaypointService.renameByDestinationId(player, renameParts[0], renameParts[1])) {
                        sendWaypointList(player);
                    }
                }
                case WaypointActionPayload.ACTION_MOVE -> {
                    if (!isHoldingCompass(player)) {
                        return;
                    }
                    String[] moveParts = splitWaypointArgument(payload.argument());
                    if (moveParts.length == 2) {
                        boolean up = "up".equals(moveParts[1]);
                        if (WaypointService.moveByDestinationId(player, moveParts[0], up)) {
                            sendWaypointList(player);
                        }
                    }
                }
                default -> {
                }
            }
        });
    }

    private static void sendWaypointList(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, WaypointService.buildListPayload(player));
    }

    private static String[] splitWaypointArgument(String argument) {
        int separator = argument.indexOf(WaypointActionPayload.ARG_SEPARATOR);
        if (separator < 0) {
            return new String[0];
        }
        return new String[]{
                argument.substring(0, separator),
                argument.substring(separator + 1)
        };
    }

    private static boolean isHoldingCompass(ServerPlayer player) {
        return player.getMainHandItem().is(TELEPORT_COMPASS.get())
                || player.getOffhandItem().is(TELEPORT_COMPASS.get());
    }

    private void handleTeleportRequest(TeleportRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!canUseCompassMenu(player)) {
                PacketDistributor.sendToPlayer(
                        player,
                        TeleportResultPayload.message(false, "radial_teleport.message.not_using_item")
                );
                return;
            }

            TeleportResultPayload result = TeleportService.teleport(player, payload.destinationId());
            PacketDistributor.sendToPlayer(player, result);

            if (result.success() && player.isUsingItem()) {
                player.stopUsingItem();
            }
        });
    }

    private static boolean canUseCompassMenu(ServerPlayer player) {
        if (player.isUsingItem() && player.getUseItem().is(TELEPORT_COMPASS.get())) {
            return true;
        }
        // スペクテーター: ホットバー不可のため所持なしでもメニュー/テレポートを許可
        return player.isSpectator();
    }

    @SubscribeEvent
    public void onServerStarting(net.neoforged.neoforge.event.server.ServerStartingEvent event) {
        PlayerWaypointStorage.load(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        PlayerWaypointStorage.save(event.getServer());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("radialteleport")
                        .then(Commands.literal("give")
                                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> giveCompass(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets")
                                        ))
                                )
                        )
        );
    }

    private static int giveCompass(CommandSourceStack source, Collection<ServerPlayer> targets)
            throws CommandSyntaxException {
        ItemStack stack = TELEPORT_COMPASS.get().getDefaultInstance();

        for (ServerPlayer target : targets) {
            ItemStack copy = stack.copy();
            if (!target.getInventory().add(copy)) {
                target.drop(copy, false);
            } else {
                target.inventoryMenu.broadcastChanges();
            }
        }

        source.sendSuccess(() -> Component.translatable("radial_teleport.message.given"), true);
        return targets.size();
    }
}
