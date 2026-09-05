package com.ogatamizuki.instantstructure.fabric;

import com.ogatamizuki.instantstructure.*;
import com.ogatamizuki.instantstructure.client.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import com.mojang.blaze3d.vertex.PoseStack;

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
            context.client().execute(() -> context.client().gui.setScreen(new InstantBuilderScreen(payload.templates())));
        });

        ClientPlayNetworking.registerGlobalReceiver(TemplatePreviewPayload.TYPE, (payload, context) -> {
            // 設置モード外（素材確認など）でもキャッシュする。apply は loader 側で active 時のみ行う。
            context.client().execute(() -> ClientPreviewLoader.storeFromPayload(
                    payload.category(),
                    payload.templateName(),
                    payload.blocks()
            ));
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
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            PoseStack poseStack = context.poseStack();
            SubmitNodeCollector collector = context.submitNodeCollector();
            float lineWidth = mc.getWindow().getAppropriateLineWidth();

            if (ClientSelectionRegistry.isHoldingMarker(mc) && ClientSelectionRegistry.hasStart
                    && ClientSelectionRegistry.pos1 != null) {
                BlockPos pos2 = ClientSelectionRegistry.resolvePreviewEnd(mc);
                if (pos2 != null) {
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

                    SelectionWireframeRenderer.drawBox(
                            ClientSelectionRegistry.pos1,
                            pos2,
                            r, g, b, 1.0F,
                            lineWidth
                    );
                }
            }

            if (ClientPlacementRegistry.active) {
                BlockPos targetPos = ClientPlacementRegistry.resolvePlacementOrigin(mc);
                if (targetPos != null) {
                    if (ClientPlacementRegistry.shouldRenderDetailedGhosts()) {
                        if (ClientPlacementRegistry.previewCache != null) {
                            GhostBlockRenderer.renderCached(
                                    poseStack,
                                    collector,
                                    ClientPlacementRegistry.previewCache,
                                    mc.gameRenderer.mainCamera().position()
                            );
                        } else if (!ClientPlacementRegistry.previewBlocks.isEmpty()) {
                            GhostBlockRenderer.renderBlockModels(
                                    poseStack,
                                    collector,
                                    targetPos,
                                    ClientPlacementRegistry.placementTransform(),
                                    ClientPlacementRegistry.previewBlocks
                            );
                        }
                    }
                    PlacementPreviewRenderer.renderWireframe(
                            targetPos,
                            ClientPlacementRegistry.placementTransform(),
                            ClientPlacementRegistry.sizeX,
                            ClientPlacementRegistry.sizeY,
                            ClientPlacementRegistry.sizeZ,
                            ClientPlacementRegistry.previewBlocks,
                            lineWidth,
                            ClientPlacementRegistry.tentativelyConfirmed
                    );

                    BlockPos anchorBlock = ClientPlacementRegistry.tentativelyConfirmed
                            ? ClientPlacementRegistry.lockedAnchor
                            : ClientPlacementRegistry.resolveCrosshairAnchor(mc);
                    if (anchorBlock != null) {
                        PlacementPreviewRenderer.renderAnchorWireframe(
                                anchorBlock,
                                lineWidth
                        );
                    }
                }
            }
        });
    }
}
