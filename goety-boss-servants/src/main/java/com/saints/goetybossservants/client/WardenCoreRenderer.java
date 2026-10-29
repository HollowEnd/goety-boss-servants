package com.saints.goetybossservants.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.block.WardenCoreBlock;
import com.saints.goetybossservants.block.WardenCoreBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * The souls on the Warden Core's top glowing with each heartbeat, drawn like the warden's heart
 * layer (translucent and emissive, its alpha the heartbeat): the warden's heart texture on the same
 * 16x16 of its chest that the block's top shows, less what its ribcage covers (see
 * WardenCoreTextures), so every soul pixel lands on its faint copy in the block's texture.
 */
public class WardenCoreRenderer implements BlockEntityRenderer<WardenCoreBlockEntity> {
    private static final ResourceLocation SOULS = new ResourceLocation(GoetyBossServants.MOD_ID, "textures/entity/warden_core_souls.png");
    // Just above the top face, so it doesn't flicker against it.
    private static final float Y = 8.02f / 16.0f;

    public WardenCoreRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WardenCoreBlockEntity core, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffers, int light, int overlay) {
        if (core.getLevel() == null) {
            return;
        }
        float glow = WardenCoreBlock.heartbeat(core.getBlockPos(), core.getLevel().getGameTime(), partialTick);
        if (glow <= 0.0f) {
            return;
        }
        poseStack.pushPose();
        // The block model is turned by its blockstate (north = 0, then clockwise); turn the souls with it.
        poseStack.translate(0.5f, 0.0f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-(core.getBlockState().getValue(WardenCoreBlock.FACING).toYRot() + 180.0f)));
        poseStack.translate(-0.5f, 0.0f, -0.5f);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucentEmissive(SOULS));
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        // The top face's corners as the block model maps them: texture u east, v south.
        vertex(buffer, matrix, normal, 0.0f, 0.0f, 0.0f, 0.0f, glow);
        vertex(buffer, matrix, normal, 0.0f, 1.0f, 0.0f, 1.0f, glow);
        vertex(buffer, matrix, normal, 1.0f, 1.0f, 1.0f, 1.0f, glow);
        vertex(buffer, matrix, normal, 1.0f, 0.0f, 1.0f, 0.0f, glow);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal, float x, float z, float u, float v, float alpha) {
        buffer.vertex(matrix, x, Y, z).color(1.0f, 1.0f, 1.0f, alpha).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(normal, 0.0f, 1.0f, 0.0f).endVertex();
    }
}
