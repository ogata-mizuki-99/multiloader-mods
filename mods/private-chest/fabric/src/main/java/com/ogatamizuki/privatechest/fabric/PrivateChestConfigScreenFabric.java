package com.ogatamizuki.privatechest.fabric;

import com.ogatamizuki.privatechest.Config;
import com.ogatamizuki.privatechest.PrivateChestCommonConfigPushPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PrivateChestConfigScreenFabric extends Screen {
    private final Screen parent;
    private boolean enableLockerCrafting;

    public PrivateChestConfigScreenFabric(Screen parent) {
        super(Component.translatable("privatechest.configuration.title"));
        this.parent = parent;
        this.enableLockerCrafting = Config.isEnableLockerCrafting();
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        addRenderableWidget(Checkbox.builder(
                Component.translatable("privatechest.config.enableLockerCrafting"), this.font)
                .pos(centerX - 120, centerY - 30)
                .selected(enableLockerCrafting)
                .onValueChange((cb, value) -> {
                    this.enableLockerCrafting = value;
                    Config.setEnableLockerCrafting(value);
                })
                .build());

        addRenderableWidget(Button.builder(
                Component.translatable("privatechest.configuration.push_to_server"),
                button -> pushToServer()
        ).bounds(centerX - 110, centerY + 8, 220, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> this.minecraft.setScreen(parent)
        ).bounds(centerX - 50, centerY + 40, 100, 20).build());
    }

    private void pushToServer() {
        Minecraft mc = this.minecraft;
        if (mc == null || mc.player == null) {
            return;
        }
        if (mc.getConnection() == null) {
            mc.player.sendSystemMessage(
                    Component.translatable("privatechest.configuration.push_need_connection")
                            .withStyle(ChatFormatting.RED));
            return;
        }
        ClientPlayNetworking.send(new PrivateChestCommonConfigPushPayload(this.enableLockerCrafting));
        mc.player.sendSystemMessage(
                Component.translatable("privatechest.configuration.push_sent").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);
        guiGraphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 56, 0xFFFFFFFF);
        guiGraphics.centeredText(
                this.font,
                Component.translatable("privatechest.configuration.push_hint"),
                this.width / 2,
                this.height / 2 + 30,
                0xFFAAAAAA);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
