package com.ogatamizuki.elytraslot.mixin;

import com.ogatamizuki.elytraslot.ElytraSlot;
import com.ogatamizuki.elytraslot.FireworkSlot;
import com.ogatamizuki.elytraslot.client.CustomSlotBorderRenderer;
import com.ogatamizuki.elytraslot.client.CustomSlotClickHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class InventoryScreenMixin extends Screen {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected AbstractContainerMenu menu;

    protected InventoryScreenMixin(Component title) {
        super(title);
    }

    private boolean elytraSlot$usesCustomSlotBorders() {
        if ((Object) this instanceof InventoryScreen) {
            return true;
        }
        return (Object) this instanceof CreativeModeInventoryScreen creativeScreen && creativeScreen.isInventoryOpen();
    }

    @Inject(method = "extractContents", at = @At("HEAD"))
    private void elytraSlot$drawCustomSlotBackgrounds(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (elytraSlot$usesCustomSlotBorders()) {
            CustomSlotBorderRenderer.drawBackgrounds(gui, this.leftPos, this.topPos, this.menu.slots, this.width, this.height);
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void elytraSlot$handleSurvivalCustomSlotClick(
            Slot slot,
            int slotId,
            int mouseButton,
            ContainerInput containerInput,
            CallbackInfo ci) {
        if (!((Object) this instanceof InventoryScreen)) {
            return;
        }
        if (slot == null || !(slot instanceof ElytraSlot || slot instanceof FireworkSlot)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) {
            return;
        }

        boolean handled = CustomSlotClickHandler.handle(
                this.menu,
                slot,
                mouseButton,
                containerInput,
                mc,
                customSlot -> mc.gameMode.handleContainerInput(
                        this.menu.containerId,
                        customSlot.index,
                        mouseButton,
                        containerInput,
                        mc.player
                )
        );

        if (handled) {
            ci.cancel();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (elytraSlot$usesCustomSlotBorders()) {
            double mouseX = event.x();
            double mouseY = event.y();
            int button = event.button();

            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (button == 1 && mc.options.keyShift.isDown()) { // Right-click + Shift
                net.minecraft.world.inventory.Slot clicked = null;
                for (net.minecraft.world.inventory.Slot slot : this.menu.slots) {
                    if (!CustomSlotBorderRenderer.isCustomAttachmentSlot(slot)) {
                        continue;
                    }
                    int sx = this.leftPos + slot.x;
                    int sy = this.topPos + slot.y;
                    if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                        clicked = slot;
                        break;
                    }
                }

                if (clicked != null && clicked.hasItem()) {
                    if (mc.gameMode != null && mc.player != null) {
                        mc.gameMode.handleContainerInput(
                                this.menu.containerId,
                                clicked.index,
                                0,
                                net.minecraft.world.inventory.ContainerInput.QUICK_MOVE,
                                (net.minecraft.world.entity.player.Player) mc.player
                        );
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }
}
