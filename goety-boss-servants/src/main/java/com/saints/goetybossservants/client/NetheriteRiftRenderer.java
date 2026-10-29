package com.saints.goetybossservants.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.entity.monstrosity.Ground;
import com.saints.goetybossservants.entity.monstrosity.NetheriteRift;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Draws a Netherite rift flat on the ground, glowing: a fissure as a strip of crack texture, one
 * block per tile, following the ground block by block and narrowing towards its end; a crater as
 * one square of radial cracks. Both glow white-hot at first, then cool to dark red and fade.
 * Textures from tools/MonstrositySkins.java.
 */
public class NetheriteRiftRenderer extends EntityRenderer<NetheriteRift> {
    private static final ResourceLocation FISSURE = new ResourceLocation(GoetyBossServants.MOD_ID, "textures/entity/monstrosity/netherite/netherite_rift.png");
    private static final ResourceLocation CRATER = new ResourceLocation(GoetyBossServants.MOD_ID, "textures/entity/monstrosity/netherite/netherite_crater.png");
    private static final float WIDTH = 1.6f;
    private static final float LIFT = 0.02f;

    public NetheriteRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(@NotNull NetheriteRift rift, float yaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffers, int light) {
        float age = rift.tickCount + partialTicks;
        float heat = Mth.clamp((NetheriteRift.LIFE - age) / NetheriteRift.FADE_TICKS, 0.0f, 1.0f);
        if (heat <= 0.0f) {
            return;
        }
        float grown = Mth.clamp(age / rift.growTicks(), 0.0f, 1.0f);
        // Cools from full colour to a dull red as it fades out.
        float red = 0.55f + 0.45f * heat, green = 0.25f + 0.75f * heat, blue = 0.2f + 0.8f * heat, alpha = Math.min(1.0f, heat * 1.6f);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucentEmissive(rift.isCrater() ? CRATER : FISSURE));
        PoseStack.Pose pose = poseStack.last();
        if (rift.isCrater()) {
            float r = rift.size() * (0.6f + 0.4f * grown);
            quad(pose, buffer, -r, LIFT, -r, r, LIFT, -r, r, LIFT, r, -r, LIFT, r, 0.0f, 1.0f, red, green, blue, alpha);
        } else {
            float length = rift.size() * grown;
            double angle = Math.toRadians(rift.getYRot());
            float dx = (float) Math.cos(angle), dz = (float) Math.sin(angle);
            for (int i = 0; i < Mth.ceil(length); ++i) {
                float start = i, end = Math.min(i + 1.0f, length);
                float y = this.groundOffset(rift, start + 0.5f, dx, dz);
                float w0 = WIDTH / 2.0f * taper(start / rift.size()), w1 = WIDTH / 2.0f * taper(end / rift.size());
                // Corners: left and right of the start and the end, the side direction being (-dz, dx).
                quad(pose, buffer,
                        dx * start - dz * w0, y, dz * start + dx * w0,
                        dx * start + dz * w0, y, dz * start - dx * w0,
                        dx * end + dz * w1, y, dz * end - dx * w1,
                        dx * end - dz * w1, y, dz * end + dx * w1,
                        0.0f, end - start, red, green, blue, alpha);
            }
        }
        super.render(rift, yaw, partialTicks, poseStack, buffers, light);
    }

    // Narrows over the last third of a fissure.
    private static float taper(float along) {
        return along < 0.66f ? 1.0f : Math.max(0.15f, 1.0f - (along - 0.66f) * 2.5f);
    }

    // The height of the ground under a point of the fissure, relative to the rift (see Ground.floorAt:
    // it steps up and down with the ground, but lies under a roof, leaves or water, and never climbs a
    // cliff).
    private float groundOffset(NetheriteRift rift, float along, float dx, float dz) {
        return (float) (Ground.floorAt(rift.level(), rift.getX() + dx * along, rift.getY(), rift.getZ() + dz * along) - rift.getY()) + LIFT;
    }

    // One textured quad, both sides, at full brightness; u runs along it from u0 to u1, v across it.
    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3, float u0, float u1, float red, float green, float blue, float alpha) {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        float[][] corners = {{x0, y0, z0, u0, 0.0f}, {x1, y1, z1, u0, 1.0f}, {x2, y2, z2, u1, 1.0f}, {x3, y3, z3, u1, 0.0f}};
        for (int side = 0; side < 2; ++side) {
            for (int i = 0; i < 4; ++i) {
                float[] c = corners[side == 0 ? i : 3 - i];
                buffer.vertex(matrix, c[0], c[1], c[2]).color(red, green, blue, alpha).uv(c[3], c[4]).overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(0xF000F0).normal(normal, 0.0f, 1.0f, 0.0f).endVertex();
            }
        }
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull NetheriteRift rift) {
        return rift.isCrater() ? CRATER : FISSURE;
    }
}
