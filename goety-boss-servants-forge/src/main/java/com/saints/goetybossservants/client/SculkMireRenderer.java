package com.saints.goetybossservants.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.entity.monstrosity.Ground;
import com.saints.goetybossservants.entity.monstrosity.SculkMire;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Draws sculk mire flat on the ground, block by block so it follows the ground and its sculk is
 * never stretched (16 pixels a block, textures from tools/MonstrositySkins.java): a line spreads
 * out from its start, with the texture's rounded first and last blocks at its two ends; a pool
 * fades in. Both fade away at the end.
 */
public class SculkMireRenderer extends EntityRenderer<SculkMire> {
    private static final ResourceLocation LINE = new ResourceLocation(GoetyBossServants.MOD_ID, "textures/entity/monstrosity/sculk/sculk_mire.png");
    private static final ResourceLocation POOL = new ResourceLocation(GoetyBossServants.MOD_ID, "textures/entity/monstrosity/sculk/sculk_pool.png");
    /** Blocks along the line's texture: a rounded start, six to repeat between, a rounded end. */
    private static final int LINE_TILES = 8;

    public SculkMireRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(@NotNull SculkMire mire, float yaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffers, int light) {
        float age = mire.tickCount + partialTicks;
        float alpha = Mth.clamp((mire.life() - age) / SculkMire.FADE_TICKS, 0.0f, 1.0f);
        if (alpha <= 0.0f) {
            return;
        }
        // Mire overlapping mire sits at slightly different heights, so they don't flicker.
        float lift = 0.015f + (mire.getId() & 7) * 0.004f;
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(this.getTextureLocation(mire)));
        PoseStack.Pose pose = poseStack.last();
        float grown = Mth.clamp(age / SculkMire.GROW_TICKS, 0.0f, 1.0f);
        if (mire.isPool()) {
            float r = mire.size();
            alpha *= grown;
            for (float x0 = -r; x0 < r; x0 += 1.0f) {
                for (float z0 = -r; z0 < r; z0 += 1.0f) {
                    float x1 = Math.min(x0 + 1.0f, r), z1 = Math.min(z0 + 1.0f, r);
                    float y = this.groundOffset(mire, (x0 + x1) / 2.0f, (z0 + z1) / 2.0f) + lift;
                    quad(pose, buffer, x0, z0, x0, z1, x1, z1, x1, z0, y, (x0 + r) / (2.0f * r), (z0 + r) / (2.0f * r),
                            (x1 + r) / (2.0f * r), (z1 + r) / (2.0f * r), alpha, light);
                }
            }
        } else {
            float length = mire.size();
            int blocks = Mth.ceil(length);
            Vec3 dir = mire.direction();
            float dx = (float) dir.x, dz = (float) dir.z, w = (float) SculkMire.HALF_WIDTH;
            for (int i = 0; i < blocks && i < blocks * grown; ++i) {
                float a = i, b = Math.min(i + 1.0f, length);
                int tile = i == 0 ? 0 : i == blocks - 1 ? LINE_TILES - 1 : 1 + (i - 1) % (LINE_TILES - 2);
                float y = this.groundOffset(mire, dx * (a + b) / 2.0f, dz * (a + b) / 2.0f) + lift;
                // Left and right of the start, then of the end (the side direction being (dz, -dx)); v runs across.
                quad(pose, buffer, dx * a + dz * w, dz * a - dx * w, dx * a - dz * w, dz * a + dx * w,
                        dx * b - dz * w, dz * b + dx * w, dx * b + dz * w, dz * b - dx * w, y,
                        tile / (float) LINE_TILES, 0.0f, (tile + b - a) / LINE_TILES, 1.0f, alpha, light);
            }
        }
        super.render(mire, yaw, partialTicks, poseStack, buffers, light);
    }

    // The height of the floor under a point of it, relative to it (see Ground.floorAt: it steps up and
    // down with the ground, but lies under a roof, leaves or water, and never climbs a cliff).
    private float groundOffset(SculkMire mire, float x, float z) {
        return (float) (Ground.floorAt(mire.level(), mire.getX() + x, mire.getY(), mire.getZ() + z) - mire.getY());
    }

    // One flat textured quad, both sides, its corners (x, z) at height y, uv running from (u0, v0) at the
    // first corner to (u1, v1) at the third.
    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, float x0, float z0, float x1, float z1, float x2, float z2, float x3, float z3,
                             float y, float u0, float v0, float u1, float v1, float alpha, int light) {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        float[][] corners = {{x0, z0, u0, v0}, {x1, z1, u0, v1}, {x2, z2, u1, v1}, {x3, z3, u1, v0}};
        for (int side = 0; side < 2; ++side) {
            for (int i = 0; i < 4; ++i) {
                float[] c = corners[side == 0 ? i : 3 - i];
                buffer.vertex(matrix, c[0], y, c[1]).color(1.0f, 1.0f, 1.0f, alpha).uv(c[2], c[3]).overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(light).normal(normal, 0.0f, 1.0f, 0.0f).endVertex();
            }
        }
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SculkMire mire) {
        return mire.isPool() ? POOL : LINE;
    }
}
