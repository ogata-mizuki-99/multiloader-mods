package com.ogatamizuki.instantstructure.neoforge;

import com.ogatamizuki.instantstructure.*;
import com.ogatamizuki.instantstructure.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = InstantStructureCommon.MODID, dist = Dist.CLIENT)
public class InstantStructureModNeoForgeClient {

    public InstantStructureModNeoForgeClient(ModContainer container) {
        InstantStructurePlatform.sendToServer = payload -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                mc.getConnection().send(payload);
            }
        };
        IEventBus modEventBus = container.getEventBus();
        modEventBus.addListener(this::registerClientPayloads);
        modEventBus.addListener(InstantStructureModNeoForgeClient::onRegisterGuiLayers);
        container.registerExtensionPoint(
                net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                InstantStructureConfigScreen::new
        );

        NeoForge.EVENT_BUS.register(this);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.CROSSHAIR,
                Identifier.fromNamespaceAndPath(InstantStructureCommon.MODID, "selection_hud"),
                SelectionHudOverlay::render
        );
        event.registerAbove(
                VanillaGuiLayers.CROSSHAIR,
                Identifier.fromNamespaceAndPath(InstantStructureCommon.MODID, "builder_placement_hud"),
                BuilderPlacementHudOverlay::render
        );
    }

    private void registerClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(SelectionSyncPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
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

        event.register(TemplatesListPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> mc.setScreen(new InstantBuilderScreen(payload.templates())));
        });

        event.register(TemplatePreviewPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
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

        event.register(OpenExportDialogPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> StructureExportScreen.open(
                    new BlockPos(payload.x1(), payload.y1(), payload.z1()),
                    new BlockPos(payload.x2(), payload.y2(), payload.z2())
            ));
        });

        event.register(BuildResultPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
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
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        ClientPlacementRegistry.syncHeldItem(Minecraft.getInstance());
    }

    @SubscribeEvent
    public void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        if (!mc.options.keyShift.isDown()) {
            return;
        }

        int delta = event.getScrollDeltaY() > 0 ? 1 : -1;
        if (ClientSelectionRegistry.isHoldingMarker(mc)) {
            event.setCanceled(true);
            mc.getConnection().send(new AdjustHeightPayload(delta));
            return;
        }
        if (ClientPlacementRegistry.active
                && ClientPlacementRegistry.isHoldingBuilder(mc)
                && !ClientPlacementRegistry.tentativelyConfirmed) {
            event.setCanceled(true);
            ClientPlacementRegistry.placementYOffset += delta;
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        if (event.getAction() == GLFW.GLFW_PRESS && event.getKey() == GLFW.GLFW_KEY_ESCAPE) {
            if (ClientPlacementRegistry.active) {
                ClientPlacementRegistry.reset();
            } else if (ClientSelectionRegistry.isHoldingMarker(mc) && ClientSelectionRegistry.hasStart) {
                // マーカー所持中かつ選択始点がある場合、Escで選択クリアパケットをサーバーに送る
                if (mc.getConnection() != null) {
                    mc.getConnection().send(new com.ogatamizuki.instantstructure.ExportCancelPayload(true));
                }
                ClientSelectionRegistry.clear();
            }
        }
    }

    @SubscribeEvent
    public void onScreenOpening(net.neoforged.neoforge.client.event.ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof net.minecraft.client.gui.screens.PauseScreen) {
            Minecraft mc = Minecraft.getInstance();
            boolean handled = false;
            if (ClientPlacementRegistry.active) {
                ClientPlacementRegistry.reset();
                handled = true;
            } else if (ClientSelectionRegistry.isHoldingMarker(mc) && ClientSelectionRegistry.hasStart) {
                if (mc.getConnection() != null) {
                    mc.getConnection().send(new com.ogatamizuki.instantstructure.ExportCancelPayload(true));
                }
                ClientSelectionRegistry.clear();
                handled = true;
            }
            if (handled) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        // Builder item handling
        if (ClientPlacementRegistry.active && ClientPlacementRegistry.isHoldingBuilder(mc)) {
            if (event.getAction() == GLFW.GLFW_PRESS) {
                if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    if (mc.options.keyShift.isDown()) {
                        if (ClientPlacementRegistry.tentativelyConfirmed) {
                            ClientPlacementRegistry.clearTentative();
                        } else {
                            ClientPlacementRegistry.rotation = (ClientPlacementRegistry.rotation + 90) % 360;
                        }
                        event.setCanceled(true);
                        return;
                    }

                    if (!ClientPlacementRegistry.tentativelyConfirmed) {
                        if (ClientPlacementRegistry.lockTentativeAnchor(mc)) {
                            event.setCanceled(true);
                        }
                        return;
                    }

                    BlockPos targetPos = ClientPlacementRegistry.resolvePlacementOrigin(mc);
                    if (targetPos != null) {
                        if (GhostBlockRenderer.isPlayerInsidePreview(
                                mc,
                                targetPos,
                                ClientPlacementRegistry.placementTransform(),
                                ClientPlacementRegistry.sizeX,
                                ClientPlacementRegistry.sizeY,
                                ClientPlacementRegistry.sizeZ
                        )) {
                            mc.player.sendSystemMessage(
                                    Component.translatable("instant_structure.message.build_player_inside")
                            );
                            ClientPlacementRegistry.onBuildRejected();
                            event.setCanceled(true);
                            return;
                        }
                        if (mc.getConnection() != null) {
                            boolean hasAnchor = ClientPlacementRegistry.lockedAnchor != null;
                            BlockPos anchor = hasAnchor ? ClientPlacementRegistry.lockedAnchor : BlockPos.ZERO;
                            mc.getConnection().send(new BuildRequestPayload(
                                    ClientPlacementRegistry.category,
                                    ClientPlacementRegistry.templateName,
                                    targetPos.getX(), targetPos.getY(), targetPos.getZ(),
                                    ClientPlacementRegistry.rotation,
                                    ClientPlacementRegistry.mirrorLeftRight,
                                    ClientPlacementRegistry.mirrorFrontBack,
                                    hasAnchor,
                                    anchor.getX(), anchor.getY(), anchor.getZ()
                            ));
                            event.setCanceled(true);
                        }
                    }
                } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    ClientPlacementRegistry.reset();
                    event.setCanceled(true);
                }
            }
        }
        // Marker item handling
        else if (ClientSelectionRegistry.isHoldingMarker(mc)) {
            if (event.getAction() == GLFW.GLFW_PRESS && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (ClientSelectionRegistry.hasStart) {
                    if (mc.getConnection() != null) {
                        mc.getConnection().send(new com.ogatamizuki.instantstructure.ExportCancelPayload(true));
                    }
                    ClientSelectionRegistry.clear();
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        float lineWidth = mc.getWindow().getAppropriateLineWidth();

        if (ClientSelectionRegistry.isHoldingMarker(mc) && ClientSelectionRegistry.hasStart
                && ClientSelectionRegistry.pos1 != null) {
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

                MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
                VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.lines());
                SelectionWireframeRenderer.drawBox(
                        poseStack,
                        consumer,
                        ClientSelectionRegistry.pos1,
                        pos2,
                        r, g, b, 1.0F,
                        lineWidth,
                        cam
                );
                bufferSource.endBatch(RenderTypes.lines());
                poseStack.popPose();
            }
        }

        if (ClientPlacementRegistry.isPlacementVisible(mc)) {
            BlockPos targetPos = ClientPlacementRegistry.resolvePlacementOrigin(mc);
            if (targetPos != null) {
                poseStack.pushPose();
                poseStack.translate(-cam.x, -cam.y, -cam.z);

                MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
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
    }
}
