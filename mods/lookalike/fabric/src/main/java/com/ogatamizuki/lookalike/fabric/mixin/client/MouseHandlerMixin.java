package com.ogatamizuki.lookalike.fabric.mixin.client;

import com.ogatamizuki.lookalike.LookalikeCommon;
import com.ogatamizuki.lookalike.NetworkPayloads.DisguiseRequestPayload;
import com.ogatamizuki.lookalike.client.LookalikeRadialSession;
import com.ogatamizuki.lookalike.client.ScanHistoryEditScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void lookalike$onMouseButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        if (this.minecraft.player == null || this.minecraft.gui.screen() != null) {
            return;
        }

        if (action != GLFW.GLFW_PRESS) {
            return;
        }

        int button = buttonInfo.button();

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && LookalikeRadialSession.isActive()) {
            ci.cancel();

            LookalikeRadialSession.refreshHoverForInput(this.minecraft);

            if (LookalikeRadialSession.isMouseOverCenter(this.minecraft)) {
                ScanHistoryEditScreen.open();
                return;
            }

            if (LookalikeRadialSession.getHoveredIndex() < 0) {
                return;
            }

            LookalikeRadialSession.confirmSelection(this.minecraft);
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void lookalike$onMouseScroll(long window, double xoffset, double yoffset, CallbackInfo ci) {
        if (LookalikeRadialSession.isActive() && this.minecraft.gui.screen() == null) {
            ci.cancel();
        }
    }
}
