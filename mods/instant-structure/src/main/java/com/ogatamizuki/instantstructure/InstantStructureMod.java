package com.ogatamizuki.instantstructure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.common.conditions.ICondition;
import com.mojang.serialization.MapCodec;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod(InstantStructureMod.MODID)
public class InstantStructureMod {
    public static final String MODID = "instant_structure";
    public static final Logger LOGGER = LogManager.getLogger(InstantStructureMod.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> VALID_CATEGORIES = Set.of("houses", "arenas", "custom");
    private static final Set<String> VALID_DIRECTIONS = Set.of("up", "down", "north", "south", "east", "west");
    private static final SuggestionProvider<CommandSourceStack> CATEGORY_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(VALID_CATEGORIES, builder);
    private static final SuggestionProvider<CommandSourceStack> DIRECTION_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(VALID_DIRECTIONS, builder);

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, MODID);

    public static final DeferredItem<StructureMarkerItem> STRUCTURE_MARKER = ITEMS.registerItem(
            "structure_marker",
            props -> new StructureMarkerItem(props.stacksTo(1))
    );

    public static final DeferredItem<InstantBuilderItem> INSTANT_BUILDER = ITEMS.registerItem(
            "instant_builder",
            props -> new InstantBuilderItem(props.stacksTo(1))
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_MODE_TABS.register(
            "instant_structure_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.instant_structure"))
                    .icon(() -> INSTANT_BUILDER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(STRUCTURE_MARKER.get());
                        output.accept(INSTANT_BUILDER.get());
                        if (GuideLibIntegration.isAvailable()) {
                            ItemStack book = GuideLibIntegration.createGuideBook();
                            if (!book.isEmpty()) {
                                output.accept(book);
                            }
                        }
                    })
                    .build()
    );

    // Server-side selections map
    private static final Map<UUID, Selection> SELECTIONS = new ConcurrentHashMap<>();

    public InstantStructureMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Instant Structure Mod Initializing...");

        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        CONDITION_CODECS.register("crafting_recipe_enabled", () -> CraftingRecipeEnabledCondition.CODEC);
        CONDITION_CODECS.register(modEventBus);

        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::onConfigReload);

        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, Config.SPEC);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(SpreadBuildManager.class);

        // Auto-create directories
        createDirs();
    }

    private void onConfigReload(net.neoforged.fml.event.config.ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != Config.SPEC) {
            return;
        }
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> server.reloadResources(server.getPackRepository().getSelectedIds())
                    .thenRun(() -> LOGGER.info("Reloaded datapacks after instant-structure config change")));
        }
    }

    private void createDirs() {
        try {
            Path root = InstantStructurePaths.configRoot();
            Path housesDir = root.resolve("templates-structure/houses");
            Path arenasDir = root.resolve("templates-structure/arenas");
            Files.createDirectories(housesDir);
            Files.createDirectories(arenasDir);
            Files.createDirectories(root.resolve("templates-structure/custom"));
            LOGGER.info("Successfully created instant-structure directories");

            // Extract initial templates if they do not exist
            copyResourceIfNeeded("/assets/instant_structure/templates/arenas/village.json", arenasDir.resolve("village.json"));
            copyResourceIfNeeded("/assets/instant_structure/templates/arenas/village.nbt", arenasDir.resolve("village.nbt"));
            copyResourceIfNeeded("/assets/instant_structure/templates/houses/house.json", housesDir.resolve("house.json"));
            copyResourceIfNeeded("/assets/instant_structure/templates/houses/house.nbt", housesDir.resolve("house.nbt"));
        } catch (Exception e) {
            LOGGER.error("Failed to create directories", e);
        }
    }

    private void copyResourceIfNeeded(String resourcePath, Path destPath) {
        if (Files.exists(destPath)) {
            return;
        }
        try (java.io.InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                LOGGER.warn("Initial template resource not found: " + resourcePath);
                return;
            }
            Files.copy(in, destPath);
            LOGGER.info("Successfully extracted initial template: " + destPath.getFileName());
        } catch (Exception e) {
            LOGGER.error("Failed to extract initial template resource " + resourcePath + " to " + destPath, e);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Server -> Client
        registrar.playToClient(SelectionSyncPayload.TYPE, SelectionSyncPayload.STREAM_CODEC);
        registrar.playToClient(TemplatesListPayload.TYPE, TemplatesListPayload.STREAM_CODEC);
        registrar.playToClient(TemplatePreviewPayload.TYPE, TemplatePreviewPayload.STREAM_CODEC);
        registrar.playToClient(OpenExportDialogPayload.TYPE, OpenExportDialogPayload.STREAM_CODEC);
        registrar.playToClient(BuildResultPayload.TYPE, BuildResultPayload.STREAM_CODEC);

        // Client -> Server
        registrar.playToServer(
                AdjustHeightPayload.TYPE,
                AdjustHeightPayload.STREAM_CODEC,
                this::handleAdjustHeight
        );

        registrar.playToServer(
                BuildRequestPayload.TYPE,
                BuildRequestPayload.STREAM_CODEC,
                this::handleBuildRequest
        );

        registrar.playToServer(
                RequestTemplatesPayload.TYPE,
                RequestTemplatesPayload.STREAM_CODEC,
                this::handleRequestTemplates
        );

        registrar.playToServer(
                RequestPreviewPayload.TYPE,
                RequestPreviewPayload.STREAM_CODEC,
                this::handleRequestPreview
        );

        registrar.playToServer(
                ExportSubmitPayload.TYPE,
                ExportSubmitPayload.STREAM_CODEC,
                this::handleExportSubmit
        );

        registrar.playToServer(
                ExportCancelPayload.TYPE,
                ExportCancelPayload.STREAM_CODEC,
                this::handleExportCancel
        );

        registrar.playToServer(
                DeleteTemplatePayload.TYPE,
                DeleteTemplatePayload.STREAM_CODEC,
                this::handleDeleteTemplate
        );

        registrar.playToServer(
                InstantStructureCommonConfigPushPayload.TYPE,
                InstantStructureCommonConfigPushPayload.STREAM_CODEC,
                this::handleCommonConfigPush
        );
    }

    private void handleCommonConfigPush(
            InstantStructureCommonConfigPushPayload payload,
            IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!player.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                player.sendSystemMessage(Component.translatable("instant_structure.screen.config.push_denied")
                        .withStyle(ChatFormatting.RED));
                return;
            }
            Config.ENABLE_CRAFTING_RECIPE.set(payload.enableCraftingRecipe());
            Config.ENABLE_CRAFTING_RECIPE.save();
            Config.ENABLE_MATERIAL_CONSUMPTION.set(payload.enableMaterialConsumption());
            Config.ENABLE_MATERIAL_CONSUMPTION.save();
            Config.DROP_CLEARED_BLOCKS.set(payload.dropClearedBlocks());
            Config.DROP_CLEARED_BLOCKS.save();
            LOGGER.info(
                    "Instant Structure common config pushed by {}: recipe={}, material={}, drop={}",
                    player.getGameProfile().name(),
                    Config.ENABLE_CRAFTING_RECIPE.get(),
                    Config.ENABLE_MATERIAL_CONSUMPTION.get(),
                    Config.DROP_CLEARED_BLOCKS.get()
            );
            player.sendSystemMessage(Component.translatable("instant_structure.screen.config.push_ok")
                    .withStyle(ChatFormatting.GREEN));
        });
    }

    private void handleExportSubmit(ExportSubmitPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Selection sel = SELECTIONS.get(player.getUUID());
            if (sel != null) {
                sel.setExportPending(false);
            }
            if (performExport(player, payload.name(), payload.category())) {
                SELECTIONS.remove(player.getUUID());
                syncSelection(player, new Selection(null, null));
            }
        });
    }

    private void handleExportCancel(ExportCancelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (payload.clearCompletely()) {
                    SELECTIONS.remove(player.getUUID());
                    syncSelection(player, new Selection(null, null));
                    player.sendSystemMessage(Component.translatable("instant_structure.message.undo_cleared").withStyle(ChatFormatting.YELLOW));
                } else {
                    handleMarkerUndo(player);
                }
            }
        });
    }

    private void handleDeleteTemplate(DeleteTemplatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            boolean isOp = player.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
            if (!isOp) {
                player.sendSystemMessage(Component.translatable("instant_structure.message.delete_no_permission").withStyle(ChatFormatting.RED));
                return;
            }
            if (!isValidCategory(payload.category())) {
                player.sendSystemMessage(Component.translatable("instant_structure.message.invalid_category").withStyle(ChatFormatting.RED));
                return;
            }
            if (!isValidExportName(payload.templateName())) {
                return;
            }
            Path configDir = FMLPaths.CONFIGDIR.get().resolve("instant-structure");
            Path nbtPath = StructureTemplateHelper.resolveTemplatePath(configDir, payload.category(), payload.templateName());
            Path jsonPath = configDir.resolve("templates-structure").resolve(payload.category()).resolve(payload.templateName() + ".json");
            try {
                if (Files.exists(nbtPath)) {
                    Files.delete(nbtPath);
                }
                if (Files.exists(jsonPath)) {
                    Files.delete(jsonPath);
                }
                player.sendSystemMessage(Component.translatable("instant_structure.message.deleted_template", payload.templateName()).withStyle(ChatFormatting.YELLOW));
                sendTemplatesToClient(player);
            } catch (Exception e) {
                player.sendSystemMessage(Component.translatable("instant_structure.message.failed_delete_template").withStyle(ChatFormatting.RED));
                LOGGER.error("Failed to delete template {}", payload.templateName(), e);
            }
        });
    }

    private void handleRequestPreview(RequestPreviewPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            sendPreviewToClient(player, payload.category(), payload.templateName());
        });
    }

    public static void sendPreviewToClient(ServerPlayer player, String category, String templateName) {
        if (!VALID_CATEGORIES.contains(category.toLowerCase())) {
            return;
        }
        if (!isValidExportName(templateName)) {
            return;
        }
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("instant-structure");
        Path nbtPath = StructureTemplateHelper.resolveTemplatePath(configDir, category, templateName);
        if (!Files.exists(nbtPath)) {
            return;
        }
        try {
            List<PreviewBlockEntry> blocks = StructureTemplateHelper.extractSolidBlocks(player.level(), nbtPath);
            PacketDistributor.sendToPlayer(player, new TemplatePreviewPayload(category, templateName, blocks));
        } catch (Exception e) {
            LOGGER.error("Failed to send template preview for {}", templateName, e);
        }
    }

    private void handleAdjustHeight(AdjustHeightPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            Selection sel = SELECTIONS.get(player.getUUID());
            if (sel == null || !sel.hasStart()) {
                return;
            }
            if (sel.hasBoth()) {
                sel.adjustCeiling(payload.delta());
            } else {
                sel.adjustStartY(payload.delta());
            }
            syncSelection(player, sel);
        });
    }

    private void handleBuildRequest(BuildRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!isValidCategory(payload.category())) {
                player.sendSystemMessage(Component.translatable("instant_structure.message.invalid_category").withStyle(ChatFormatting.RED));
                sendBuildResult(player, BuildResultPayload.FAILED);
                return;
            }
            if (!isValidExportName(payload.templateName())) {
                player.sendSystemMessage(Component.translatable("instant_structure.message.invalid_template_name").withStyle(ChatFormatting.RED));
                sendBuildResult(player, BuildResultPayload.FAILED);
                return;
            }
            Path configDir = FMLPaths.CONFIGDIR.get().resolve("instant-structure");
            Path nbtPath = StructureTemplateHelper.resolveTemplatePath(configDir, payload.category(), payload.templateName());
            if (!Files.exists(nbtPath)) {
                player.sendSystemMessage(Component.translatable("instant_structure.message.template_not_found", payload.templateName()).withStyle(ChatFormatting.RED));
                sendBuildResult(player, BuildResultPayload.FAILED);
                return;
            }

            int blockCount = 0;
            try {
                StructureTemplateHelper.loadTemplate((ServerLevel) player.level(), nbtPath);
                blockCount = StructureTemplateHelper.collectSolidBlockInfos((ServerLevel) player.level(), nbtPath).size();
            } catch (Exception e) {
                player.sendSystemMessage(Component.translatable("instant_structure.message.failed_load_template").withStyle(ChatFormatting.RED));
                sendBuildResult(player, BuildResultPayload.FAILED);
                return;
            }

            BlockPos targetPos = new BlockPos(payload.x(), payload.y(), payload.z());
            PlacementTransform transform = payload.placementTransform();
            try {
                StructureTemplate template = StructureTemplateHelper.loadTemplate((ServerLevel) player.level(), nbtPath);
                List<StructureTemplate.StructureBlockInfo> blockInfos =
                        StructureTemplateHelper.collectSolidBlockInfos((ServerLevel) player.level(), nbtPath);
                PlacementBounds bounds = PlacementBounds.fromBlockInfos(targetPos, transform, blockInfos);
                if (bounds == null) {
                    bounds = PlacementBounds.from(template, targetPos, transform);
                }
                if (bounds.containsAnyPlayer((ServerLevel) player.level())) {
                    player.sendSystemMessage(Component.translatable("instant_structure.message.build_player_inside")
                            .withStyle(ChatFormatting.RED));
                    sendBuildResult(player, BuildResultPayload.PLAYER_INSIDE);
                    return;
                }

                // 素材消費チェック
                boolean consume = Config.ENABLE_MATERIAL_CONSUMPTION.get() && !player.isCreative() && !player.isSpectator();
                BlockPos anchorPos = payload.hasAnchor() ? new BlockPos(payload.anchorX(), payload.anchorY(), payload.anchorZ()) : null;
                if (consume) {
                    Map<net.minecraft.world.item.Item, Integer> required = new HashMap<>();
                    for (StructureTemplate.StructureBlockInfo info : blockInfos) {
                        net.minecraft.world.level.block.state.BlockState state = info.state();
                        
                        // 2マス占有ブロック（ベッド、ドア、2段の植物など）の重複カウントを防ぐ
                        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART) && 
                            state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART) == net.minecraft.world.level.block.state.properties.BedPart.FOOT) {
                            continue; // HEADのみカウント
                        }
                        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF) && 
                            state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) {
                            continue; // LOWERのみカウント
                        }
                        
                        net.minecraft.world.level.block.Block block = state.getBlock();
                        net.minecraft.world.item.Item item = block.asItem();
                        if (item != net.minecraft.world.item.Items.AIR) {
                            required.put(item, required.getOrDefault(item, 0) + 1);
                        }
                    }

                    Map<net.minecraft.world.item.Item, Integer> available = countAvailableMaterials(player, (ServerLevel) player.level(), anchorPos, payload.hasAnchor());
                    List<Component> materialStatusList = new ArrayList<>();
                    boolean hasMissing = false;
                    for (Map.Entry<net.minecraft.world.item.Item, Integer> entry : required.entrySet()) {
                        int avail = available.getOrDefault(entry.getKey(), 0);
                        int reqCount = entry.getValue();
                        Component itemName = Component.translatable(entry.getKey().getDescriptionId());
                        if (avail >= reqCount) {
                            materialStatusList.add(itemName.copy().append(" : " + avail + " / " + reqCount).withStyle(ChatFormatting.WHITE));
                        } else {
                            hasMissing = true;
                            materialStatusList.add(itemName.copy().append(" : " + avail + " / " + reqCount).withStyle(ChatFormatting.RED));
                        }
                    }

                    if (hasMissing) {
                        player.sendSystemMessage(Component.translatable("instant_structure.message.required_materials_header").withStyle(ChatFormatting.RED));
                        for (Component status : materialStatusList) {
                            player.sendSystemMessage(status);
                        }
                        sendBuildResult(player, BuildResultPayload.FAILED);
                        return;
                    }

                    // 消費実行
                    consumeMaterials(player, (ServerLevel) player.level(), anchorPos, payload.hasAnchor(), required);
                }
            } catch (Exception e) {
                player.sendSystemMessage(Component.translatable("instant_structure.message.failed_load_template").withStyle(ChatFormatting.RED));
                sendBuildResult(player, BuildResultPayload.FAILED);
                return;
            }

            boolean success = buildTemplateInternal(
                    (ServerLevel) player.level(),
                    payload.category(),
                    payload.templateName(),
                    targetPos,
                    transform,
                    player
            );
            if (success) {
                if (blockCount > SpreadBuildManager.SPREAD_THRESHOLD) {
                    player.sendSystemMessage(Component.translatable("instant_structure.message.spread_build_started", blockCount).withStyle(ChatFormatting.YELLOW));
                } else {
                    player.sendSystemMessage(Component.translatable("instant_structure.message.successfully_built", payload.templateName()).withStyle(ChatFormatting.GREEN));
                }
                sendBuildResult(player, BuildResultPayload.SUCCESS);
            } else {
                player.sendSystemMessage(Component.translatable("instant_structure.message.failed_build").withStyle(ChatFormatting.RED));
                sendBuildResult(player, BuildResultPayload.FAILED);
            }
        });
    }

    private static Map<net.minecraft.world.item.Item, Integer> countAvailableMaterials(
            ServerPlayer player, ServerLevel level, BlockPos anchorPos, boolean hasAnchor) {
        Map<net.minecraft.world.item.Item, Integer> available = new HashMap<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                available.put(stack.getItem(), available.getOrDefault(stack.getItem(), 0) + stack.getCount());
            }
        }
        if (hasAnchor && anchorPos != null) {
            List<BlockPos> checkPositions = new ArrayList<>();
            checkPositions.add(anchorPos);
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                checkPositions.add(anchorPos.relative(dir));
            }
            for (BlockPos pos : checkPositions) {
                net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof net.minecraft.world.Container container) {
                    for (int i = 0; i < container.getContainerSize(); i++) {
                        ItemStack stack = container.getItem(i);
                        if (!stack.isEmpty()) {
                            available.put(stack.getItem(), available.getOrDefault(stack.getItem(), 0) + stack.getCount());
                        }
                    }
                }
            }
        }
        return available;
    }

    private static void consumeMaterials(
            ServerPlayer player, ServerLevel level, BlockPos anchorPos, boolean hasAnchor,
            Map<net.minecraft.world.item.Item, Integer> required) {
        Map<net.minecraft.world.item.Item, Integer> toConsume = new HashMap<>(required);
        if (hasAnchor && anchorPos != null) {
            List<BlockPos> checkPositions = new ArrayList<>();
            checkPositions.add(anchorPos);
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                checkPositions.add(anchorPos.relative(dir));
            }
            for (BlockPos pos : checkPositions) {
                net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof net.minecraft.world.Container container) {
                    for (int i = 0; i < container.getContainerSize(); i++) {
                        ItemStack stack = container.getItem(i);
                        if (!stack.isEmpty()) {
                            net.minecraft.world.item.Item item = stack.getItem();
                            int needed = toConsume.getOrDefault(item, 0);
                            if (needed > 0) {
                                int toTake = Math.min(stack.getCount(), needed);
                                stack.shrink(toTake);
                                container.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
                                toConsume.put(item, needed - toTake);
                            }
                        }
                    }
                }
            }
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                net.minecraft.world.item.Item item = stack.getItem();
                int needed = toConsume.getOrDefault(item, 0);
                if (needed > 0) {
                    int toTake = Math.min(stack.getCount(), needed);
                    stack.shrink(toTake);
                    player.getInventory().setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
                    toConsume.put(item, needed - toTake);
                }
            }
        }
    }

    private static void sendBuildResult(ServerPlayer player, byte result) {
        PacketDistributor.sendToPlayer(player, new BuildResultPayload(result));
    }

    private void handleRequestTemplates(RequestTemplatesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                sendTemplatesToClient(player);
            }
        });
    }

    public static void sendTemplatesToClient(ServerPlayer player) {
        List<TemplateInfo> list = new ArrayList<>();
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("instant-structure").resolve("templates-structure");
        String[] categories = {"houses", "arenas", "custom"};
        for (String cat : categories) {
            Path catDir = configDir.resolve(cat);
            if (Files.exists(catDir)) {
                File[] files = catDir.toFile().listFiles((dir, name) -> name.endsWith(".nbt"));
                if (files != null) {
                    for (File f : files) {
                        String baseName = f.getName().substring(0, f.getName().length() - 4);
                        // Try reading description from json
                        String desc = "";
                        int sizeX = 1, sizeY = 1, sizeZ = 1;
                        try {
                            CompoundTag compoundTag = NbtIo.readCompressed(f.toPath(), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                            if (compoundTag.contains("size")) {
                                List<Integer> sizeList = compoundTag.read("size", com.mojang.serialization.Codec.INT.listOf()).orElse(List.of());
                                if (sizeList.size() == 3) {
                                    sizeX = sizeList.get(0);
                                    sizeY = sizeList.get(1);
                                    sizeZ = sizeList.get(2);
                                }
                            }
                        } catch (Exception ignored) {}

                        File jsonFile = new File(f.getParentFile(), baseName + ".json");
                        if (jsonFile.exists()) {
                            try (java.io.BufferedReader reader = Files.newBufferedReader(jsonFile.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
                                Map<?, ?> map = GSON.fromJson(reader, Map.class);
                                if (map != null && map.containsKey("description")) {
                                    desc = String.valueOf(map.get("description"));
                                }
                            } catch (Exception ignored) {}
                        }
                        list.add(new TemplateInfo(cat, baseName, desc, sizeX, sizeY, sizeZ));
                    }
                }
            }
        }
        PacketDistributor.sendToPlayer(player, new TemplatesListPayload(list));
    }

    public static void handleMarkerClick(Player player, BlockPos pos) {
        Selection sel = SELECTIONS.computeIfAbsent(player.getUUID(), k -> new Selection(null, null));

        if (sel.isExportPending()) {
            return;
        }

        if (!sel.hasStart()) {
            sel.setPos1(pos);
            player.sendSystemMessage(Component.translatable("instant_structure.message.point1_set", pos.toShortString()).withStyle(ChatFormatting.GREEN));
        } else if (!sel.hasBoth()) {
            sel.setPos2(pos);
            player.sendSystemMessage(Component.translatable("instant_structure.message.point2_set", pos.toShortString()).withStyle(ChatFormatting.GREEN));
            player.sendSystemMessage(Component.translatable("instant_structure.message.confirm_hint").withStyle(ChatFormatting.YELLOW));
        } else if (!sel.isConfirmed()) {
            sel.setConfirmed(true);
            sel.setExportPending(true);
            player.sendSystemMessage(Component.translatable("instant_structure.message.selection_confirmed").withStyle(ChatFormatting.GREEN));
            syncSelection((ServerPlayer) player, sel);
            sendExportDialog((ServerPlayer) player, sel);
            return;
        } else if (!sel.isExportPending()) {
            sel.setExportPending(true);
            sendExportDialog((ServerPlayer) player, sel);
            return;
        }

        syncSelection((ServerPlayer) player, sel);
    }

    public static void handleMarkerUndo(Player player) {
        Selection sel = SELECTIONS.get(player.getUUID());
        if (sel == null) {
            player.sendSystemMessage(Component.translatable("instant_structure.message.undo_nothing").withStyle(ChatFormatting.GRAY));
            return;
        }

        boolean wasConfirmed = sel.isConfirmed();
        boolean wasExportPending = sel.isExportPending();
        boolean hadBoth = sel.hasBoth();
        boolean hadStart = sel.hasStart();

        if (!sel.undoStep()) {
            player.sendSystemMessage(Component.translatable("instant_structure.message.undo_nothing").withStyle(ChatFormatting.GRAY));
            return;
        }

        if (wasExportPending || wasConfirmed) {
            player.sendSystemMessage(Component.translatable("instant_structure.message.undo_unconfirmed").withStyle(ChatFormatting.YELLOW));
        } else if (hadBoth) {
            player.sendSystemMessage(Component.translatable("instant_structure.message.undo_to_point1").withStyle(ChatFormatting.YELLOW));
        } else if (hadStart) {
            player.sendSystemMessage(Component.translatable("instant_structure.message.undo_cleared").withStyle(ChatFormatting.YELLOW));
        }

        syncSelection((ServerPlayer) player, sel);
    }

    private static void sendExportDialog(ServerPlayer player, Selection sel) {
        if (!sel.hasBoth() || sel.getPos1() == null || sel.getPos2() == null) {
            return;
        }
        BlockPos pos1 = sel.getPos1();
        BlockPos pos2 = sel.getPos2();
        PacketDistributor.sendToPlayer(player, new OpenExportDialogPayload(
                pos1.getX(), pos1.getY(), pos1.getZ(),
                pos2.getX(), pos2.getY(), pos2.getZ()
        ));
    }

    private static void syncSelection(ServerPlayer player, Selection sel) {
        if (!sel.hasStart() || sel.getPos1() == null) {
            PacketDistributor.sendToPlayer(player, new SelectionSyncPayload(
                    false, false, false,
                    0, 0, 0,
                    0, 0, 0
            ));
            return;
        }

        BlockPos pos1 = sel.getPos1();
        if (sel.hasBoth() && sel.getPos2() != null) {
            BlockPos pos2 = sel.getPos2();
            PacketDistributor.sendToPlayer(player, new SelectionSyncPayload(
                    true,
                    true,
                    sel.isConfirmed(),
                    pos1.getX(), pos1.getY(), pos1.getZ(),
                    pos2.getX(), pos2.getY(), pos2.getZ()
            ));
        } else {
            PacketDistributor.sendToPlayer(player, new SelectionSyncPayload(
                    true,
                    false,
                    false,
                    pos1.getX(), pos1.getY(), pos1.getZ(),
                    0, 0, 0
            ));
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("instant-structure")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.literal("export")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .then(Commands.argument("category", StringArgumentType.string())
                                                .suggests(CATEGORY_SUGGESTIONS)
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    String name = StringArgumentType.getString(context, "name");
                                                    String category = StringArgumentType.getString(context, "category");
                                                    return exportStructure(player, name, category);
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal("expand")
                                .then(Commands.argument("direction", StringArgumentType.string())
                                        .suggests(DIRECTION_SUGGESTIONS)
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    String direction = StringArgumentType.getString(context, "direction");
                                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                                    return expandSelection(player, direction, amount);
                                                })
                                        )
                                )
                        )
        );
    }

    private int exportStructure(ServerPlayer player, String name, String category) {
        return performExport(player, name, category) ? 1 : 0;
    }

    private boolean performExport(ServerPlayer player, String name, String category) {
        if (!isValidCategory(category)) {
            player.sendSystemMessage(Component.translatable("instant_structure.message.invalid_category").withStyle(ChatFormatting.RED));
            return false;
        }

        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty() || !isValidExportName(trimmedName)) {
            player.sendSystemMessage(Component.translatable("instant_structure.message.invalid_export_name").withStyle(ChatFormatting.RED));
            return false;
        }

        Selection sel = SELECTIONS.get(player.getUUID());
        if (sel == null || !sel.isConfirmed()) {
            player.sendSystemMessage(Component.translatable("instant_structure.message.export_not_confirmed").withStyle(ChatFormatting.RED));
            return false;
        }

        try {
            Path exportsDir = InstantStructurePaths.configRoot().resolve("templates-structure").resolve(category);
            Files.createDirectories(exportsDir);
            Path nbtPath = exportsDir.resolve(trimmedName + ".nbt");
            Path jsonPath = exportsDir.resolve(trimmedName + ".json");

            BlockPos pos1 = sel.getPos1();
            BlockPos pos2 = sel.getPos2();
            BlockPos min = new BlockPos(
                    Math.min(pos1.getX(), pos2.getX()),
                    Math.min(pos1.getY(), pos2.getY()),
                    Math.min(pos1.getZ(), pos2.getZ())
            );
            BlockPos max = new BlockPos(
                    Math.max(pos1.getX(), pos2.getX()),
                    Math.max(pos1.getY(), pos2.getY()),
                    Math.max(pos1.getZ(), pos2.getZ())
            );
            Vec3i size = new Vec3i(
                    max.getX() - min.getX() + 1,
                    max.getY() - min.getY() + 1,
                    max.getZ() - min.getZ() + 1
            );

            StructureTemplate template = new StructureTemplate();
            template.fillFromWorld((ServerLevel) player.level(), min, size, true, java.util.List.of(net.minecraft.world.level.block.Blocks.AIR));

            CompoundTag tag = template.save(new CompoundTag());
            NbtIo.writeCompressed(tag, nbtPath);

            Map<String, BlockPos> detectedHousePositions = Map.of();
            if ("houses".equals(category)) {
                try {
                    List<StructureTemplate.StructureBlockInfo> blockInfos =
                            StructureTemplateHelper.collectSolidBlockInfos((ServerLevel) player.level(), nbtPath);
                    detectedHousePositions = WerewolfHouseMetadata.detect(blockInfos).orElse(Map.of());
                } catch (Exception e) {
                    LOGGER.warn("Failed to auto-detect werewolf house metadata for {}", trimmedName, e);
                }
            }

            // Write metadata JSON
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("name", trimmedName);
            meta.put("description", "Exported from category: " + category);
            meta.put("author", player.getName().getString());
            Map<String, Integer> offset = new LinkedHashMap<>();
            offset.put("x", 0);
            offset.put("y", 0);
            offset.put("z", 0);
            meta.put("offset", offset);
            if (!detectedHousePositions.isEmpty()) {
                meta.put("special_positions", toJsonPositionMap(detectedHousePositions));
                Map<String, Object> integration = new LinkedHashMap<>();
                integration.put("werewolf_house", true);
                integration.put("auto_detected", true);
                meta.put("game_integration", integration);
            }

            try (java.io.BufferedWriter writer = Files.newBufferedWriter(jsonPath, java.nio.charset.StandardCharsets.UTF_8)) {
                GSON.toJson(meta, writer);
            }

            player.sendSystemMessage(Component.translatable("instant_structure.message.export_success", trimmedName).withStyle(ChatFormatting.GREEN));
            if ("houses".equals(category)) {
                if (WerewolfHouseMetadata.isComplete(detectedHousePositions)) {
                    player.sendSystemMessage(Component.translatable(
                            "instant_structure.message.export_special_positions_ready",
                            formatBlockPos(detectedHousePositions.get(WerewolfHouseMetadata.KEY_DOOR))
                    ).withStyle(ChatFormatting.AQUA));
                } else {
                    player.sendSystemMessage(Component.translatable("instant_structure.message.export_special_positions_no_door")
                            .withStyle(ChatFormatting.YELLOW));
                }
            }
            return true;
        } catch (Exception e) {
            player.sendSystemMessage(Component.translatable("instant_structure.message.export_failed").withStyle(ChatFormatting.RED));
            LOGGER.error("Export failed", e);
            return false;
        }
    }

    private static boolean isValidExportName(String name) {
        return name != null && !name.trim().isEmpty() && !name.contains("/") && !name.contains("\\") && !name.contains(":") && !name.contains("*") && !name.contains("?") && !name.contains("\"") && !name.contains("<") && !name.contains(">") && !name.contains("|");
    }

    private int expandSelection(ServerPlayer player, String direction, int amount) {
        if (!VALID_DIRECTIONS.contains(direction.toLowerCase())) {
            player.sendSystemMessage(Component.translatable("instant_structure.message.invalid_direction").withStyle(ChatFormatting.RED));
            return 0;
        }

        Selection sel = SELECTIONS.get(player.getUUID());
        if (sel == null || !sel.hasBoth()) {
            player.sendSystemMessage(Component.translatable("instant_structure.message.no_selection_to_expand").withStyle(ChatFormatting.RED));
            return 0;
        }
        sel.expand(direction, amount);
        syncSelection(player, sel);
        player.sendSystemMessage(Component.translatable("instant_structure.message.expanded_selection", amount, direction).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    public static boolean buildTemplateInternal(ServerLevel level, String category, String templateName, BlockPos pos, int rotationDegrees) {
        return buildTemplateInternal(level, category, templateName, pos, new PlacementTransform(rotationDegrees, false, false), null);
    }

    public static boolean buildTemplateInternal(
            ServerLevel level,
            String category,
            String templateName,
            BlockPos pos,
            int rotationDegrees,
            net.minecraft.world.level.block.Mirror mirror
    ) {
        boolean flipLeftRight = mirror == net.minecraft.world.level.block.Mirror.LEFT_RIGHT;
        boolean flipFrontBack = mirror == net.minecraft.world.level.block.Mirror.FRONT_BACK;
        return buildTemplateInternal(level, category, templateName, pos, new PlacementTransform(rotationDegrees, flipLeftRight, flipFrontBack), null);
    }

    public static boolean buildTemplateInternal(
            ServerLevel level,
            String category,
            String templateName,
            BlockPos pos,
            PlacementTransform transform,
            ServerPlayer player
    ) {
        if (!isValidCategory(category)) {
            return false;
        }
        try {
            Path configDir = FMLPaths.CONFIGDIR.get().resolve("instant-structure");
            Path nbtPath = StructureTemplateHelper.resolveTemplatePath(configDir, category, templateName);
            if (!Files.exists(nbtPath)) {
                LOGGER.error("NBT template file not found: {}", nbtPath);
                return false;
            }

            StructureTemplate template = StructureTemplateHelper.loadTemplate(level, nbtPath);
            List<StructureTemplate.StructureBlockInfo> blockInfos = StructureTemplateHelper.collectSolidBlockInfos(level, nbtPath);
            PlacementBounds bounds = PlacementBounds.fromBlockInfos(pos, transform, blockInfos);
            if (bounds == null) {
                bounds = PlacementBounds.from(template, pos, transform);
            }
            if (bounds.containsAnyPlayer(level)) {
                return false;
            }

            clearPlacementArea(level, bounds, player);

            if (SpreadBuildManager.queueIfLarge(level, nbtPath, template, pos, transform, player)) {
                int blockCount = StructureTemplateHelper.collectSolidBlockInfos(level, nbtPath).size();
                LOGGER.info("Queued spread build for {} ({} blocks)", templateName, blockCount);
                return true;
            }

            if (transform.usesManualPlacement()) {
                for (StructureTemplate.StructureBlockInfo info : StructureTemplateHelper.collectSolidBlockInfos(level, nbtPath)) {
                BlockPos worldPos = pos.offset(transform.toWorldRelative(info.pos()));
                    level.setBlock(worldPos, transform.transformBlockState(info.state()), 3);
                }
            } else {
                StructurePlaceSettings settings = transform.toPlaceSettings();
                template.placeInWorld(level, pos, pos, settings, level.getRandom(), 3);
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to build NBT template: " + templateName, e);
            return false;
        }
    }

    private static void clearPlacementArea(ServerLevel level, PlacementBounds bounds, ServerPlayer player) {
        boolean drop = Config.DROP_CLEARED_BLOCKS.get() && player != null && !player.isCreative() && !player.isSpectator();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (drop) {
                        level.destroyBlock(pos, true, player);
                    } else {
                        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    public static boolean isValidCategory(String category) {
        return category != null && VALID_CATEGORIES.contains(category.toLowerCase());
    }

    public static Optional<net.minecraft.core.Vec3i> getTemplateSize(ServerLevel level, String category, String templateName) {
        if (level == null || !isValidCategory(category) || !isValidExportName(templateName)) {
            return Optional.empty();
        }
        try {
            Path configDir = FMLPaths.CONFIGDIR.get().resolve("instant-structure");
            Path nbtPath = StructureTemplateHelper.resolveTemplatePath(configDir, category, templateName);
            if (!Files.exists(nbtPath)) {
                return Optional.empty();
            }
            StructureTemplate template = StructureTemplateHelper.loadTemplate(level, nbtPath);
            return Optional.of(template.getSize());
        } catch (Exception e) {
            LOGGER.error("Failed to read template size for {}", templateName, e);
            return Optional.empty();
        }
    }

    public static Optional<TemplateMetadata> readTemplateMetadata(String category, String templateName) {
        if (!isValidCategory(category)) {
            return Optional.empty();
        }
        if (!isValidExportName(templateName)) {
            return Optional.empty();
        }
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("instant-structure");
        Path jsonPath = configDir.resolve("templates-structure").resolve(category).resolve(templateName + ".json");
        if (!Files.exists(jsonPath)) {
            return Optional.empty();
        }
        try (java.io.BufferedReader reader = Files.newBufferedReader(jsonPath, java.nio.charset.StandardCharsets.UTF_8)) {
            Map<?, ?> map = GSON.fromJson(reader, Map.class);
            if (map == null) {
                return Optional.empty();
            }
            String name = map.containsKey("name") ? String.valueOf(map.get("name")) : templateName;
            String description = map.containsKey("description") ? String.valueOf(map.get("description")) : "";
            String author = map.containsKey("author") ? String.valueOf(map.get("author")) : "";
            BlockPos offset = parseOffset(map.get("offset"));
            Map<String, BlockPos> specialPositions = parseSpecialPositions(map.get("special_positions"));
            if (specialPositions.isEmpty()) {
                specialPositions = loadBuiltinSpecialPositions(category, templateName);
            }
            if ("houses".equals(category) && !WerewolfHouseMetadata.isComplete(specialPositions)) {
                specialPositions = detectHousePositionsFromNbt(category, templateName).orElse(specialPositions);
            }
            return Optional.of(new TemplateMetadata(name, description, author, offset, specialPositions));
        } catch (Exception e) {
            LOGGER.error("Failed to read template metadata for {}", templateName, e);
            return Optional.empty();
        }
    }

    private static Optional<Map<String, BlockPos>> detectHousePositionsFromNbt(String category, String templateName) {
        if (!"houses".equals(category) || !isValidExportName(templateName)) {
            return Optional.empty();
        }
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return Optional.empty();
        }
        try {
            Path configDir = FMLPaths.CONFIGDIR.get().resolve("instant-structure");
            Path nbtPath = StructureTemplateHelper.resolveTemplatePath(configDir, category, templateName);
            if (!Files.exists(nbtPath)) {
                return Optional.empty();
            }
            List<StructureTemplate.StructureBlockInfo> blockInfos =
                    StructureTemplateHelper.collectSolidBlockInfos(server.overworld(), nbtPath);
            return WerewolfHouseMetadata.detect(blockInfos);
        } catch (Exception e) {
            LOGGER.warn("Failed to detect house metadata from NBT for {}/{}", category, templateName, e);
            return Optional.empty();
        }
    }

    private static Map<String, BlockPos> loadBuiltinSpecialPositions(String category, String templateName) {
        String resourcePath = "/assets/instant_structure/templates/" + category + "/" + templateName + ".json";
        try (java.io.InputStream in = InstantStructureMod.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return Map.of();
            }
            Map<?, ?> map = GSON.fromJson(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8), Map.class);
            if (map == null) {
                return Map.of();
            }
            Map<String, BlockPos> positions = parseSpecialPositions(map.get("special_positions"));
            if (!positions.isEmpty()) {
                LOGGER.info("Using bundled special_positions for {}/{} (config JSON had none)", category, templateName);
            }
            return positions;
        } catch (Exception e) {
            LOGGER.warn("Failed to load bundled template metadata for {}/{}", category, templateName, e);
            return Map.of();
        }
    }

    private static BlockPos parseOffset(Object rawOffset) {
        if (rawOffset instanceof Map<?, ?> offsetMap) {
            return new BlockPos(
                    toInt(offsetMap.get("x")),
                    toInt(offsetMap.get("y")),
                    toInt(offsetMap.get("z"))
            );
        }
        return BlockPos.ZERO;
    }

    private static Map<String, BlockPos> parseSpecialPositions(Object rawPositions) {
        if (!(rawPositions instanceof Map<?, ?> positionsMap)) {
            return Map.of();
        }
        Map<String, BlockPos> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : positionsMap.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> posMap) {
                result.put(String.valueOf(entry.getKey()), new BlockPos(
                        toInt(posMap.get("x")),
                        toInt(posMap.get("y")),
                        toInt(posMap.get("z"))
                ));
            }
        }
        return result;
    }

    public static List<String> listTemplateNames(String category) {
        if (!isValidCategory(category)) {
            return List.of();
        }
        Path categoryDir = FMLPaths.CONFIGDIR.get().resolve("instant-structure")
                .resolve("templates-structure")
                .resolve(category);
        if (!Files.isDirectory(categoryDir)) {
            return List.of();
        }
        File[] files = categoryDir.toFile().listFiles((dir, name) -> name.endsWith(".nbt"));
        if (files == null || files.length == 0) {
            return List.of();
        }
        List<String> names = new ArrayList<>(files.length);
        for (File file : files) {
            String baseName = file.getName().substring(0, file.getName().length() - 4);
            if (isValidExportName(baseName)) {
                names.add(baseName);
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private static Map<String, Map<String, Integer>> toJsonPositionMap(Map<String, BlockPos> positions) {
        Map<String, Map<String, Integer>> result = new LinkedHashMap<>();
        for (Map.Entry<String, BlockPos> entry : positions.entrySet()) {
            BlockPos pos = entry.getValue();
            Map<String, Integer> coords = new LinkedHashMap<>();
            coords.put("x", pos.getX());
            coords.put("y", pos.getY());
            coords.put("z", pos.getZ());
            result.put(entry.getKey(), coords);
        }
        return result;
    }

    private static String formatBlockPos(BlockPos pos) {
        return pos == null ? "?" : pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
