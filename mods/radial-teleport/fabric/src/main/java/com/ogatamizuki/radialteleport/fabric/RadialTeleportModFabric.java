package com.ogatamizuki.radialteleport.fabric;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ogatamizuki.radialteleport.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

public class RadialTeleportModFabric implements ModInitializer {
    public static final String MODID = "radial_teleport";
    public static final Logger LOGGER = LogManager.getLogger(RadialTeleportModFabric.class);

    public static TeleportCompassItem TELEPORT_COMPASS;
    public static CreativeModeTab TAB;

    private static MinecraftServer currentServer;

    public static MinecraftServer getServer() {
        return currentServer;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Radial Teleport Mod (Fabric) Initializing...");
        FabricRegistryHelper.prepare();

        ResourceKey<Item> compassKey = ResourceKey.create(Registries.ITEM, RadialTeleportCommon.id("teleport_compass"));
        TELEPORT_COMPASS = FabricRegistryHelper.register(
                BuiltInRegistries.ITEM,
                compassKey,
                new TeleportCompassItem(new Item.Properties().setId(compassKey).stacksTo(1))
        );

        RadialTeleportCommon.TELEPORT_COMPASS = () -> TELEPORT_COMPASS;
        RadialTeleportCommon.sendToPlayer = (player, payload) -> ServerPlayNetworking.send(player, payload);
        RadialTeleportCommon.isModLoadedCheck = modId ->
                net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(modId);

        if (!RadialTeleportCommon.isModLoaded("werewolf")) {
            ResourceKey<CreativeModeTab> tabKey = ResourceKey.create(Registries.CREATIVE_MODE_TAB, RadialTeleportCommon.id("teleport_tab"));
            TAB = FabricRegistryHelper.register(
                    BuiltInRegistries.CREATIVE_MODE_TAB,
                    tabKey,
                    FabricRegistryHelper.createTabBuilder()
                            .title(Component.translatable("itemGroup.radial_teleport"))
                            .icon(() -> TELEPORT_COMPASS.getDefaultInstance())
                            .displayItems((parameters, output) -> output.accept(TELEPORT_COMPASS))
                            .build()
            );
        }

        // Register Payloads S2C
        PayloadTypeRegistry.clientboundPlay().register(TeleportDestinationsPayload.TYPE, TeleportDestinationsPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TeleportResultPayload.TYPE, TeleportResultPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WaypointListPayload.TYPE, WaypointListPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RadialTeleportClientFlagsPayload.TYPE, RadialTeleportClientFlagsPayload.STREAM_CODEC);

        // Register Payloads C2S
        PayloadTypeRegistry.serverboundPlay().register(RequestDestinationsPayload.TYPE, RequestDestinationsPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TeleportRequestPayload.TYPE, TeleportRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(WaypointActionPayload.TYPE, WaypointActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RadialTeleportCommonConfigPushPayload.TYPE, RadialTeleportCommonConfigPushPayload.STREAM_CODEC);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            currentServer = server;
            PlayerWaypointStorage.load(server);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            PlayerWaypointStorage.save(server);
            currentServer = null;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            ServerPlayNetworking.send(player, RadialTeleportClientFlagsPayload.fromConfig());
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestDestinationsPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                if (!isHoldingCompass(player) && !player.isSpectator()) {
                    return;
                }
                ServerPlayNetworking.send(player, TeleportService.buildDestinations(context.server(), player));
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(TeleportRequestPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                if (!canUseCompassMenu(player)) {
                    ServerPlayNetworking.send(player, TeleportResultPayload.message(false, "radial_teleport.message.not_using_item"));
                    return;
                }

                TeleportResultPayload result = TeleportService.teleport(player, payload.destinationId());
                ServerPlayNetworking.send(player, result);

                if (result.success() && player.isUsingItem()) {
                    player.stopUsingItem();
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(WaypointActionPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                switch (payload.action()) {
                    case WaypointActionPayload.ACTION_SAVE -> {
                        if (!isHoldingCompass(player)) return;
                        WaypointService.saveAtPlayer(player, payload.argument());
                    }
                    case WaypointActionPayload.ACTION_DELETE -> {
                        if (!isHoldingCompass(player)) return;
                        if (WaypointService.deleteByDestinationId(player, payload.argument())) {
                            sendWaypointList(player);
                        }
                    }
                    case WaypointActionPayload.ACTION_OPEN_EDIT -> {
                        if (!canUseCompassMenu(player)) return;
                        sendWaypointList(player);
                    }
                    case WaypointActionPayload.ACTION_RENAME -> {
                        if (!isHoldingCompass(player)) return;
                        String[] renameParts = splitWaypointArgument(payload.argument());
                        if (renameParts.length == 2 && WaypointService.renameByDestinationId(player, renameParts[0], renameParts[1])) {
                            sendWaypointList(player);
                        }
                    }
                    case WaypointActionPayload.ACTION_MOVE -> {
                        if (!isHoldingCompass(player)) return;
                        String[] moveParts = splitWaypointArgument(payload.argument());
                        if (moveParts.length == 2) {
                            boolean up = "up".equals(moveParts[1]);
                            if (WaypointService.moveByDestinationId(player, moveParts[0], up)) {
                                sendWaypointList(player);
                            }
                        }
                    }
                    default -> {}
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(RadialTeleportCommonConfigPushPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer serverPlayer = context.player();
                if (!serverPlayer.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                    serverPlayer.sendSystemMessage(
                            Component.translatable("radial_teleport.configuration.push_denied")
                                    .withStyle(ChatFormatting.RED));
                    return;
                }

                int maxWaypoints = Math.max(1, Math.min(32, payload.maxWaypointsPerPlayer()));
                int cooldownTicks = Math.max(0, Math.min(72000, payload.teleportCooldownTicks()));
                Config.setEnableCraftingRecipe(payload.enableCraftingRecipe());
                Config.setEnableWaypoints(payload.enableWaypoints());
                Config.setMaxWaypointsPerPlayer(maxWaypoints);
                Config.setTeleportCooldownTicks(cooldownTicks);

                MinecraftServer server = serverPlayer.level().getServer();
                if (server != null) {
                    RadialTeleportClientFlagsPayload flagsPayload = RadialTeleportClientFlagsPayload.fromConfig();
                    for (ServerPlayer p : PlayerLookup.all(server)) {
                        ServerPlayNetworking.send(p, flagsPayload);
                    }
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
                                .withStyle(ChatFormatting.GREEN));
            });
        });

        // Command Registration
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
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
        });
    }

    private static void sendWaypointList(ServerPlayer player) {
        ServerPlayNetworking.send(player, WaypointService.buildListPayload(player));
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
        return player.getMainHandItem().is(TELEPORT_COMPASS)
                || player.getOffhandItem().is(TELEPORT_COMPASS);
    }

    private static boolean canUseCompassMenu(ServerPlayer player) {
        if (player.isUsingItem() && player.getUseItem().is(TELEPORT_COMPASS)) {
            return true;
        }
        return player.isSpectator();
    }

    private static int giveCompass(CommandSourceStack source, Collection<ServerPlayer> targets)
            throws CommandSyntaxException {
        ItemStack stack = TELEPORT_COMPASS.getDefaultInstance();

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
