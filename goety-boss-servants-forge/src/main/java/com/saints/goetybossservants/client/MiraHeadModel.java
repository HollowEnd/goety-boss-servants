package com.saints.goetybossservants.client;

import com.jerotes.meowhem.client.model.Modelobsidian_monstrosity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.saints.goetybossservants.entity.MiraVariant;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws just the head of meowhem's Mira Monstrosity model (horns and jaw included), shrunk down to
 * roughly one block, for the head block and its item.
 */
public class MiraHeadModel {
    private static final ResourceLocation MIRA = new ResourceLocation("meowhem", "textures/entity/mira_monstrosity/mira_monstrosity.png");
    private static final ResourceLocation OBSIDIAN = new ResourceLocation("meowhem", "textures/entity/mira_monstrosity/mira_monstrosity_obsidian_monstrosity.png");
    private static final ResourceLocation WARDEN = new ResourceLocation("meowhem", "textures/entity/mira_monstrosity/mira_monstrosity_warden.png");
    // The model is in pixels; this makes the 56px wide head about 0.6 blocks and the horns about 1.5.
    private static final float SCALE = 0.18f;
    // Bottom of the jaw, in pixels below the head's pivot.
    private static final float JAW_BOTTOM = 37.75f;

    private final ModelPart head;

    public MiraHeadModel(EntityModelSet models) {
        this.head = models.bakeLayer(Modelobsidian_monstrosity.LAYER_LOCATION).getChild("body").getChild("head");
    }

    public static ResourceLocation texture(MiraVariant variant) {
        return switch (variant) {
            case OBSIDIAN -> OBSIDIAN;
            case WARDEN -> WARDEN;
            default -> MIRA;
        };
    }

    /** Renders the head standing on the block at the pose's origin, facing away from {@code yRot} like a mob skull. */
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, float yRot, MiraVariant variant) {
        poseStack.pushPose();
        poseStack.translate(0.5, JAW_BOTTOM * SCALE / 16.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - yRot));
        poseStack.scale(-SCALE, -SCALE, SCALE);
        // Undo any pose left over and drop the head's offset inside the body.
        this.head.getAllParts().forEach(ModelPart::resetPose);
        this.head.setPos(0.0f, 0.0f, 0.0f);
        this.head.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(texture(variant))), light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
