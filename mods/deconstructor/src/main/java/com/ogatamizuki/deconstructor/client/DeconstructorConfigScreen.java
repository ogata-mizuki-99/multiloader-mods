package com.ogatamizuki.deconstructor.client;

import com.ogatamizuki.deconstructor.DeconstructorCommonConfigPushPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/**
 * Mods 設定の入口。詳細編集は NeoForge {@link ConfigurationScreen}、COMMON のサーバー反映は別ボタン。
 */
public final class DeconstructorConfigScreen extends Screen {
    private final ModContainer container;
    private final Screen parent;

    public DeconstructorConfigScreen(ModContainer container, Screen parent) {
        super(Component.translatable("deconstructor.configuration.title"));
        this.container = container;
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addRenderableWidget(Button.builder(
                Component.translatable("deconstructor.configuration.open_editor"),
                button -> this.minecraft.gui.setScreen(new ConfigurationScreen(container, this))
        ).bounds(centerX - 110, centerY - 24, 220, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("deconstructor.configuration.push_to_server"),
                button -> pushToServer()
        ).bounds(centerX - 110, centerY + 4, 220, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> this.minecraft.gui.setScreen(parent)
        ).bounds(centerX - 50, centerY + 36, 100, 20).build());
    }

    private void pushToServer() {
        Minecraft mc = this.minecraft;
        if (mc == null || mc.player == null) {
            return;
        }
        if (mc.getConnection() == null) {
            mc.player.sendSystemMessage(
                    Component.translatable("deconstructor.configuration.push_need_connection")
                            .withStyle(ChatFormatting.RED));
            return;
        }
        mc.getConnection().send(DeconstructorCommonConfigPushPayload.fromLocalConfig());
        mc.player.sendSystemMessage(
                Component.translatable("deconstructor.configuration.push_sent").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);
        guiGraphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 52, 0xFFFFFFFF);
        guiGraphics.centeredText(
                this.font,
                Component.translatable("deconstructor.configuration.push_hint"),
                this.width / 2,
                this.height / 2 - 40,
                0xFFAAAAAA);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }
}
