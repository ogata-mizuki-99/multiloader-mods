package com.ogatamizuki.deconstructor.fabric;

import com.ogatamizuki.deconstructor.Config;
import com.ogatamizuki.deconstructor.DeconstructorCommonConfigPushPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DeconstructorConfigScreenFabric extends Screen {
    private final Screen parent;
    private EditBox excludedItemsBox;

    public DeconstructorConfigScreenFabric(Screen parent) {
        super(Component.translatable("deconstructor.configuration.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.excludedItemsBox = new EditBox(
                this.font,
                centerX - 110,
                centerY - 20,
                220,
                20,
                Component.translatable("deconstructor.configuration.excludedItems")
        );
        this.excludedItemsBox.setMaxLength(1024);
        this.excludedItemsBox.setValue(Config.getExcludedItems());
        addRenderableWidget(this.excludedItemsBox);

        addRenderableWidget(Button.builder(
                Component.translatable("deconstructor.configuration.push_to_server"),
                btn -> pushToServer()
        ).bounds(centerX - 110, centerY + 10, 220, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                btn -> {
                    Config.setExcludedItems(this.excludedItemsBox.getValue());
                    this.minecraft.setScreen(parent);
                }
        ).bounds(centerX - 50, centerY + 40, 100, 20).build());
    }

    private void pushToServer() {
        Minecraft mc = this.minecraft;
        if (mc == null || mc.player == null) return;

        Config.setExcludedItems(this.excludedItemsBox.getValue());
        if (ClientPlayNetworking.canSend(DeconstructorCommonConfigPushPayload.TYPE)) {
            ClientPlayNetworking.send(DeconstructorCommonConfigPushPayload.fromLocalConfig());
            mc.player.sendSystemMessage(
                    Component.translatable("deconstructor.configuration.push_sent")
                            .withStyle(ChatFormatting.YELLOW));
        }
        this.minecraft.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);
        guiGraphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFFFF);
        guiGraphics.centeredText(
                this.font,
                Component.translatable("deconstructor.configuration.excludedItems"),
                this.width / 2,
                this.height / 2 - 34,
                0xFFAAAAAA
        );
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.excludedItemsBox != null) {
            Config.setExcludedItems(this.excludedItemsBox.getValue());
        }
        this.minecraft.setScreen(parent);
    }
}
