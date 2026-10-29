package com.saints.goetybossservants.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import com.saints.goetybossservants.entity.monstrosity.SculkTentacle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * A sculk tentacle of the Sculk Monstrosity's special: its own tentacle arm's pieces (see
 * MonstrosityExtras.renderTentacle), rising out of the ground and slamming the way it faces, posed
 * by its age (SculkTentacle.curl).
 */
public class SculkTentacleRenderer extends EntityRenderer<SculkTentacle> {
    /** How far its base is sunk into the ground, so it grows out of it rather than standing on it. */
    private static final float SUNK = 0.4f;
    /** Its length, in blocks (12 segments of 8 pixels). */
    private static final float LENGTH = 6.0f;

    public SculkTentacleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.7f;
    }

    @Override
    public void render(@NotNull SculkTentacle tentacle, float yaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffers, int light) {
        float age = tentacle.tickCount + partialTicks;
        float risen = SculkTentacle.risen(age);
        if (risen <= 0.0f) {
            return;
        }
        this.shadowStrength = risen;
        poseStack.pushPose();
        poseStack.translate(0.0f, -SUNK - (1.0f - risen) * LENGTH, 0.0f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-tentacle.getYRot()));
        MonstrosityExtras.renderTentacle(SculkTentacle.curl(age), age, poseStack, buffers, light);
        poseStack.popPose();
        super.render(tentacle, yaw, partialTicks, poseStack, buffers, light);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SculkTentacle tentacle) {
        return MaterialMonstrosityRenderer.texture(MonstrosityMaterial.SCULK, "parts");
    }
}
