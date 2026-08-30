package com.ogatamizuki.elytraslot.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("elytra_slot.screen.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        addRenderableWidget(Button.builder(Component.translatable("elytra_slot.screen.config.edit_slots"), btn -> {
            this.minecraft.setScreen(new SlotConfigScreen(this));
        }).bounds(centerX - 110, centerY - 30, 220, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("elytra_slot.screen.config.edit_hud"), btn -> {
            this.minecraft.setScreen(new HudConfigScreen(this));
        }).bounds(centerX - 110, centerY, 220, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.back"), btn -> {
            this.minecraft.setScreen(parent);
        }).bounds(centerX - 50, centerY + 40, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);
        guiGraphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFFFF);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
