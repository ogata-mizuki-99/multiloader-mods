package com.ogatamizuki.deconstructor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.EnchantTableRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("deprecation")
public class EnchantmentManagerRenderer implements BlockEntityRenderer<EnchantmentManagerBlockEntity, EnchantTableRenderState> {
    public static final SpriteId ORANGE_BOOK_TEXTURE = new SpriteId(
        TextureAtlas.LOCATION_BLOCKS,
        Identifier.fromNamespaceAndPath("deconstructor", "entity/enchant_manager_book")
    );

    private final BookModel bookModel;
    private final SpriteGetter sprites;

    public EnchantmentManagerRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public EnchantTableRenderState createRenderState() {
        return new EnchantTableRenderState();
    }

    @Override
    public void extractRenderState(EnchantmentManagerBlockEntity blockEntity, EnchantTableRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(blockEntity, state, crumblingOverlay);
        state.lightCoords = net.minecraft.client.renderer.LevelRenderer.getLightCoords(blockEntity.getLevel(), blockEntity.getBlockPos().above());
        state.time = (float)blockEntity.time + partialTick;

        float f1;
        for (f1 = blockEntity.rot - blockEntity.oRot; f1 >= (float)Math.PI; f1 -= ((float)Math.PI * 2F)) {
        }
        while (f1 < -(float)Math.PI) {
            f1 += ((float)Math.PI * 2F);
        }
        state.yRot = blockEntity.oRot + f1 * partialTick;

        state.flip = Mth.lerp(partialTick, blockEntity.oFlip, blockEntity.flip);
        state.open = Mth.lerp(partialTick, blockEntity.oOpen, blockEntity.open);
    }

    @Override
    public void submit(EnchantTableRenderState state, PoseStack poseStack, SubmitNodeCollector queue, CameraRenderState cameraState) {
        poseStack.pushPose();
        
        // Enchantment table is at 0.75F, our table top is at 14/16 block space (0.875F).
        // Shift up by 0.125F
        poseStack.translate(0.5F, 0.75F + 0.125F, 0.5F);
        
        float bob = (float)Math.sin(state.time * 0.1F) * 0.01F + 0.1F;
        poseStack.translate(0.0F, bob, 0.0F);
        
        float yRot = state.yRot;
        poseStack.mulPose(Axis.YP.rotation(-yRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(80.0F));
        
        float flip1 = Mth.frac(state.flip + 0.25F) * 1.6F - 0.3F;
        float flip2 = Mth.frac(state.flip + 0.75F) * 1.6F - 0.3F;
        float clampFlip1 = Mth.clamp(flip1, 0.0F, 1.0F);
        float clampFlip2 = Mth.clamp(flip2, 0.0F, 1.0F);
        
        BookModel.State bookState = BookModel.State.forAnimation(state.time, clampFlip1, clampFlip2, state.open);
        
        queue.submitModel(
            this.bookModel,
            bookState,
            poseStack,
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            -1,
            ORANGE_BOOK_TEXTURE,
            this.sprites,
            0,
            state.breakProgress
        );
        
        poseStack.popPose();
    }
}
