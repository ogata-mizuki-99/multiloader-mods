package com.ogatamizuki.lookalike.fabric;

import com.ogatamizuki.lookalike.Config;
import com.ogatamizuki.lookalike.LookalikeCommon;
import com.ogatamizuki.lookalike.LookalikeCommonConfigPushPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class LookalikeConfigScreenFabric extends Screen {
    private final Screen parent;

    private boolean allowDefaultPlayerList;
    private boolean hideAllNametags;
    private boolean enableMirrorCrafting;

    public LookalikeConfigScreenFabric(Screen parent) {
        super(Component.translatable("lookalike.configuration.title"));
        this.parent = parent;
        this.allowDefaultPlayerList = Config.allowDefaultPlayerList.get();
        this.hideAllNametags = Config.hideAllNametags.get();
        this.enableMirrorCrafting = Config.enableMirrorCrafting.get();
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int startY = this.height / 2 - 50;

        addRenderableWidget(Checkbox.builder(
                Component.translatable("lookalike.config.allowDefaultPlayerList"), this.font)
                .pos(centerX - 110, startY)
                .selected(allowDefaultPlayerList)
                .onValueChange((cb, value) -> this.allowDefaultPlayerList = value)
                .build());

        addRenderableWidget(Checkbox.builder(
                Component.translatable("lookalike.config.hideAllNametags"), this.font)
                .pos(centerX - 110, startY + 24)
                .selected(hideAllNametags)
                .onValueChange((cb, value) -> this.hideAllNametags = value)
                .build());

        addRenderableWidget(Checkbox.builder(
                Component.translatable("lookalike.config.enableMirrorCrafting"), this.font)
                .pos(centerX - 110, startY + 48)
                .selected(enableMirrorCrafting)
                .onValueChange((cb, value) -> this.enableMirrorCrafting = value)
                .build());

        addRenderableWidget(Button.builder(
                Component.translatable("lookalike.configuration.push_to_server"),
                btn -> pushToServer()
        ).bounds(centerX - 110, startY + 80, 220, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                btn -> this.minecraft.setScreen(parent)
        ).bounds(centerX - 50, startY + 108, 100, 20).build());
    }

    private void pushToServer() {
        Minecraft mc = this.minecraft;
        if (mc == null || mc.player == null) return;

        Config.allowDefaultPlayerList.set(this.allowDefaultPlayerList);
        Config.hideAllNametags.set(this.hideAllNametags);
        Config.enableMirrorCrafting.set(this.enableMirrorCrafting);

        if (ClientPlayNetworking.canSend(LookalikeCommonConfigPushPayload.TYPE)) {
            ClientPlayNetworking.send(LookalikeCommonConfigPushPayload.fromLocalConfig());
            mc.player.sendSystemMessage(
                    Component.translatable("lookalike.configuration.push_sent")
                            .withStyle(ChatFormatting.YELLOW));
        }
        this.minecraft.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);
        guiGraphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 75, 0xFFFFFFFF);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
