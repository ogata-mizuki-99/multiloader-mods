package com.ogatamizuki.instantstructure.fabric.mixin;

import com.ogatamizuki.instantstructure.AdjustHeightPayload;
import com.ogatamizuki.instantstructure.BuildRequestPayload;
import com.ogatamizuki.instantstructure.ExportCancelPayload;
import com.ogatamizuki.instantstructure.client.ClientPlacementRegistry;
import com.ogatamizuki.instantstructure.client.ClientSelectionRegistry;
import com.ogatamizuki.instantstructure.client.GhostBlockRenderer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void instantStructure$onMouseScroll(long windowPointer, double xoffset, double yoffset, CallbackInfo ci) {
        if (this.minecraft.player == null || this.minecraft.screen != null) {
            return;
        }
        if (!this.minecraft.options.keyShift.isDown()) {
            return;
        }

        int delta = yoffset > 0 ? 1 : -1;
        if (ClientSelectionRegistry.isHoldingMarker(this.minecraft)) {
            ci.cancel();
            ClientPlayNetworking.send(new AdjustHeightPayload(delta));
            return;
        }
        if (ClientPlacementRegistry.active
                && ClientPlacementRegistry.isHoldingBuilder(this.minecraft)
                && !ClientPlacementRegistry.tentativelyConfirmed) {
            ci.cancel();
            ClientPlacementRegistry.placementYOffset += delta;
        }
    }

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void instantStructure$onMouseButton(long windowPointer, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        if (this.minecraft.player == null || this.minecraft.screen != null) {
            return;
        }
        if (action != GLFW.GLFW_PRESS) {
            return;
        }

        // Builder item handling
        if (ClientPlacementRegistry.active && ClientPlacementRegistry.isHoldingBuilder(this.minecraft)) {
            if (buttonInfo.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                if (this.minecraft.options.keyShift.isDown()) {
                    if (ClientPlacementRegistry.tentativelyConfirmed) {
                        ClientPlacementRegistry.clearTentative();
                    } else {
                        ClientPlacementRegistry.rotation = (ClientPlacementRegistry.rotation + 90) % 360;
                    }
                    ci.cancel();
                    return;
                }

                if (!ClientPlacementRegistry.tentativelyConfirmed) {
                    if (ClientPlacementRegistry.lockTentativeAnchor(this.minecraft)) {
                        ci.cancel();
                    }
                    return;
                }

                BlockPos targetPos = ClientPlacementRegistry.resolvePlacementOrigin(this.minecraft);
                if (targetPos != null) {
                    if (GhostBlockRenderer.isPlayerInsidePreview(
                            this.minecraft,
                            targetPos,
                            ClientPlacementRegistry.placementTransform(),
                            ClientPlacementRegistry.sizeX,
                            ClientPlacementRegistry.sizeY,
                            ClientPlacementRegistry.sizeZ
                    )) {
                        this.minecraft.player.sendSystemMessage(
                                Component.translatable("instant_structure.message.build_player_inside")
                        );
                        ClientPlacementRegistry.onBuildRejected();
                        ci.cancel();
                        return;
                    }

                    boolean hasAnchor = ClientPlacementRegistry.lockedAnchor != null;
                    BlockPos anchor = hasAnchor ? ClientPlacementRegistry.lockedAnchor : BlockPos.ZERO;
                    ClientPlayNetworking.send(new BuildRequestPayload(
                            ClientPlacementRegistry.category,
                            ClientPlacementRegistry.templateName,
                            targetPos.getX(), targetPos.getY(), targetPos.getZ(),
                            ClientPlacementRegistry.rotation,
                            ClientPlacementRegistry.mirrorLeftRight,
                            ClientPlacementRegistry.mirrorFrontBack,
                            hasAnchor,
                            anchor.getX(), anchor.getY(), anchor.getZ()
                    ));
                    ci.cancel();
                }
            } else if (buttonInfo.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                ClientPlacementRegistry.reset();
                ci.cancel();
            }
        }
        // Marker item handling
        else if (ClientSelectionRegistry.isHoldingMarker(this.minecraft)) {
            if (buttonInfo.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && ClientSelectionRegistry.hasStart) {
                ClientPlayNetworking.send(new ExportCancelPayload(true));
                ClientSelectionRegistry.clear();
                ci.cancel();
            }
        }
    }
}
