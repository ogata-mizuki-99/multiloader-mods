package com.ogatamizuki.instantstructure.client;

import com.ogatamizuki.instantstructure.RequestPreviewPayload;
import com.ogatamizuki.instantstructure.TemplateInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.ogatamizuki.instantstructure.DeleteTemplatePayload;
import com.ogatamizuki.instantstructure.InstantStructurePaths;

public class InstantBuilderScreen extends Screen {
    private static final Set<String> VALID_CATEGORIES = Set.of("houses", "arenas", "custom");
    private static final int TAB_WIDTH = 80;
    private static final int TAB_HEIGHT = 20;
    private static final int ROW_HEIGHT = 22;
    private static final int VISIBLE_ROWS = 4;
    private static final int LIST_WIDTH = 240;
    private static final int LIST_TOP_MIN = 66;
    private static final int FOOTER_BUTTON_HEIGHT = 20;
    private static final int FOOTER_BUTTON_GAP = 10;
    private static final int FOOTER_BOTTOM_MARGIN = 10;
    private static final int FLIP_BUTTON_WIDTH = 78;
    private static final int SELECT_BUTTON_WIDTH = 92;
    private static final int CANCEL_BUTTON_WIDTH = 72;

    private final List<TemplateInfo> allTemplates;
    private final List<TemplateInfo> filteredTemplates = new ArrayList<>();
    private String currentCategory = "houses";
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private boolean previewFlipLeftRight = false;
    private boolean previewFlipFrontBack = false;
    private int tabY;
    private int selectedTabX = -1;
    private int listX;
    private int listTop;
    private int listBottom;

    record MaterialCost(net.minecraft.world.item.Item item, int required, int owned) {}
    private List<MaterialCost> currentCosts = new ArrayList<>();
    private int materialScrollOffset = 0;

    public InstantBuilderScreen(List<TemplateInfo> templates) {
        super(Component.translatable("instant_structure.screen.builder.title"));
        this.allTemplates = templates;
        BuilderGuiPreferences.ensureLoaded();
        String lastCategory = BuilderGuiPreferences.lastCategory();
        if (VALID_CATEGORIES.contains(lastCategory.toLowerCase())) {
            currentCategory = lastCategory.toLowerCase();
        }
        filterByCategory(true);
    }

    private void filterByCategory(boolean restoreLastSelection) {
        filteredTemplates.clear();
        for (TemplateInfo t : allTemplates) {
            if (t.category().equalsIgnoreCase(currentCategory)) {
                filteredTemplates.add(t);
            }
        }
        if (filteredTemplates.isEmpty()) {
            selectedIndex = -1;
            scrollOffset = 0;
            return;
        }
        if (restoreLastSelection) {
            String lastName = BuilderGuiPreferences.lastTemplateName();
            int found = -1;
            for (int i = 0; i < filteredTemplates.size(); i++) {
                if (filteredTemplates.get(i).name().equals(lastName)) {
                    found = i;
                    break;
                }
            }
            selectedIndex = found >= 0 ? found : 0;
        } else {
            selectedIndex = 0;
        }
        ensureSelectedVisible();
    }

    private int maxScrollOffset() {
        return Math.max(0, filteredTemplates.size() - VISIBLE_ROWS);
    }

    private void clampScrollOffset() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset()));
    }

    private void ensureSelectedVisible() {
        if (selectedIndex < 0) {
            return;
        }
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + VISIBLE_ROWS) {
            scrollOffset = selectedIndex - VISIBLE_ROWS + 1;
        }
        clampScrollOffset();
    }

    private boolean isMouseOverList(double mouseX, double mouseY) {
        return mouseX >= listX
                && mouseX < listX + LIST_WIDTH
                && mouseY >= listTop
                && mouseY < listBottom;
    }

    private void layoutListArea() {
        listTop = LIST_TOP_MIN;
        listBottom = listTop + VISIBLE_ROWS * ROW_HEIGHT;
    }

    private int buttonY() {
        return height - FOOTER_BOTTOM_MARGIN - FOOTER_BUTTON_HEIGHT;
    }

    private int scrollRangeY() {
        return listBottom + 12;
    }

    private int selectedTemplateY() {
        return listBottom + 2;
    }

    private int footerRowLeft() {
        int totalWidth = FLIP_BUTTON_WIDTH + SELECT_BUTTON_WIDTH + CANCEL_BUTTON_WIDTH + FLIP_BUTTON_WIDTH
                + FOOTER_BUTTON_GAP * 3;
        return width / 2 - totalWidth / 2;
    }

    private void addFooterButtons(int buttonY) {
        int x = footerRowLeft();

        addRenderableWidget(Button.builder(
                flipButtonLabel("instant_structure.screen.builder.flip_left", previewFlipLeftRight),
                btn -> {
                    previewFlipLeftRight = !previewFlipLeftRight;
                    rebuildWidgets();
                }
        ).bounds(x, buttonY, FLIP_BUTTON_WIDTH, FOOTER_BUTTON_HEIGHT).build());
        x += FLIP_BUTTON_WIDTH + FOOTER_BUTTON_GAP;

        addRenderableWidget(Button.builder(Component.translatable("instant_structure.screen.builder.select_place"), btn -> {
            if (selectedIndex >= 0 && selectedIndex < filteredTemplates.size()) {
                TemplateInfo selected = filteredTemplates.get(selectedIndex);
                BuilderGuiPreferences.saveSelection(selected.category(), selected.name());
                ClientPlacementRegistry.active = true;
                ClientPlacementRegistry.tentativelyConfirmed = false;
                ClientPlacementRegistry.category = selected.category();
                ClientPlacementRegistry.templateName = selected.name();
                ClientPlacementRegistry.sizeX = selected.sizeX();
                ClientPlacementRegistry.sizeY = selected.sizeY();
                ClientPlacementRegistry.sizeZ = selected.sizeZ();
                ClientPlacementRegistry.rotation = 0;
                ClientPlacementRegistry.mirrorLeftRight = previewFlipLeftRight;
                ClientPlacementRegistry.mirrorFrontBack = previewFlipFrontBack;
                ClientPlacementRegistry.placementYOffset = 0;
                ClientPlacementRegistry.lockedAnchor = null;
                ClientPlacementRegistry.lockedPlacementOrigin = null;
                ClientPlacementRegistry.previewBlocks = List.of();
                Minecraft.getInstance().gui.setScreen(null);
                if (Minecraft.getInstance().getConnection() != null) {
                    Minecraft.getInstance().getConnection().send(new RequestPreviewPayload(selected.category(), selected.name()));
                }
                ClientPreviewLoader.requestPreview(selected.category(), selected.name());
            }
        }).bounds(x, buttonY, SELECT_BUTTON_WIDTH, FOOTER_BUTTON_HEIGHT).build());
        x += SELECT_BUTTON_WIDTH + FOOTER_BUTTON_GAP;

        addRenderableWidget(Button.builder(Component.translatable("instant_structure.screen.builder.cancel"), btn ->
                Minecraft.getInstance().gui.setScreen(null)
        ).bounds(x, buttonY, CANCEL_BUTTON_WIDTH, FOOTER_BUTTON_HEIGHT).build());
        x += CANCEL_BUTTON_WIDTH + FOOTER_BUTTON_GAP;

        addRenderableWidget(Button.builder(
                flipButtonLabel("instant_structure.screen.builder.flip_right", previewFlipFrontBack),
                btn -> {
                    previewFlipFrontBack = !previewFlipFrontBack;
                    rebuildWidgets();
                }
        ).bounds(x, buttonY, FLIP_BUTTON_WIDTH, FOOTER_BUTTON_HEIGHT).build());
    }

    @Override
    protected void init() {
        super.init();
        tabY = 40;
        listX = width / 2 - LIST_WIDTH / 2;
        layoutListArea();

        addCategoryTab(width / 2 - 130, "houses", "instant_structure.screen.builder.tab.houses");
        addCategoryTab(width / 2 - 40, "arenas", "instant_structure.screen.builder.tab.arenas");
        addCategoryTab(width / 2 + 50, "custom", "instant_structure.screen.builder.tab.custom");

        int visibleCount = Math.min(filteredTemplates.size() - scrollOffset, VISIBLE_ROWS);
        for (int i = 0; i < visibleCount; i++) {
            final int index = scrollOffset + i;
            TemplateInfo info = filteredTemplates.get(index);
            addRenderableWidget(Button.builder(templateLabel(info, index == selectedIndex), btn -> {
                selectedIndex = index;
                BuilderGuiPreferences.saveSelection(info.category(), info.name());
                rebuildWidgets();
            }).bounds(listX, listTop + i * ROW_HEIGHT, LIST_WIDTH, 20).build());
        }

        if (filteredTemplates.size() > VISIBLE_ROWS) {
            int scrollBtnX = listX + LIST_WIDTH + 4;
            Button scrollUpBtn = Button.builder(Component.literal("▲"), btn -> {
                if (scrollOffset > 0) {
                    scrollOffset--;
                    rebuildWidgets();
                }
            }).bounds(scrollBtnX, listTop, 20, 20).build();
            scrollUpBtn.active = scrollOffset > 0;
            addRenderableWidget(scrollUpBtn);

            Button scrollDownBtn = Button.builder(Component.literal("▼"), btn -> {
                if (scrollOffset < maxScrollOffset()) {
                    scrollOffset++;
                    rebuildWidgets();
                }
            }).bounds(scrollBtnX, listBottom - 20, 20, 20).build();
            scrollDownBtn.active = scrollOffset < maxScrollOffset();
            addRenderableWidget(scrollDownBtn);
        }

        if (selectedIndex >= 0 && selectedIndex < filteredTemplates.size()) {
            TemplateInfo selected = filteredTemplates.get(selectedIndex);
            currentCosts = getMaterialCosts(selected);
            
            // 素材確認ボタンを追加（ローカル NBT が無い場合はサーバープレビューで算出）
            addRenderableWidget(Button.builder(Component.translatable("instant_structure.screen.builder.check_materials"), btn -> {
                Minecraft.getInstance().gui.setScreen(new MaterialListScreen(this, selected.category(), selected.name()));
            }).bounds(listX, selectedTemplateY() + 24, 115, 20).build());

            boolean isOp = Minecraft.getInstance().player != null; // 簡易権限チェック (OP判定の代わり)
            if (isOp) {
                addRenderableWidget(Button.builder(Component.translatable("instant_structure.screen.builder.delete").withStyle(ChatFormatting.RED), btn -> {
                    Minecraft.getInstance().gui.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(
                            confirmed -> {
                                if (confirmed) {
                                    if (Minecraft.getInstance().getConnection() != null) {
                                        Minecraft.getInstance().getConnection().send(new DeleteTemplatePayload(selected.category(), selected.name()));
                                    }
                                }
                                Minecraft.getInstance().gui.setScreen(this);
                            },
                            Component.translatable("instant_structure.screen.builder.delete_confirm.title"),
                            Component.translatable("instant_structure.screen.builder.delete_confirm.message", selected.name())
                    ));
                }).bounds(listX + LIST_WIDTH - 115, selectedTemplateY() + 24, 115, 20).build());
            }
        } else {
            currentCosts = new java.util.ArrayList<>();
        }

        addFooterButtons(buttonY());
    }

    private void addCategoryTab(int x, String categoryId, String labelKey) {
        if (currentCategory.equals(categoryId)) {
            selectedTabX = x;
        }
        addRenderableWidget(Button.builder(categoryTabLabel(categoryId, labelKey), btn -> {
            currentCategory = categoryId;
            filterByCategory(false);
            rebuildWidgets();
        }).bounds(x, tabY, TAB_WIDTH, TAB_HEIGHT).build());
    }

    private Component categoryTabLabel(String categoryId, String labelKey) {
        MutableComponent label = Component.translatable(labelKey);
        if (currentCategory.equals(categoryId)) {
            return Component.literal("▶ ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD).append(label);
        }
        return label;
    }

    private Component templateLabel(TemplateInfo info, boolean selected) {
        MutableComponent label = Component.literal(
                info.name() + " (" + info.sizeX() + "x" + info.sizeY() + "x" + info.sizeZ() + ")"
        );
        if (selected) {
            return Component.literal("▶ ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD).append(label);
        }
        return label;
    }

    private static Component flipButtonLabel(String baseKey, boolean active) {
        MutableComponent label = Component.translatable(baseKey);
        if (active) {
            return label.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                    .append(Component.translatable("instant_structure.screen.builder.flip_on"));
        }
        return label.append(Component.translatable("instant_structure.screen.builder.flip_off"));
    }

    @Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);
        layoutListArea();
        guiGraphics.centeredText(font, title, width / 2, 15, 0xFFFFFFFF);

        guiGraphics.centeredText(font,
                 Component.translatable("instant_structure.screen.export.category"),
                width / 2, 28, 0xFFE0E0E0);

        if (selectedTabX >= 0) {
            guiGraphics.fill(selectedTabX - 2, tabY - 2, selectedTabX + TAB_WIDTH + 2, tabY + TAB_HEIGHT + 2, 0x66FFD700);
        }

        if (filteredTemplates.size() > VISIBLE_ROWS) {
            int from = scrollOffset + 1;
            int to = Math.min(scrollOffset + VISIBLE_ROWS, filteredTemplates.size());
            guiGraphics.centeredText(font,
                    Component.translatable("instant_structure.screen.builder.scroll_range", from, to, filteredTemplates.size()),
                    width / 2,
                    scrollRangeY(),
                    0xFFAAAAAA);
        }



        if (selectedIndex >= 0 && selectedIndex < filteredTemplates.size()) {
            TemplateInfo selected = filteredTemplates.get(selectedIndex);
            guiGraphics.centeredText(font,
                    Component.translatable("instant_structure.screen.builder.selected", selected.name()),
                    width / 2, selectedTemplateY(), 0xFFFF55);
        }
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (filteredTemplates.size() > VISIBLE_ROWS && isMouseOverList(mouseX, mouseY)) {
            int nextOffset = scrollOffset - (int) Math.signum(scrollY);
            if (nextOffset != scrollOffset) {
                scrollOffset = Math.max(0, Math.min(nextOffset, maxScrollOffset()));
                rebuildWidgets();
                return true;
            }
        }
        if (!currentCosts.isEmpty() && isMouseOverMaterialList(mouseX, mouseY)) {
            int nextOffset = materialScrollOffset - (int) Math.signum(scrollY);
            if (nextOffset != materialScrollOffset) {
                materialScrollOffset = Math.max(0, Math.min(nextOffset, maxMaterialScrollOffset()));
                rebuildWidgets();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isMouseOverMaterialList(double mouseX, double mouseY) {
        int costX = width / 2 + 135;
        return mouseX >= costX && mouseX < width - 15 && mouseY >= listTop && mouseY < listBottom;
    }

    private int maxMaterialScrollOffset() {
        return Math.max(0, currentCosts.size() - 5);
    }

    private List<MaterialCost> getMaterialCosts(TemplateInfo info) {
        return computeMaterialCosts(info.category(), info.name());
    }

    /**
     * プレビューキャッシュ → ローカル NBT の順で必要素材を集計する。
     * 専用サーバー上のテンプレなど、クライアントに NBT が無い場合はキャッシュ待ちになる。
     */
    static List<MaterialCost> computeMaterialCosts(String category, String templateName) {
        List<MaterialCost> costs = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return costs;
        }

        List<com.ogatamizuki.instantstructure.PreviewBlockEntry> entries =
                new ArrayList<>(ClientPreviewLoader.getCachedBlocks(category, templateName));
        if (entries.isEmpty()) {
            java.nio.file.Path nbtPath = com.ogatamizuki.instantstructure.StructureTemplateHelper.resolveTemplatePath(
                    InstantStructurePaths.configRoot(), category, templateName);
            if (java.nio.file.Files.exists(nbtPath)) {
                try {
                    entries = com.ogatamizuki.instantstructure.StructureTemplateHelper.extractSolidBlocks(
                            mc.level.registryAccess(), nbtPath);
                } catch (Exception e) {
                    com.ogatamizuki.instantstructure.InstantStructureCommon.LOGGER.error(
                            "Failed to load local template for materials: {}/{}", category, templateName, e);
                }
            }
        }
        if (entries.isEmpty()) {
            return costs;
        }

        try {
            Map<net.minecraft.world.item.Item, Integer> req = new HashMap<>();
            net.minecraft.core.HolderLookup.Provider provider = mc.level.registryAccess();
            net.minecraft.core.HolderLookup<net.minecraft.world.level.block.Block> blockLookup =
                    (net.minecraft.core.HolderLookup<net.minecraft.world.level.block.Block>) provider.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK);

            for (com.ogatamizuki.instantstructure.PreviewBlockEntry entry : entries) {
                net.minecraft.world.level.block.state.BlockState state =
                        com.ogatamizuki.instantstructure.BlockStateParserSupport.parse(blockLookup, entry.blockState());

                // 2マス占有ブロック（ベッド、ドア、2段の植物など）の重複カウントを防ぐ
                if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART) &&
                        state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART)
                                == net.minecraft.world.level.block.state.properties.BedPart.FOOT) {
                    continue;
                }
                if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF) &&
                        state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                                == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) {
                    continue;
                }

                net.minecraft.world.item.Item item = state.getBlock().asItem();
                if (item != net.minecraft.world.item.Items.AIR) {
                    req.put(item, req.getOrDefault(item, 0) + 1);
                }
            }

            Map<net.minecraft.world.item.Item, Integer> owned = new HashMap<>();
            net.minecraft.world.entity.player.Inventory inv = mc.player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                net.minecraft.world.item.ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty()) {
                    owned.put(stack.getItem(), owned.getOrDefault(stack.getItem(), 0) + stack.getCount());
                }
            }

            BlockPos anchorPos = ClientPlacementRegistry.lockedAnchor != null
                    ? ClientPlacementRegistry.lockedAnchor
                    : ClientPlacementRegistry.resolveCrosshairAnchor(mc);
            if (anchorPos != null) {
                List<BlockPos> checkPositions = new ArrayList<>();
                checkPositions.add(anchorPos);
                for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                    checkPositions.add(anchorPos.relative(dir));
                }
                for (BlockPos pos : checkPositions) {
                    net.minecraft.world.level.block.entity.BlockEntity be = mc.level.getBlockEntity(pos);
                    if (be instanceof net.minecraft.world.Container container) {
                        for (int i = 0; i < container.getContainerSize(); i++) {
                            net.minecraft.world.item.ItemStack stack = container.getItem(i);
                            if (!stack.isEmpty()) {
                                owned.put(stack.getItem(), owned.getOrDefault(stack.getItem(), 0) + stack.getCount());
                            }
                        }
                    }
                }
            }

            List<Map.Entry<net.minecraft.world.item.Item, Integer>> sorted = new ArrayList<>(req.entrySet());
            sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
            for (Map.Entry<net.minecraft.world.item.Item, Integer> entry : sorted) {
                costs.add(new MaterialCost(entry.getKey(), entry.getValue(), owned.getOrDefault(entry.getKey(), 0)));
            }
        } catch (Exception e) {
            com.ogatamizuki.instantstructure.InstantStructureCommon.LOGGER.error("Failed to calculate materials in GUI", e);
        }
        return costs;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
