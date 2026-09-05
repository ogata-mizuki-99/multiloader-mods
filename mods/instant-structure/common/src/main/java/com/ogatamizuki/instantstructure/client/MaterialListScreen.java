package com.ogatamizuki.instantstructure.client;

import com.ogatamizuki.instantstructure.InstantStructurePlatform;
import com.ogatamizuki.instantstructure.RequestPreviewPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class MaterialListScreen extends Screen {
    private final Screen parent;
    private final String category;
    private final String templateName;
    private final Runnable cacheListener = this::onPreviewCached;
    private List<InstantBuilderScreen.MaterialCost> costs;
    private boolean loading;
    private int waitTicks;
    private int scrollOffset = 0;
    private static final int VISIBLE_ROWS = 9;
    private static final int ROW_HEIGHT = 18;

    public MaterialListScreen(Screen parent, String category, String templateName) {
        super(Component.translatable("instant_structure.screen.material_list.title"));
        this.parent = parent;
        this.category = category;
        this.templateName = templateName;
        this.costs = new ArrayList<>(InstantBuilderScreen.computeMaterialCosts(category, templateName));
        this.loading = this.costs.isEmpty();
    }

    private int maxScrollOffset() {
        return Math.max(0, costs.size() - VISIBLE_ROWS);
    }

    private void onPreviewCached() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != this) {
            return;
        }
        List<InstantBuilderScreen.MaterialCost> next = InstantBuilderScreen.computeMaterialCosts(category, templateName);
        if (next.isEmpty()) {
            return;
        }
        this.costs = new ArrayList<>(next);
        this.loading = false;
        this.scrollOffset = 0;
    }

    private void requestServerPreview() {
        RequestPreviewPayload payload = new RequestPreviewPayload(category, templateName);
        if (InstantStructurePlatform.sendToServer != null) {
            InstantStructurePlatform.sendToServer.accept(payload);
        } else if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().send(payload);
        }
        ClientPreviewLoader.requestPreview(category, templateName);
    }

    @Override
    protected void init() {
        super.init();
        ClientPreviewLoader.addCacheListener(cacheListener);
        if (loading) {
            requestServerPreview();
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        addRenderableWidget(Button.builder(Component.translatable("gui.back"), btn -> this.minecraft.gui.setScreen(parent))
                .bounds(centerX - 50, centerY + 95, 100, 20)
                .build());
    }

    @Override
    public void removed() {
        ClientPreviewLoader.removeCacheListener(cacheListener);
        super.removed();
    }

    @Override
    public void tick() {
        super.tick();
        if (loading) {
            waitTicks++;
            if (waitTicks > 100) {
                loading = false;
            }
        }
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

        if (loading && costs.isEmpty()) {
            guiGraphics.centeredText(this.font, Component.translatable("instant_structure.screen.material_list.loading"), centerX, centerY, 0xFFAAAAAA);
        } else if (costs.isEmpty()) {
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
        this.minecraft.gui.setScreen(parent);
    }
}
