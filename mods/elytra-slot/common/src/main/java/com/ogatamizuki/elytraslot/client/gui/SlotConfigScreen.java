package com.ogatamizuki.elytraslot.client.gui;

import com.ogatamizuki.elytraslot.Config;
import com.ogatamizuki.elytraslot.network.SlotPosSyncPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

public class SlotConfigScreen extends Screen {
    private static final Identifier INVENTORY_LOCATION = Identifier.fromNamespaceAndPath("minecraft",
            "textures/gui/container/inventory.png");

    private final Screen parent;

    // Temporary variables to track positions
    private int elytraX;
    private int elytraY;
    private int fireworkX;
    private int fireworkY;

    // Dragging state
    private int selectedSlot = 0; // 0: None, 1: Elytra, 2: Firework
    private boolean isDragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public SlotConfigScreen(Screen parent) {
        super(Component.translatable("elytra_slot.screen.slot_config.title"));
        this.parent = parent;
        this.elytraX = Config.ELYTRA_SLOT_X.get();
        this.elytraY = Config.ELYTRA_SLOT_Y.get();
        this.fireworkX = Config.FIREWORK_SLOT_X.get();
        this.fireworkY = Config.FIREWORK_SLOT_Y.get();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        addRenderableWidget(Button.builder(Component.translatable("elytra_slot.screen.config.save"), btn -> {
            Config.ELYTRA_SLOT_X.set(this.elytraX);
            Config.ELYTRA_SLOT_Y.set(this.elytraY);
            Config.FIREWORK_SLOT_X.set(this.fireworkX);
            Config.FIREWORK_SLOT_Y.set(this.fireworkY);
            Config.ELYTRA_SLOT_X.save();
            Config.ELYTRA_SLOT_Y.save();
            Config.FIREWORK_SLOT_X.save();
            Config.FIREWORK_SLOT_Y.save();

            // Dynamic local menu slot updating
            if (this.minecraft.player != null) {
                com.ogatamizuki.elytraslot.ElytraSlotCommon.updatePlayerContainerSlotPositions(this.minecraft.player,
                        this.elytraX, this.elytraY, this.fireworkX, this.fireworkY);
            }

            // Sync with server container
            if (this.minecraft.getConnection() != null) {
                this.minecraft.getConnection().send(new SlotPosSyncPayload(
                        this.elytraX, this.elytraY, this.fireworkX, this.fireworkY));
            }

            this.minecraft.gui.setScreen(parent);
        }).bounds(centerX - 115, centerY + 95, 70, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("elytra_slot.screen.config.default"), btn -> {
            this.elytraX = 77;
            this.elytraY = 26;
            this.fireworkX = 77;
            this.fireworkY = 8;
            this.selectedSlot = 0;
        }).bounds(centerX - 35, centerY + 95, 70, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), btn -> {
            this.minecraft.gui.setScreen(parent);
        }).bounds(centerX + 45, centerY + 95, 70, 20).build());
    }

    private int getBaseX() {
        return this.width / 2 - 88;
    }

    private int getBaseY() {
        return this.height / 2 - 83;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);

        int baseX = getBaseX();
        int baseY = getBaseY();

        // 1. Draw Real Inventory Texture (176x166)
        guiGraphics.blit(
                net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                INVENTORY_LOCATION,
                baseX,
                baseY,
                0.0f,
                0.0f,
                176,
                166,
                256,
                256);

        // 2. Draw Custom Slots with simulated items
        // Elytra Slot
        int ex = baseX + elytraX - 1;
        int ey = baseY + elytraY - 1;
        drawSlotBorder(guiGraphics, ex, ey, selectedSlot == 1);
        guiGraphics.fakeItem(new ItemStack(Items.ELYTRA), ex + 1, ey + 1);

        // Firework Slot
        int fx = baseX + fireworkX - 1;
        int fy = baseY + fireworkY - 1;
        drawSlotBorder(guiGraphics, fx, fy, selectedSlot == 2);
        guiGraphics.fakeItem(new ItemStack(Items.FIREWORK_ROCKET), fx + 1, fy + 1);

        // 3. Draw instructions
        guiGraphics.centeredText(this.font, this.title, this.width / 2, baseY - 20, 0xFFFFFFFF);
        guiGraphics.centeredText(this.font, Component.translatable("elytra_slot.screen.config.slot_instructions"),
                this.width / 2, baseY + 167, 0xAAAAAAFF);

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawSlotBorder(GuiGraphicsExtractor gui, int x, int y, boolean selected) {
        int border = selected ? 0xFF00FF00 : 0xFF373737; // Green if selected, dark gray otherwise
        gui.fill(x, y, x + 18, y + 18, 0xFF8B8B8B); // Background
        gui.fill(x, y, x + 18, y + 1, border); // Top
        gui.fill(x, y, x + 1, y + 18, border); // Left
        gui.fill(x, y + 17, x + 18, y + 18, selected ? border : 0xFFFFFFFF); // Bottom
        gui.fill(x + 17, y, x + 18, y + 18, selected ? border : 0xFFFFFFFF); // Right
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        int baseX = getBaseX();
        int baseY = getBaseY();

        // Check if clicked Elytra Slot (18x18)
        if (mouseX >= baseX + elytraX - 1 && mouseX < baseX + elytraX + 17 &&
                mouseY >= baseY + elytraY - 1 && mouseY < baseY + elytraY + 17) {
            selectedSlot = 1;
            isDragging = true;
            dragOffsetX = (int) mouseX - (baseX + elytraX);
            dragOffsetY = (int) mouseY - (baseY + elytraY);
            return true;
        }

        // Check if clicked Firework Slot
        if (mouseX >= baseX + fireworkX - 1 && mouseX < baseX + fireworkX + 17 &&
                mouseY >= baseY + fireworkY - 1 && mouseY < baseY + fireworkY + 17) {
            selectedSlot = 2;
            isDragging = true;
            dragOffsetX = (int) mouseX - (baseX + fireworkX);
            dragOffsetY = (int) mouseY - (baseY + fireworkY);
            return true;
        }

        selectedSlot = 0; // Deselect if clicked elsewhere
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (isDragging) {
            double mouseX = event.x();
            double mouseY = event.y();
            int baseX = getBaseX();
            int baseY = getBaseY();
            int targetX = (int) mouseX - baseX - dragOffsetX;
            int targetY = (int) mouseY - baseY - dragOffsetY;

            // Clamp positions to stay inside screen boundaries
            int minX = -baseX;
            int maxX = this.width - baseX - 18;
            int minY = -baseY;
            int maxY = this.height - baseY - 18;

            targetX = Math.max(minX, Math.min(maxX, targetX));
            targetY = Math.max(minY, Math.min(maxY, targetY));

            if (selectedSlot == 1) {
                elytraX = targetX;
                elytraY = targetY;
            } else if (selectedSlot == 2) {
                fireworkX = targetX;
                fireworkY = targetY;
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        isDragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (selectedSlot != 0) {
            int moveX = 0;
            int moveY = 0;
            int keyCode = event.key();

            if (keyCode == GLFW.GLFW_KEY_UP) {
                moveY = -1;
            } else if (keyCode == GLFW.GLFW_KEY_DOWN) {
                moveY = 1;
            } else if (keyCode == GLFW.GLFW_KEY_LEFT) {
                moveX = -1;
            } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                moveX = 1;
            }

            if (moveX != 0 || moveY != 0) {
                int baseX = getBaseX();
                int baseY = getBaseY();
                int minX = -baseX;
                int maxX = this.width - baseX - 18;
                int minY = -baseY;
                int maxY = this.height - baseY - 18;

                if (selectedSlot == 1) {
                    elytraX = Math.max(minX, Math.min(maxX, elytraX + moveX));
                    elytraY = Math.max(minY, Math.min(maxY, elytraY + moveY));
                } else if (selectedSlot == 2) {
                    fireworkX = Math.max(minX, Math.min(maxX, fireworkX + moveX));
                    fireworkY = Math.max(minY, Math.min(maxY, fireworkY + moveY));
                }
                return true;
            }
        }
        return super.keyPressed(event);
    }
}
