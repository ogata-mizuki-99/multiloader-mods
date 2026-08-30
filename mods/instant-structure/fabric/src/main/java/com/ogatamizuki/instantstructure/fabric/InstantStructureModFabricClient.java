package com.ogatamizuki.instantstructure.fabric;

import com.ogatamizuki.instantstructure.*;
import com.ogatamizuki.instantstructure.client.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public class InstantStructureModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        InstantStructureCommon.LOGGER.info("Instant Structure Mod (Fabric Client) Initializing...");
        InstantStructurePlatform.sendToServer = ClientPlayNetworking::send;

        // HUD Elements
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CROSSHAIR,
                InstantStructureCommon.id("selection_hud"),
                SelectionHudOverlay::render
        );
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CROSSHAIR,
                InstantStructureCommon.id("builder_placement_hud"),
                BuilderPlacementHudOverlay::render
        );

        // Network Handlers
        ClientPlayNetworking.registerGlobalReceiver(SelectionSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (payload.hasStart()) {
                    BlockPos pos1 = new BlockPos(payload.x1(), payload.y1(), payload.z1());
                    BlockPos pos2 = payload.hasBoth()
                            ? new BlockPos(payload.x2(), payload.y2(), payload.z2())
                            : null;
                    ClientSelectionRegistry.apply(payload.hasStart(), payload.hasBoth(), payload.confirmed(), pos1, pos2);
                } else {
                    ClientSelectionRegistry.clear();
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(TemplatesListPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> context.client().setScreen(new InstantBuilderScreen(payload.templates())));
        });

        ClientPlayNetworking.registerGlobalReceiver(TemplatePreviewPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (ClientPlacementRegistry.active
                        && payload.category().equals(ClientPlacementRegistry.category)
                        && payload.templateName().equals(ClientPlacementRegistry.templateName)) {
                    ClientPreviewLoader.storeFromPayload(
                            payload.category(),
                            payload.templateName(),
                            payload.blocks()
                    );
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(OpenExportDialogPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> StructureExportScreen.open(
                    new BlockPos(payload.x1(), payload.y1(), payload.z1()),
                    new BlockPos(payload.x2(), payload.y2(), payload.z2())
            ));
        });

        ClientPlayNetworking.registerGlobalReceiver(BuildResultPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (!ClientPlacementRegistry.active) {
                    return;
                }
                if (payload.result() == BuildResultPayload.SUCCESS) {
                    ClientPlacementRegistry.reset();
                } else {
                    ClientPlacementRegistry.onBuildRejected();
                }
            });
        });

        // World Render Events (Selection Box & Placement Preview)
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(
                ClientPlacementRegistry::syncHeldItem
        );
        net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.BEFORE_GIZMOS.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            com.mojang.blaze3d.vertex.PoseStack poseStack = context.poseStack();
            net.minecraft.world.phys.Vec3 cam = mc.gameRenderer.getMainCamera().position();
            float lineWidth = mc.getWindow().getAppropriateLineWidth();

            if (ClientSelectionRegistry.isHoldingMarker(mc) && ClientSelectionRegistry.hasStart && ClientSelectionRegistry.pos1 != null) {
                BlockPos pos2 = ClientSelectionRegistry.resolvePreviewEnd(mc);
                if (pos2 != null) {
                    poseStack.pushPose();
                    poseStack.translate(-cam.x, -cam.y, -cam.z);

                    float r;
                    float g;
                    float b;
                    if (ClientSelectionRegistry.confirmed) {
                        r = 0.0F;
                        g = 1.0F;
                        b = 0.0F;
                    } else if (ClientSelectionRegistry.hasBoth) {
                        r = 1.0F;
                        g = 0.85F;
                        b = 0.0F;
                    } else {
                        r = 1.0F;
                        g = 1.0F;
                        b = 0.35F;
                    }

                    net.minecraft.client.renderer.MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
                    com.mojang.blaze3d.vertex.VertexConsumer consumer = bufferSource.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.lines());
                    SelectionWireframeRenderer.drawBox(
                            poseStack,
                            consumer,
                            ClientSelectionRegistry.pos1,
                            pos2,
                            r, g, b, 1.0F,
                            lineWidth,
                            cam
                    );
                    bufferSource.endBatch(net.minecraft.client.renderer.rendertype.RenderTypes.lines());
                    poseStack.popPose();
                }
            }

            if (ClientPlacementRegistry.isPlacementVisible(mc)) {
                BlockPos targetPos = ClientPlacementRegistry.resolvePlacementOrigin(mc);
                if (targetPos != null) {
                    poseStack.pushPose();
                    poseStack.translate(-cam.x, -cam.y, -cam.z);

                    net.minecraft.client.renderer.MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
                    if (ClientPlacementRegistry.shouldRenderDetailedGhosts()) {
                        if (ClientPlacementRegistry.previewCache != null) {
                            GhostBlockRenderer.renderCached(
                                    poseStack,
                                    bufferSource,
                                    ClientPlacementRegistry.previewCache,
                                    cam
                            );
                        } else if (!ClientPlacementRegistry.previewBlocks.isEmpty()) {
                            GhostBlockRenderer.renderBlockModels(
                                    poseStack,
                                    bufferSource,
                                    targetPos,
                                    ClientPlacementRegistry.placementTransform(),
                                    ClientPlacementRegistry.previewBlocks
                            );
                        }
                    }
                    PlacementPreviewRenderer.renderWireframe(
                            poseStack,
                            bufferSource,
                            targetPos,
                            ClientPlacementRegistry.placementTransform(),
                            ClientPlacementRegistry.sizeX,
                            ClientPlacementRegistry.sizeY,
                            ClientPlacementRegistry.sizeZ,
                            ClientPlacementRegistry.previewBlocks,
                            lineWidth,
                            cam,
                            ClientPlacementRegistry.tentativelyConfirmed
                    );

                    BlockPos anchorBlock = ClientPlacementRegistry.tentativelyConfirmed
                            ? ClientPlacementRegistry.lockedAnchor
                            : ClientPlacementRegistry.resolveCrosshairAnchor(mc);
                    if (anchorBlock != null) {
                        PlacementPreviewRenderer.renderAnchorWireframe(
                                poseStack,
                                bufferSource,
                                anchorBlock,
                                lineWidth,
                                cam
                        );
                    }

                    poseStack.popPose();
                }
            }
        });
    }
}
