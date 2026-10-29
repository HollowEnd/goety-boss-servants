package com.saints.goetybossservants.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityIronGolem;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/** The Iron Monstrosity's golems look and move like vanilla Iron Golems (vanilla model layer and texture). */
public class MonstrosityIronGolemRenderer extends MobRenderer<MonstrosityIronGolem, MonstrosityIronGolemRenderer.Model> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/entity/iron_golem/iron_golem.png");

    public MonstrosityIronGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModelLayers.IRON_GOLEM)), 0.7f);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull MonstrosityIronGolem entity) {
        return TEXTURE;
    }

    // Vanilla's sway while walking.
    @Override
    protected void setupRotations(@NotNull MonstrosityIronGolem entity, @NotNull PoseStack poseStack, float bob, float yaw, float partialTicks) {
        super.setupRotations(entity, poseStack, bob, yaw, partialTicks);
        if (entity.walkAnimation.speed() >= 0.01) {
            float position = entity.walkAnimation.position(partialTicks) + 6.0f;
            float swing = (Math.abs(position % 13.0f - 6.5f) - 3.25f) / 3.25f;
            poseStack.mulPose(Axis.ZP.rotationDegrees(6.5f * swing));
        }
    }

    /** Vanilla's IronGolemModel, for this servant type. */
    public static class Model extends HierarchicalModel<MonstrosityIronGolem> {
        private final ModelPart root;
        private final ModelPart head;
        private final ModelPart rightArm;
        private final ModelPart leftArm;
        private final ModelPart rightLeg;
        private final ModelPart leftLeg;

        public Model(ModelPart root) {
            this.root = root;
            this.head = root.getChild("head");
            this.rightArm = root.getChild("right_arm");
            this.leftArm = root.getChild("left_arm");
            this.rightLeg = root.getChild("right_leg");
            this.leftLeg = root.getChild("left_leg");
        }

        @Override
        public @NotNull ModelPart root() {
            return this.root;
        }

        @Override
        public void setupAnim(@NotNull MonstrosityIronGolem entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0f);
            this.head.xRot = headPitch * ((float) Math.PI / 180.0f);
            this.rightLeg.xRot = -1.5f * Mth.triangleWave(limbSwing, 13.0f) * limbSwingAmount;
            this.leftLeg.xRot = 1.5f * Mth.triangleWave(limbSwing, 13.0f) * limbSwingAmount;
            this.rightLeg.yRot = 0.0f;
            this.leftLeg.yRot = 0.0f;
        }

        @Override
        public void prepareMobModel(@NotNull MonstrosityIronGolem entity, float limbSwing, float limbSwingAmount, float partialTicks) {
            int attack = entity.getAttackAnimationTick();
            if (attack > 0) {
                this.rightArm.xRot = -2.0f + 1.5f * Mth.triangleWave(attack - partialTicks, 10.0f);
                this.leftArm.xRot = -2.0f + 1.5f * Mth.triangleWave(attack - partialTicks, 10.0f);
            } else {
                this.rightArm.xRot = (-0.2f + 1.5f * Mth.triangleWave(limbSwing, 13.0f)) * limbSwingAmount;
                this.leftArm.xRot = (-0.2f - 1.5f * Mth.triangleWave(limbSwing, 13.0f)) * limbSwingAmount;
            }
        }
    }
}
