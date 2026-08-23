package com.ogatamizuki.instantstructure.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class MaterialListScreen extends Screen {
    private final Screen parent;
    private final List<InstantBuilderScreen.MaterialCost> costs;
    private int scrollOffset = 0;
    private static final int VISIBLE_ROWS = 9;
    private static final int ROW_HEIGHT = 18;

    public MaterialListScreen(Screen parent, List<InstantBuilderScreen.MaterialCost> costs) {
        super(Component.translatable("instant_structure.screen.material_list.title"));
        this.parent = parent;
        this.costs = costs;
    }

    private int maxScrollOffset() {
        return Math.max(0, costs.size() - VISIBLE_ROWS);
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        addRenderableWidget(Button.builder(Component.translatable("gui.back"), btn -> this.minecraft.setScreen(parent))
                .bounds(centerX - 50, centerY + 95, 100, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        guiGraphics.centeredText(this.font, this.title, centerX, centerY - 110, 0xFFFFFFFF);

        int panelWidth = 320;
        int panelHeight = 175;
        int left = centerX - panelWidth / 2;
        int top = centerY - 88;
        guiGraphics.fill(left - 4, top - 4, left + panelWidth + 4, top + panelHeight + 4, 0xA0000000);

        if (costs.isEmpty()) {
            guiGraphics.centeredText(this.font, Component.translatable("instant_structure.screen.material_list.empty"), centerX, centerY, 0xFFAAAAAA);
        } else {
            int visibleCount = Math.min(costs.size() - scrollOffset, VISIBLE_ROWS);
            for (int i = 0; i < visibleCount; i++) {
                InstantBuilderScreen.MaterialCost cost = costs.get(scrollOffset + i);
                String itemName = Component.translatable(cost.item().getDescriptionId()).getString();
                String ratio = cost.owned() + " / " + cost.required();
                int color = cost.owned() >= cost.required() ? 0xFF55FF55 : 0xFFFF5555;
                int rowY = top + i * ROW_HEIGHT + 4;

                guiGraphics.fakeItem(new net.minecraft.world.item.ItemStack(cost.item()), left + 6, rowY - 2);
                guiGraphics.text(this.font, itemName, left + 26, rowY, 0xFFFFFFFF, true);
                guiGraphics.text(this.font, ratio, left + panelWidth - this.font.width(ratio) - 6, rowY, color, true);
            }

            if (costs.size() > VISIBLE_ROWS) {
                Component scrollText = Component.translatable("instant_structure.screen.material_list.scrollable", scrollOffset + 1, scrollOffset + visibleCount, costs.size());
                guiGraphics.centeredText(this.font, scrollText, centerX, top + panelHeight - 12, 0xFFAAAAAA);
            }
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int nextOffset = scrollOffset - (int) Math.signum(scrollY);
        if (nextOffset != scrollOffset) {
            scrollOffset = Math.max(0, Math.min(nextOffset, maxScrollOffset()));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
