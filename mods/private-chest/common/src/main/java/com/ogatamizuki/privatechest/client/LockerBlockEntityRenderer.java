package com.ogatamizuki.privatechest.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ogatamizuki.privatechest.LockerBlock;
import com.ogatamizuki.privatechest.LockerBlockEntity;
import com.ogatamizuki.privatechest.PrivateChestCommon;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public class LockerBlockEntityRenderer implements BlockEntityRenderer<LockerBlockEntity, LockerBlockRenderState> {
    /** Atlas: locker_atlas.png (256×64). Each tile is 64×64. */
    public static final int ATLAS_WIDTH = 256;
    public static final int ATLAS_HEIGHT = 64;
    public static final int TILE_SIZE = 64;
    /** 16 model units = one 64×64 tile. */
    public static final float TEX_SCALE = 16.0F / TILE_SIZE;
    public static final float WALL_THICKNESS = 1.0F;

    /** Pixel origins inside locker_atlas.png. */
    public static final int UV_SIDE_PIXEL_U = 0;
    public static final int UV_VENT_PIXEL_U = 64;
    public static final int UV_KNOB_PIXEL_U = 128;
    public static final int UV_PIXEL_V = 0;

    private static final Identifier LOCKER_ATLAS = Identifier.fromNamespaceAndPath("privatechest",
            "textures/block/locker_atlas.png");

    private static final float DOOR_OPEN_DEGREES = 110.0F;
    private static final float DOOR_HINGE_X = 16.0F;
    private static final float DOOR_Z = 15.0F;
    private static final float BLOCK_SIZE = 16.0F;
    private static final float INTERIOR_MAX = BLOCK_SIZE - WALL_THICKNESS;

    private final LockerModel model;

    public LockerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new LockerModel(LockerModel.createLayer().bakeRoot());
    }

    @Override
    public LockerBlockRenderState createRenderState() {
        return new LockerBlockRenderState();
    }

    @Override
    public void extractRenderState(LockerBlockEntity blockEntity, LockerBlockRenderState state, float partialTicks,
            net.minecraft.world.phys.Vec3 cameraPosition,
            net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = blockEntity.getBlockState();
        if (blockState.is(PrivateChestCommon.LOCKER_BLOCK.get())) {
            state.facing = blockState.getValue(LockerBlock.FACING);
        }
        state.openProgress = blockEntity.getOpenProgress(partialTicks);
    }

    @Override
    public void submit(LockerBlockRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot()));
        poseStack.scale(1.0F, -1.0F, 1.0F);
        poseStack.translate(-0.5D, -2.0D, -0.5D);

        submitNodeCollector.submitModel(
                this.model,
                state,
                poseStack,
                RenderTypes.entityCutout(LOCKER_ATLAS),
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0,
                state.breakProgress);

        poseStack.popPose();
    }

    private static float easeDoorOpen(float openProgress) {
        float open = 1.0F - openProgress;
        return 1.0F - open * open * open;
    }

    public static class LockerModel extends Model<LockerBlockRenderState> {
        public final ModelPart root;
        public final ModelPart doorHinge;

        public LockerModel(ModelPart root) {
            super(root, RenderTypes::entityCutout);
            this.root = root;
            this.doorHinge = root.getChild("door_hinge");
        }

        @Override
        public void setupAnim(LockerBlockRenderState state) {
            super.setupAnim(state);
            float open = easeDoorOpen(state.openProgress);
            this.doorHinge.yRot = (float) Math.toRadians(open * DOOR_OPEN_DEGREES);
        }

        private static void addVisibleBox(PartDefinition parent, String name, float x, float y, float z, float w,
                float h, float d, Direction face, int pixelU, int pixelV) {
            int u = LockerAtlasUv.u(face, w, h, d, pixelU);
            int v = LockerAtlasUv.v(face, w, h, d, pixelV);
            parent.addOrReplaceChild(name,
                    LockerCubeBuilder.scaledVisible(u, v, x, y, z, w, h, d, false, TEX_SCALE, EnumSet.of(face)),
                    PartPose.ZERO);
        }

        private static void addInteriorPanel(PartDefinition parent, String name, float x, float y, float z,
                Direction face, float size) {
            addVisibleBox(parent, name, x, y, z, size, WALL_THICKNESS, size, face, UV_SIDE_PIXEL_U, UV_PIXEL_V);
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();

            float sideDepth = 16.0F;
            float sideHeight = 16.0F;

            addVisibleBox(root, "left_lower", 0.0F, 0.0F, 0.0F, WALL_THICKNESS, sideHeight, sideDepth,
                    Direction.WEST, UV_SIDE_PIXEL_U, UV_PIXEL_V);
            addVisibleBox(root, "left_upper", 0.0F, 16.0F, 0.0F, WALL_THICKNESS, sideHeight, sideDepth,
                    Direction.WEST, UV_SIDE_PIXEL_U, UV_PIXEL_V);
            addVisibleBox(root, "right_lower", INTERIOR_MAX, 0.0F, 0.0F, WALL_THICKNESS, sideHeight, sideDepth,
                    Direction.EAST, UV_SIDE_PIXEL_U, UV_PIXEL_V);
            addVisibleBox(root, "right_upper", INTERIOR_MAX, 16.0F, 0.0F, WALL_THICKNESS, sideHeight, sideDepth,
                    Direction.EAST, UV_SIDE_PIXEL_U, UV_PIXEL_V);

            addVisibleBox(root, "back_lower", 0.0F, 0.0F, 0.0F, 16.0F, sideHeight, WALL_THICKNESS,
                    Direction.NORTH, UV_SIDE_PIXEL_U, UV_PIXEL_V);
            addVisibleBox(root, "back_upper", 0.0F, 16.0F, 0.0F, 16.0F, sideHeight, WALL_THICKNESS,
                    Direction.NORTH, UV_SIDE_PIXEL_U, UV_PIXEL_V);

            addInteriorPanel(root, "bottom", 0.0F, 0.0F, 0.0F, Direction.UP, BLOCK_SIZE);
            addInteriorPanel(root, "top", 0.0F, 31.0F, 0.0F, Direction.DOWN, BLOCK_SIZE);
            addInteriorPanel(root, "shelf_down", 0.0F, 4.0F, 0.0F, Direction.DOWN, BLOCK_SIZE);

            PartDefinition doorHinge = root.addOrReplaceChild("door_hinge",
                    CubeListBuilder.create(),
                    PartPose.offset(DOOR_HINGE_X, 0.0F, DOOR_Z));

            doorHinge.addOrReplaceChild("door_lower",
                    LockerCubeBuilder.scaledVisible(
                            LockerAtlasUv.knobU(), LockerAtlasUv.knobV(),
                            -16.0F, 0.0F, 0.0F, 16.0F, 16.0F, WALL_THICKNESS,
                            true, TEX_SCALE, EnumSet.of(Direction.SOUTH)),
                    PartPose.ZERO);

            doorHinge.addOrReplaceChild("door_upper",
                    LockerCubeBuilder.scaledVisible(
                            LockerAtlasUv.ventU(), LockerAtlasUv.ventV(),
                            -16.0F, 16.0F, 0.0F, 16.0F, 16.0F, WALL_THICKNESS,
                            true, TEX_SCALE, EnumSet.of(Direction.SOUTH)),
                    PartPose.ZERO);

            return LayerDefinition.create(mesh, ATLAS_WIDTH, ATLAS_HEIGHT);
        }
    }
}
