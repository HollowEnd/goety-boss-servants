package com.saints.goetybossservants.client;

import com.Polarice3.Goety.client.render.block.ModBlockLayer;
import com.Polarice3.Goety.client.render.model.RedstoneMonstrosityHeadModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.saints.goetybossservants.block.MaterialMonstrosityHeadBlock;
import com.saints.goetybossservants.block.MaterialMonstrosityHeadBlockEntity;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import com.saints.goetybossservants.item.MaterialMonstrosityHeadItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Draws the material monstrosity heads with Goety's Redstone Monstrosity head model and the
 * material's skin, placed the same way as Goety's head (block and item).
 */
public final class MaterialHeadRenderer {
    private MaterialHeadRenderer() {
    }

    /** Goety's head model and its root, for the extra pieces on the head (crown, rod, ...). */
    record Head(RedstoneMonstrosityHeadModel model, ModelPart root) {
        static Head bake(ModelPart root) {
            MonstrosityExtras.hideGoetyEyes(root, true);
            return new Head(new RedstoneMonstrosityHeadModel(root), root);
        }
    }

    private static Head model() {
        return Head.bake(Minecraft.getInstance().getEntityModels().bakeLayer(ModBlockLayer.REDSTONE_MONSTROSITY_HEAD));
    }

    static void render(Head head, MonstrosityMaterial material, float yRot, float scale, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.scale(-scale, -scale, scale);
        head.model().setupAnim(0.0f, yRot, 0.0f);
        MonstrosityExtras.reshape(head.root(), true, material);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCullZOffset(MaterialMonstrosityRenderer.texture(material, "")));
        head.model().renderToBuffer(poseStack, buffer, light, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        MonstrosityExtras.renderEyes(material, head.root(), true, poseStack, buffer, light, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        MonstrosityExtras.render(material, head.root(), true, false, 0.0f, 0.0f, 0.0f, 0.0f, poseStack, buffers, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    /** The head item, shrunk to item size; in inventories tilted like a block, as Goety draws its own head item. */
    static void renderItem(Head head, ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.5f, 0.5f, 0.5f);
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0f));
            poseStack.mulPose(Axis.YN.rotationDegrees(-45.0f));
            poseStack.translate(-0.5f, -0.5f, -0.5f);
            poseStack.translate(0.0f, 0.25f, 0.0f);
        }
        render(head, MaterialMonstrosityHeadItem.getMaterial(stack), 180.0f, 0.25f, poseStack, buffers, light);
        poseStack.popPose();
    }

    public static class BlockRenderer implements BlockEntityRenderer<MaterialMonstrosityHeadBlockEntity> {
        private final Head model;

        public BlockRenderer(BlockEntityRendererProvider.Context context) {
            this.model = Head.bake(context.bakeLayer(ModBlockLayer.REDSTONE_MONSTROSITY_HEAD));
        }

        @Override
        public void render(MaterialMonstrosityHeadBlockEntity head, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffers, int light, int overlay) {
            float yRot = head.getBlockState().getValue(MaterialMonstrosityHeadBlock.ROTATION) * 22.5f;
            MaterialHeadRenderer.render(this.model, head.getMaterial(), yRot, 1.0f, poseStack, buffers, light);
        }
    }

    /** The head item (its model is builtin/entity), shrunk to item size like Goety's. */
    public static class ItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
        private Head model;

        @Override
        public void render(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
            if (this.model == null) {
                this.model = model();
            }
            MaterialHeadRenderer.renderItem(this.model, stack, context, poseStack, buffers, light);
        }
    }
}
