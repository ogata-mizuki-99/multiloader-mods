package com.ogatamizuki.instantstructure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class InstantStructureServerOps {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> VALID_CATEGORIES = Set.of("houses", "arenas", "custom");
    private static final Map<UUID, Selection> SELECTIONS = new ConcurrentHashMap<>();

    private InstantStructureServerOps() {}

    public static void initDirectories() {
        try {
            Path root = InstantStructurePlatform.getConfigDir().resolve("instant-structure");
            Path housesDir = root.resolve("templates-structure/houses");
            Path arenasDir = root.resolve("templates-structure/arenas");
            Files.createDirectories(housesDir);
            Files.createDirectories(arenasDir);
            Files.createDirectories(root.resolve("templates-structure/custom"));
            InstantStructureCommon.LOGGER.info("Successfully created instant-structure directories");

            copyResourceIfNeeded("/assets/instant_structure/templates/arenas/village.json", arenasDir.resolve("village.json"));
            copyResourceIfNeeded("/assets/instant_structure/templates/arenas/village.nbt", arenasDir.resolve("village.nbt"));
            copyResourceIfNeeded("/assets/instant_structure/templates/houses/house.json", housesDir.resolve("house.json"));
            copyResourceIfNeeded("/assets/instant_structure/templates/houses/house.nbt", housesDir.resolve("house.nbt"));
        } catch (Exception e) {
            InstantStructureCommon.LOGGER.error("Failed to create directories", e);
        }
    }

    private static void copyResourceIfNeeded(String resourcePath, Path destPath) {
        if (Files.exists(destPath)) {
            return;
        }
        try (java.io.InputStream in = InstantStructureServerOps.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                InstantStructureCommon.LOGGER.warn("Initial template resource not found: " + resourcePath);
                return;
            }
            Files.copy(in, destPath);
            InstantStructureCommon.LOGGER.info("Successfully extracted initial template: " + destPath.getFileName());
        } catch (Exception e) {
            InstantStructureCommon.LOGGER.error("Failed to extract initial template resource " + resourcePath + " to " + destPath, e);
        }
    }

    public static Selection getSelection(UUID uuid) {
        return SELECTIONS.get(uuid);
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

        if (!sel.undoStep()) {
            player.sendSystemMessage(Component.translatable("instant_structure.message.undo_nothing").withStyle(ChatFormatting.GRAY));
            return;
        }

        if (wasExportPending || wasConfirmed) {
            player.sendSystemMessage(Component.translatable("instant_structure.message.undo_unconfirmed").withStyle(ChatFormatting.YELLOW));
        } else if (hadBoth) {
            player.sendSystemMessage(Component.translatable("instant_structure.message.undo_point2").withStyle(ChatFormatting.YELLOW));
        } else {
            player.sendSystemMessage(Component.translatable("instant_structure.message.undo_point1").withStyle(ChatFormatting.YELLOW));
        }

        syncSelection((ServerPlayer) player, sel);
    }

    public static void handleAdjustHeight(ServerPlayer player, int delta) {
        Selection sel = SELECTIONS.get(player.getUUID());
        if (sel == null || !sel.hasStart()) {
            return;
        }
        if (sel.hasBoth()) {
            sel.adjustCeiling(delta);
        } else {
            sel.adjustStartY(delta);
        }
        syncSelection(player, sel);
    }

    public static void handleExportSubmit(ServerPlayer player, String name, String category) {
        Selection sel = SELECTIONS.get(player.getUUID());
        if (sel != null) {
            sel.setExportPending(false);
        }
        if (performExport(player, name, category)) {
            SELECTIONS.remove(player.getUUID());
            syncSelection(player, new Selection(null, null));
        }
    }

    public static void handleExportCancel(ServerPlayer player, boolean clearCompletely) {
        if (clearCompletely) {
            SELECTIONS.remove(player.getUUID());
            syncSelection(player, new Selection(null, null));
            player.sendSystemMessage(Component.translatable("instant_structure.message.undo_cleared").withStyle(ChatFormatting.YELLOW));
        } else {
            handleMarkerUndo(player);
        }
    }

    public static boolean performExport(ServerPlayer player, String name, String category) {
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
            Path configDir = InstantStructurePlatform.getConfigDir().resolve("instant-structure");
            Path exportsDir = configDir.resolve("templates-structure").resolve(category);
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
                    InstantStructureCommon.LOGGER.warn("Failed to auto-detect werewolf house metadata for {}", trimmedName, e);
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
            InstantStructureCommon.LOGGER.error("Export failed", e);
            return false;
        }
    }

    private static Map<String, Object> toJsonPositionMap(Map<String, BlockPos> positions) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, BlockPos> entry : positions.entrySet()) {
            BlockPos pos = entry.getValue();
            Map<String, Integer> coords = new LinkedHashMap<>();
            coords.put("x", pos.getX());
            coords.put("y", pos.getY());
            coords.put("z", pos.getZ());
            map.put(entry.getKey(), coords);
        }
        return map;
    }

    private static String formatBlockPos(BlockPos pos) {
        if (pos == null) {
            return "null";
        }
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    public static boolean isValidCategory(String category) {
        return category != null && VALID_CATEGORIES.contains(category.toLowerCase());
    }

    public static boolean isValidExportName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return name.matches("^[a-zA-Z0-9_-]+$");
    }

    public static void handleDeleteTemplate(ServerPlayer player, String category, String templateName) {
        if (!isValidCategory(category)) {
            player.sendSystemMessage(Component.translatable("instant_structure.message.invalid_category").withStyle(ChatFormatting.RED));
            return;
        }
        if (!isValidExportName(templateName)) {
            return;
        }
        Path configDir = InstantStructurePlatform.getConfigDir().resolve("instant-structure");
        Path nbtPath = StructureTemplateHelper.resolveTemplatePath(configDir, category, templateName);
        Path jsonPath = configDir.resolve("templates-structure").resolve(category).resolve(templateName + ".json");
        try {
            if (Files.exists(nbtPath)) {
                Files.delete(nbtPath);
            }
            if (Files.exists(jsonPath)) {
                Files.delete(jsonPath);
            }
            player.sendSystemMessage(Component.translatable("instant_structure.message.deleted_template", templateName).withStyle(ChatFormatting.YELLOW));
            sendTemplatesToClient(player);
        } catch (Exception e) {
            player.sendSystemMessage(Component.translatable("instant_structure.message.failed_delete_template").withStyle(ChatFormatting.RED));
            InstantStructureCommon.LOGGER.error("Failed to delete template {}", templateName, e);
        }
    }

    public static void sendPreviewToClient(ServerPlayer player, String category, String templateName) {
        if (!VALID_CATEGORIES.contains(category.toLowerCase())) {
            return;
        }
        if (!isValidExportName(templateName)) {
            return;
        }
        Path configDir = InstantStructurePlatform.getConfigDir().resolve("instant-structure");
        Path nbtPath = StructureTemplateHelper.resolveTemplatePath(configDir, category, templateName);
        if (!Files.exists(nbtPath)) {
            return;
        }
        try {
            List<PreviewBlockEntry> blocks = StructureTemplateHelper.extractSolidBlocks(player.level(), nbtPath);
            InstantStructurePlatform.send(player, new TemplatePreviewPayload(category, templateName, blocks));
        } catch (Exception e) {
            InstantStructureCommon.LOGGER.error("Failed to extract solid blocks for preview from {}", templateName, e);
        }
    }

    public static void syncSelection(ServerPlayer player, Selection sel) {
        if (sel.hasStart()) {
            BlockPos p1 = sel.getPos1();
            BlockPos p2 = sel.hasBoth() ? sel.getPos2() : BlockPos.ZERO;
            InstantStructurePlatform.send(player, new SelectionSyncPayload(
                    true, sel.hasBoth(), sel.isConfirmed(),
                    p1.getX(), p1.getY(), p1.getZ(),
                    p2.getX(), p2.getY(), p2.getZ()
            ));
        } else {
            InstantStructurePlatform.send(player, new SelectionSyncPayload(
                    false, false, false, 0, 0, 0, 0, 0, 0
            ));
        }
    }

    private static void sendExportDialog(ServerPlayer player, Selection sel) {
        if (!sel.hasBoth()) return;
        BlockPos p1 = sel.getPos1();
        BlockPos p2 = sel.getPos2();
        InstantStructurePlatform.send(player, new OpenExportDialogPayload(
                p1.getX(), p1.getY(), p1.getZ(),
                p2.getX(), p2.getY(), p2.getZ()
        ));
    }

    public static void sendTemplatesToClient(ServerPlayer player) {
        List<TemplateInfo> list = new ArrayList<>();
        Path configDir = InstantStructurePlatform.getConfigDir().resolve("instant-structure").resolve("templates-structure");
        String[] categories = {"houses", "arenas", "custom"};
        for (String cat : categories) {
            Path catDir = configDir.resolve(cat);
            if (Files.exists(catDir)) {
                File[] files = catDir.toFile().listFiles((dir, name) -> name.endsWith(".nbt"));
                if (files != null) {
                    for (File f : files) {
                        String baseName = f.getName().substring(0, f.getName().length() - 4);
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
        InstantStructurePlatform.send(player, new TemplatesListPayload(list));
    }

    public static boolean buildTemplateInternal(
            ServerLevel level,
            String category,
            String templateName,
            BlockPos targetPos,
            PlacementTransform transform,
            Player player) {
        Path configDir = InstantStructurePlatform.getConfigDir().resolve("instant-structure");
        Path nbtPath = StructureTemplateHelper.resolveTemplatePath(configDir, category, templateName);
        if (!Files.exists(nbtPath)) {
            return false;
        }

        try {
            StructureTemplate template = StructureTemplateHelper.loadTemplate(level, nbtPath);
            if (player instanceof ServerPlayer serverPlayer) {
                if (SpreadBuildManager.queueIfLarge(level, nbtPath, template, targetPos, transform, serverPlayer)) {
                    return true;
                }
            }

            StructurePlaceSettings settings = transform.toPlaceSettings();

            BlockPos originPos = targetPos.offset(transform.toWorldRelative(BlockPos.ZERO));
            template.placeInWorld(level, targetPos, targetPos, settings, level.getRandom(), 2);
            return true;
        } catch (Exception e) {
            InstantStructureCommon.LOGGER.error("Failed to build template {}", templateName, e);
            return false;
        }
    }

    public static boolean buildTemplateInternal(
            ServerLevel level,
            String category,
            String templateName,
            BlockPos targetPos,
            int rotationDegrees) {
        PlacementTransform transform = new PlacementTransform(rotationDegrees, false, false);
        return buildTemplateInternal(level, category, templateName, targetPos, transform, null);
    }

    public static TemplateMetadata readTemplateMetadata(String category, String templateName) {
        Path configDir = InstantStructurePlatform.getConfigDir().resolve("instant-structure");
        Path jsonPath = configDir.resolve("templates-structure").resolve(category).resolve(templateName + ".json");
        if (!Files.exists(jsonPath)) {
            return null;
        }
        try (java.io.BufferedReader reader = Files.newBufferedReader(jsonPath, java.nio.charset.StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, TemplateMetadata.class);
        } catch (Exception e) {
            InstantStructureCommon.LOGGER.error("Failed to read template metadata for " + templateName, e);
            return null;
        }
    }

    public static Vec3i getTemplateSize(ServerLevel level, String category, String templateName) {
        Path configDir = InstantStructurePlatform.getConfigDir().resolve("instant-structure");
        Path nbtPath = StructureTemplateHelper.resolveTemplatePath(configDir, category, templateName);
        if (!Files.exists(nbtPath)) {
            return null;
        }
        try {
            StructureTemplate template = StructureTemplateHelper.loadTemplate(level, nbtPath);
            return template.getSize();
        } catch (Exception e) {
            InstantStructureCommon.LOGGER.error("Failed to get template size for " + templateName, e);
            return null;
        }
    }

    public static void sendBuildResult(ServerPlayer player, byte result) {
        InstantStructurePlatform.send(player, new BuildResultPayload(result));
    }

    public static void handleBuildRequest(ServerPlayer player, BuildRequestPayload payload) {
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
        Path configDir = InstantStructurePlatform.getConfigDir().resolve("instant-structure");
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
            boolean consume = InstantStructureConfig.enableMaterialConsumption && !player.isCreative() && !player.isSpectator();
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
                                int take = Math.min(stack.getCount(), needed);
                                stack.shrink(take);
                                toConsume.put(item, needed - take);
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
                    int take = Math.min(stack.getCount(), needed);
                    stack.shrink(take);
                    toConsume.put(item, needed - take);
                }
            }
        }
    }

    public static List<String> listTemplateNames(String category) {
        List<String> list = new ArrayList<>();
        Path configDir = InstantStructurePlatform.getConfigDir().resolve("instant-structure").resolve("templates-structure");
        Path catDir = configDir.resolve(category);
        if (Files.exists(catDir)) {
            File[] files = catDir.toFile().listFiles((dir, name) -> name.endsWith(".nbt"));
            if (files != null) {
                for (File f : files) {
                    list.add(f.getName().substring(0, f.getName().length() - 4));
                }
            }
        }
        return list;
    }
}
