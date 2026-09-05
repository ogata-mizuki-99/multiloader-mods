package com.ogatamizuki.instantstructure.fabric;

import com.ogatamizuki.instantstructure.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

public class InstantStructureModFabric implements ModInitializer {
    public static StructureMarkerItem STRUCTURE_MARKER;
    public static InstantBuilderItem INSTANT_BUILDER;
    public static CreativeModeTab TAB;

    @Override
    public void onInitialize() {
        InstantStructureCommon.LOGGER.info("Instant Structure Mod (Fabric) Initializing...");
        FabricRegistryHelper.prepare();

        ResourceKey<Item> markerKey = ResourceKey.create(Registries.ITEM, InstantStructureCommon.id("structure_marker"));
        ResourceKey<Item> builderKey = ResourceKey.create(Registries.ITEM, InstantStructureCommon.id("instant_builder"));

        STRUCTURE_MARKER = FabricRegistryHelper.register(
                BuiltInRegistries.ITEM,
                markerKey,
                new StructureMarkerItem(new Item.Properties().setId(markerKey).stacksTo(1))
        );

        INSTANT_BUILDER = FabricRegistryHelper.register(
                BuiltInRegistries.ITEM,
                builderKey,
                new InstantBuilderItem(new Item.Properties().setId(builderKey).stacksTo(1))
        );

        InstantStructureCommon.STRUCTURE_MARKER = () -> STRUCTURE_MARKER;
        InstantStructureCommon.INSTANT_BUILDER = () -> INSTANT_BUILDER;
        InstantStructurePlatform.sendToPlayer = (player, payload) -> ServerPlayNetworking.send(player, payload);
        InstantStructurePlatform.getConfigDir = () -> net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        InstantStructurePlatform.isModLoadedCheck = modId ->
                net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(modId);

        InstantStructureServerOps.initDirectories();
        InstantStructureConfig.load();

        ServerTickEvents.END_SERVER_TICK.register(server -> SpreadBuildManager.tickServer());

        ResourceKey<CreativeModeTab> tabKey = ResourceKey.create(Registries.CREATIVE_MODE_TAB, InstantStructureCommon.id("instant_structure_tab"));
        TAB = FabricRegistryHelper.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                tabKey,
                FabricRegistryHelper.createTabBuilder()
                        .title(Component.translatable("itemGroup.instant_structure"))
                        .icon(() -> INSTANT_BUILDER.getDefaultInstance())
                        .displayItems((parameters, output) -> {
                            output.accept(STRUCTURE_MARKER);
                            output.accept(INSTANT_BUILDER);
                        })
                        .build()
        );

        // Network Payloads S2C
        PayloadTypeRegistry.clientboundPlay().register(SelectionSyncPayload.TYPE, SelectionSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TemplatesListPayload.TYPE, TemplatesListPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TemplatePreviewPayload.TYPE, TemplatePreviewPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenExportDialogPayload.TYPE, OpenExportDialogPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BuildResultPayload.TYPE, BuildResultPayload.STREAM_CODEC);

        // Network Payloads C2S
        PayloadTypeRegistry.serverboundPlay().register(AdjustHeightPayload.TYPE, AdjustHeightPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BuildRequestPayload.TYPE, BuildRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestTemplatesPayload.TYPE, RequestTemplatesPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestPreviewPayload.TYPE, RequestPreviewPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ExportSubmitPayload.TYPE, ExportSubmitPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ExportCancelPayload.TYPE, ExportCancelPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DeleteTemplatePayload.TYPE, DeleteTemplatePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(InstantStructureCommonConfigPushPayload.TYPE, InstantStructureCommonConfigPushPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(AdjustHeightPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> InstantStructureServerOps.handleAdjustHeight(context.player(), payload.delta()));
        });
        ServerPlayNetworking.registerGlobalReceiver(BuildRequestPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> InstantStructureServerOps.handleBuildRequest(context.player(), payload));
        });
        ServerPlayNetworking.registerGlobalReceiver(RequestTemplatesPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> InstantStructureServerOps.sendTemplatesToClient(context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(RequestPreviewPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> InstantStructureServerOps.sendPreviewToClient(context.player(), payload.category(), payload.templateName()));
        });
        ServerPlayNetworking.registerGlobalReceiver(ExportSubmitPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> InstantStructureServerOps.handleExportSubmit(context.player(), payload.name(), payload.category()));
        });
        ServerPlayNetworking.registerGlobalReceiver(ExportCancelPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> InstantStructureServerOps.handleExportCancel(context.player(), payload.clearCompletely()));
        });
        ServerPlayNetworking.registerGlobalReceiver(DeleteTemplatePayload.TYPE, (payload, context) -> {
            context.server().execute(() -> InstantStructureServerOps.handleDeleteTemplate(context.player(), payload.category(), payload.templateName()));
        });
        ServerPlayNetworking.registerGlobalReceiver(InstantStructureCommonConfigPushPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                if (!player.createCommandSourceStack().permissions().hasPermission(
                        net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) {
                    player.sendSystemMessage(Component.translatable("instant_structure.screen.config.push_denied")
                            .withStyle(net.minecraft.ChatFormatting.RED));
                    return;
                }
                InstantStructureConfig.enableCraftingRecipe = payload.enableCraftingRecipe();
                InstantStructureConfig.enableMaterialConsumption = payload.enableMaterialConsumption();
                InstantStructureConfig.dropClearedBlocks = payload.dropClearedBlocks();
                InstantStructureConfig.save();
                InstantStructureCommon.LOGGER.info(
                        "Instant Structure common config pushed by {}: recipe={}, material={}, drop={}",
                        player.getGameProfile().name(),
                        InstantStructureConfig.enableCraftingRecipe,
                        InstantStructureConfig.enableMaterialConsumption,
                        InstantStructureConfig.dropClearedBlocks);
                player.sendSystemMessage(Component.translatable("instant_structure.screen.config.push_ok")
                        .withStyle(net.minecraft.ChatFormatting.GREEN));
            });
        });
    }
}
