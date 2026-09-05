package com.ogatamizuki.privatechest.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ogatamizuki.privatechest.LockerBlock;
import com.ogatamizuki.privatechest.PrivateChestCommon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.blockentity.state.SkullBlockRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LockerAwareSkullRenderer extends SkullBlockRenderer {
    private static final float LOCKER_HEAD_SCALE = 1.5F;

    public LockerAwareSkullRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void extractRenderState(
            net.minecraft.world.level.block.entity.SkullBlockEntity blockEntity,
            SkullBlockRenderState state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
    }

    @Override
    public void submit(SkullBlockRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.skullType == SkullBlock.Types.PLAYER && isLockerTopHead(state.blockPos)) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.scale(LOCKER_HEAD_SCALE, LOCKER_HEAD_SCALE, LOCKER_HEAD_SCALE);
            poseStack.translate(-0.5F, 0.0F, -0.5F);
            super.submit(state, poseStack, submitNodeCollector, camera);
            poseStack.popPose();
        } else {
            super.submit(state, poseStack, submitNodeCollector, camera);
        }
    }

    @Nullable
    private static Direction getLockerFacing(BlockPos headPos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        BlockState middleState = minecraft.level.getBlockState(headPos.below());
        if (middleState.is(PrivateChestCommon.LOCKER_BLOCK.get())
                && middleState.getValue(LockerBlock.PART) == LockerBlock.LockerPart.MIDDLE) {
            return middleState.getValue(LockerBlock.FACING);
        }
        return null;
    }

    private static boolean isLockerTopHead(BlockPos headPos) {
        return getLockerFacing(headPos) != null;
    }
}
