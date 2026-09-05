package com.ogatamizuki.instantstructure.client;

import com.ogatamizuki.instantstructure.InstantStructureCommonConfigPushPayload;
import com.ogatamizuki.instantstructure.InstantStructureConfig;
import com.ogatamizuki.instantstructure.InstantStructurePlatform;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class InstantStructureConfigScreen extends Screen {
    private final Screen parent;

    public InstantStructureConfigScreen(Screen parent) {
        super(Component.translatable("instant_structure.screen.config.title"));
        this.parent = parent;
        InstantStructureConfig.load();
    }

    public InstantStructureConfigScreen(Object container, Screen parent) {
        this(parent);
    }

    private Component getRecipeToggleLabel() {
        return CommonComponents.optionStatus(
                Component.translatable("instant_structure.screen.config.recipe"),
                InstantStructureConfig.enableCraftingRecipe
        );
    }

    private Component getMaterialConsumptionToggleLabel() {
        return CommonComponents.optionStatus(
                Component.translatable("instant_structure.screen.config.material"),
                InstantStructureConfig.enableMaterialConsumption
        );
    }

    private Component getDropClearedBlocksToggleLabel() {
        return CommonComponents.optionStatus(
                Component.translatable("instant_structure.screen.config.drop"),
                InstantStructureConfig.dropClearedBlocks
        );
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Toggles
        addRenderableWidget(Button.builder(getRecipeToggleLabel(), btn -> {
            InstantStructureConfig.enableCraftingRecipe = !InstantStructureConfig.enableCraftingRecipe;
            InstantStructureConfig.save();
            btn.setMessage(getRecipeToggleLabel());
        }).bounds(centerX - 110, centerY - 60, 220, 20).build());

        addRenderableWidget(Button.builder(getMaterialConsumptionToggleLabel(), btn -> {
            InstantStructureConfig.enableMaterialConsumption = !InstantStructureConfig.enableMaterialConsumption;
            InstantStructureConfig.save();
            btn.setMessage(getMaterialConsumptionToggleLabel());
        }).bounds(centerX - 110, centerY - 36, 220, 20).build());

        addRenderableWidget(Button.builder(getDropClearedBlocksToggleLabel(), btn -> {
            InstantStructureConfig.dropClearedBlocks = !InstantStructureConfig.dropClearedBlocks;
            InstantStructureConfig.save();
            btn.setMessage(getDropClearedBlocksToggleLabel());
        }).bounds(centerX - 110, centerY - 12, 220, 20).build());

        // Folders
        addRenderableWidget(Button.builder(
                        Component.translatable("instant_structure.screen.config.open_templates_folder"),
                        btn -> ExportFolderOpener.openTemplatesStructureFolder(this.minecraft)
                )
                .bounds(centerX - 110, centerY + 16, 220, 20)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("instant_structure.screen.config.push_to_server"),
                        btn -> pushToServer())
                .bounds(centerX - 110, centerY + 42, 220, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.back"), btn -> this.minecraft.gui.setScreen(parent))
                .bounds(centerX - 50, centerY + 72, 100, 20)
                .build());
    }

    private void pushToServer() {
        Minecraft mc = this.minecraft;
        if (mc == null || mc.player == null) {
            return;
        }
        if (mc.getConnection() == null) {
            mc.player.sendSystemMessage(
                    Component.translatable("instant_structure.screen.config.push_need_connection")
                            .withStyle(ChatFormatting.RED));
            return;
        }
        InstantStructureCommonConfigPushPayload payload = InstantStructureCommonConfigPushPayload.fromLocalConfig();
        if (InstantStructurePlatform.sendToServer != null) {
            InstantStructurePlatform.sendToServer.accept(payload);
        } else {
            mc.getConnection().send(payload);
        }
        mc.player.sendSystemMessage(
                Component.translatable("instant_structure.screen.config.push_sent").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);
        guiGraphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 76, 0xFFFFFFFF);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }
}
