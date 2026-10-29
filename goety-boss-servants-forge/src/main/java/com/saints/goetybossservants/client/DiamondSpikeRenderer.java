package com.saints.goetybossservants.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.saints.goetybossservants.entity.monstrosity.DiamondSpike;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * A diamond spike: three stacked, narrowing diamond blocks. Ground spikes grow out of the ground
 * and sink back at the end; falling spikes point down.
 */
public class DiamondSpikeRenderer extends EntityRenderer<DiamondSpike> {
    private static final BlockState DIAMOND = Blocks.DIAMOND_BLOCK.defaultBlockState();
    // Width and height of each tier, bottom to top.
    private static final float[][] TIERS = {{0.7f, 0.7f}, {0.45f, 0.8f}, {0.2f, 0.6f}};

    public DiamondSpikeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(@NotNull DiamondSpike spike, float yaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffers, int light) {
        float age = spike.tickCount + partialTicks;
        float grow;
        if (spike.isFalling()) {
            grow = 0.7f;
        } else if (age < DiamondSpike.RISE_TICKS) {
            grow = age / DiamondSpike.RISE_TICKS;
        } else {
            grow = Mth.clamp((DiamondSpike.GROUND_LIFE - age) / DiamondSpike.SINK_TICKS, 0.0f, 1.0f);
        }
        if (grow <= 0.0f) {
            return;
        }
        BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(spike.getYRot()));
        if (spike.isFalling()) {
            poseStack.translate(0.0, 2.1 * grow, 0.0);
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
            poseStack.scale(grow, grow, grow);
        } else {
            poseStack.scale(1.0f, grow, 1.0f);
        }
        float y = 0.0f;
        for (float[] tier : TIERS) {
            poseStack.pushPose();
            poseStack.translate(-tier[0] / 2.0f, y, -tier[0] / 2.0f);
            poseStack.scale(tier[0], tier[1], tier[0]);
            blocks.renderSingleBlock(DIAMOND, poseStack, buffers, light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            y += tier[1];
        }
        poseStack.popPose();
        super.render(spike, yaw, partialTicks, poseStack, buffers, light);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull DiamondSpike spike) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
