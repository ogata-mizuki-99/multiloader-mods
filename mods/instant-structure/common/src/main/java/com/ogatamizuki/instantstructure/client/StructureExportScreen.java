package com.ogatamizuki.instantstructure.client;

import com.ogatamizuki.instantstructure.ExportCancelPayload;
import com.ogatamizuki.instantstructure.ExportSubmitPayload;
import com.ogatamizuki.instantstructure.InstantStructurePlatform;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class StructureExportScreen extends Screen {
    private static final int NAME_BOX_WIDTH = 220;
    private static final int TAB_WIDTH = 80;
    private static final int TAB_HEIGHT = 20;

    private static final int TITLE_Y_OFFSET = -96;
    private static final int HINT_Y_OFFSET = -82;
    private static final int RANGE_Y_OFFSET = -66;
    private static final int SIZE_Y_OFFSET = -54;
    private static final int CATEGORY_LABEL_Y_OFFSET = -42;
    private static final int TAB_Y_OFFSET = -26;
    private static final int SELECTED_CATEGORY_Y_OFFSET = 2;
    private static final int NAME_LABEL_Y_OFFSET = 14;
    private static final int NAME_BOX_Y_OFFSET = 26;
    private static final int BUTTON_Y_OFFSET = 56;

    private final BlockPos exportPos1;
    private final BlockPos exportPos2;
    private final SelectionBounds exportBounds;

    private EditBox nameBox;
    private String category = "custom";
    private boolean submitted;
    private int tabY;
    private int selectedTabX = -1;

    private StructureExportScreen(BlockPos pos1, BlockPos pos2) {
        super(Component.translatable("instant_structure.screen.export.title"));
        this.exportPos1 = pos1;
        this.exportPos2 = pos2;
        this.exportBounds = SelectionBounds.from(pos1, pos2);
    }

    public static void open(BlockPos pos1, BlockPos pos2) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(new StructureExportScreen(pos1, pos2));
        }
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.tabY = centerY + TAB_Y_OFFSET;

        addCategoryTab(centerX - 130, "houses", "instant_structure.screen.builder.tab.houses");
        addCategoryTab(centerX - 40, "arenas", "instant_structure.screen.builder.tab.arenas");
        addCategoryTab(centerX + 50, "custom", "instant_structure.screen.builder.tab.custom");

        this.nameBox = new EditBox(this.font, centerX - NAME_BOX_WIDTH / 2, centerY + NAME_BOX_Y_OFFSET,
                NAME_BOX_WIDTH, 20, Component.empty());
        this.nameBox.setMaxLength(64);
        this.nameBox.setHint(Component.translatable("instant_structure.screen.export.name"));
        this.addRenderableWidget(this.nameBox);
        this.setInitialFocus(this.nameBox);

        addRenderableWidget(Button.builder(Component.translatable("instant_structure.screen.export.submit"), btn -> submit())
                .bounds(centerX - 105, centerY + BUTTON_Y_OFFSET, 100, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), btn -> this.onClose())
                .bounds(centerX + 5, centerY + BUTTON_Y_OFFSET, 100, 20).build());


    }

    private void addCategoryTab(int x, String categoryId, String labelKey) {
        if (category.equals(categoryId)) {
            selectedTabX = x;
        }
        addRenderableWidget(Button.builder(categoryTabLabel(categoryId, labelKey), btn -> {
            category = categoryId;
            rebuildWidgets();
        }).bounds(x, tabY, TAB_WIDTH, TAB_HEIGHT).build());
    }

    private Component categoryTabLabel(String categoryId, String labelKey) {
        MutableComponent label = Component.translatable(labelKey);
        if (category.equals(categoryId)) {
            return Component.literal("▶ ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD).append(label);
        }
        return label;
    }

    private Component selectedCategoryLabel() {
        return switch (category) {
            case "houses" -> Component.translatable("instant_structure.screen.builder.tab.houses");
            case "arenas" -> Component.translatable("instant_structure.screen.builder.tab.arenas");
            default -> Component.translatable("instant_structure.screen.builder.tab.custom");
        };
    }

    private void submit() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || this.nameBox == null) {
            return;
        }

        String name = this.nameBox.getValue().trim();
        if (name.isEmpty() || name.contains("/") || name.contains("\\") || name.contains(":")) {
            return;
        }

        submitted = true;
        if (InstantStructurePlatform.sendToServer != null) {
            InstantStructurePlatform.sendToServer.accept(new ExportSubmitPayload(name, category));
        }
        super.onClose();
    }

    @Override
    public void onClose() {
        if (!submitted && InstantStructurePlatform.sendToServer != null) {
            InstantStructurePlatform.sendToServer.accept(new ExportCancelPayload(false));
        }
        super.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        guiGraphics.centeredText(this.font, this.title, centerX, centerY + TITLE_Y_OFFSET, 0xFFFFFFFF);
        guiGraphics.centeredText(this.font,
                Component.translatable("instant_structure.screen.export.hint"),
                centerX, centerY + HINT_Y_OFFSET, 0xFFC0C0C0);

        guiGraphics.centeredText(this.font,
                SelectionDisplayTexts.exportRange(exportPos1, exportPos2),
                centerX, centerY + RANGE_Y_OFFSET, 0xFFA0D0FF);
        guiGraphics.centeredText(this.font,
                SelectionDisplayTexts.exportSize(exportBounds),
                centerX, centerY + SIZE_Y_OFFSET, 0xFFA0D0FF);

        guiGraphics.centeredText(this.font,
                Component.translatable("instant_structure.screen.export.category"),
                centerX, centerY + CATEGORY_LABEL_Y_OFFSET, 0xFFE0E0E0);

        if (selectedTabX >= 0) {
            guiGraphics.fill(selectedTabX - 2, tabY - 2, selectedTabX + TAB_WIDTH + 2, tabY + TAB_HEIGHT + 2, 0x66FFD700);
        }

        guiGraphics.centeredText(this.font,
                Component.translatable("instant_structure.screen.export.selected_category", selectedCategoryLabel()),
                centerX, centerY + SELECTED_CATEGORY_Y_OFFSET, 0xFFFFD700);

        guiGraphics.centeredText(this.font,
                Component.translatable("instant_structure.screen.export.name"),
                centerX, centerY + NAME_LABEL_Y_OFFSET, 0xFFE0E0E0);

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
