package com.saints.goetybossservants.client;

import com.Polarice3.Goety.client.render.ModModelLayer;
import com.Polarice3.Goety.client.render.RedstoneMonstrosityRenderer;
import com.Polarice3.Goety.client.render.model.RedstoneMonstrosityModel;
import com.Polarice3.Goety.config.MobsConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.entity.monstrosity.MaterialMonstrosity;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * Goety's Redstone Monstrosity renderer (model, animations, death dissolve) with every texture
 * layer swapped for the material's own set, in the same order and with the same rules as Goety's
 * layers. The Iron, Lapis, Diamond, Netherite and Sculk ones' channels are animated (running
 * water, a starfield, crystal, lava, sculk): their channel layer and attack glow step through numbered frames.
 */
public class MaterialMonstrosityRenderer extends RedstoneMonstrosityRenderer<MaterialMonstrosity> {
    private static final Map<MonstrosityMaterial, Map<String, ResourceLocation>> TEXTURES = new EnumMap<>(MonstrosityMaterial.class);

    public MaterialMonstrosityRenderer(EntityRendererProvider.Context context) {
        super(context);
        // Goety's model, reshaped for some materials (see MaterialMonstrosityModel).
        this.model = new MaterialMonstrosityModel(context.bakeLayer(ModModelLayer.REDSTONE_MONSTROSITY));
        this.layers.clear();
        // Goety's eyes are swapped for centred ones (see MonstrosityExtras), drawn in every layer.
        MonstrosityExtras.hideGoetyEyes(this.getModel().root(), false);
        this.addLayer(new BaseEyesLayer(this));
        this.addLayer(new TextureLayer(this, "eyes", RenderType::eyes, entity -> true, 0xF000F0, entity -> 1.0));
        this.addLayer(new TextureLayer(this, "lines", RenderType::entityCutoutNoCull, entity -> !entity.isActivating() && !entity.isDeadOrDying(), 0xF000F0, entity -> 1.0));
        // The sculk inside the Sculk one's shoulder shriekers: animated, but lit like the rest of it (a real
        // shrieker's does not glow).
        this.addLayer(new TextureLayer(this, "inner", RenderType::entityCutoutNoCull, entity -> entity.material() == MonstrosityMaterial.SCULK && !entity.isDeadOrDying(), -1, entity -> 1.0));
        this.addLayer(new TextureLayer(this, "non_active", RenderType::entityCutoutNoCull, MaterialMonstrosity::isActivating, -1, entity -> 1.0));
        this.addLayer(new TextureLayer(this, "bands", RenderType::entityCutoutNoCull, entity -> MobsConfig.RedstoneMonstrosityTexture.get(), -1, entity -> 1.0));
        this.addLayer(new ExtrasLayer(this));
        this.addLayer(new TextureLayer(this, "glow", RenderType::entityTranslucentEmissive, entity -> !entity.isDeadOrDying() && entity.getBigGlow() > 0.0f, -1, entity -> entity.getBigGlow()));
        this.addLayer(new TextureLayer(this, "active", RenderType::entityTranslucentEmissive,
                entity -> !entity.isActivating() && !entity.isDeadOrDying() && entity.getBigGlow() <= 0.0f, -1, entity -> entity.getMinorGlow()));
    }

    /** A texture of the material's set: "" for the base skin, otherwise the layer's suffix ("eyes", "glow", ...). */
    public static ResourceLocation texture(MonstrosityMaterial material, String layer) {
        return TEXTURES.computeIfAbsent(material, key -> new java.util.HashMap<>()).computeIfAbsent(layer, key -> {
            String name = material.entityName() + (layer.isEmpty() ? "" : "_" + layer);
            return new ResourceLocation(GoetyBossServants.MOD_ID, "textures/entity/monstrosity/" + material.getId() + "/" + name + ".png");
        });
    }

    /** The animated channel frame to show (see tools/MonstrositySkins.java), or -1 for a still one. */
    static int channelFrame(MonstrosityMaterial material, float ageInTicks) {
        return switch (material) {
            case IRON -> (int) (ageInTicks / 2.0f) % 32;
            case LAPIS, DIAMOND, SCULK -> (int) (ageInTicks / 3.0f) % 16;
            case NETHERITE -> (int) (ageInTicks / 2.0f) % 20;
            default -> -1;
        };
    }

    @Override
    protected void scale(@NotNull MaterialMonstrosity entity, @NotNull PoseStack poseStack, float partialTicks) {
        float scale = entity.material().scale() * (entity.isMinion() ? 0.5f : 1.0f);
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull MaterialMonstrosity entity) {
        return texture(entity.material(), "");
    }

    // Goety's death dissolve uses its own texture here; use the material's.
    @Override
    @Nullable
    protected RenderType getRenderType(@NotNull MaterialMonstrosity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        if (entity.getDeathTime() >= 24) {
            return RenderType.dragonExplosionAlpha(this.getTextureLocation(entity));
        }
        return super.getRenderType(entity, bodyVisible, translucent, glowing);
    }

    /** The centred eyes in the base skin (the base model itself is drawn before any layer). */
    private static class BaseEyesLayer extends RenderLayer<MaterialMonstrosity, RedstoneMonstrosityModel<MaterialMonstrosity>> {
        private final MaterialMonstrosityRenderer renderer;

        BaseEyesLayer(MaterialMonstrosityRenderer renderer) {
            super(renderer);
            this.renderer = renderer;
        }

        @Override
        public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffers, int light, @NotNull MaterialMonstrosity entity,
                           float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            RenderType type = entity.isInvisible() ? null : this.renderer.getRenderType(entity, true, false, Minecraft.getInstance().shouldEntityAppearGlowing(entity));
            if (type != null) {
                MonstrosityExtras.renderEyes(entity.material(), this.getParentModel().root(), false, poseStack, buffers.getBuffer(type), light,
                        LivingEntityRenderer.getOverlayCoords(entity, 0.0f), 1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
    }

    /** The material's extra model pieces (see MonstrosityExtras), until the death dissolve starts. */
    private static class ExtrasLayer extends RenderLayer<MaterialMonstrosity, RedstoneMonstrosityModel<MaterialMonstrosity>> {
        ExtrasLayer(RenderLayerParent<MaterialMonstrosity, RedstoneMonstrosityModel<MaterialMonstrosity>> parent) {
            super(parent);
        }

        @Override
        public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffers, int light, @NotNull MaterialMonstrosity entity,
                           float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!entity.isInvisible() && entity.getDeathTime() < 24) {
                MonstrosityExtras.render(entity.material(), this.getParentModel().root(), false, !entity.getHeldBook().isEmpty(), ageInTicks, limbSwing, limbSwingAmount,
                        ((MaterialMonstrosityModel) this.getParentModel()).tentacleCurl, poseStack, buffers, light, LivingEntityRenderer.getOverlayCoords(entity, 0.0f));
            }
        }
    }

    /** One of Goety's monstrosity layers, drawn with the material's texture of that layer. */
    private static class TextureLayer extends RenderLayer<MaterialMonstrosity, RedstoneMonstrosityModel<MaterialMonstrosity>> {
        private final String layer;
        private final java.util.function.Function<ResourceLocation, RenderType> renderType;
        private final Predicate<MaterialMonstrosity> shown;
        private final int fixedLight;
        private final ToDoubleFunction<MaterialMonstrosity> alpha;

        TextureLayer(RenderLayerParent<MaterialMonstrosity, RedstoneMonstrosityModel<MaterialMonstrosity>> parent, String layer,
                     java.util.function.Function<ResourceLocation, RenderType> renderType, Predicate<MaterialMonstrosity> shown, int fixedLight,
                     ToDoubleFunction<MaterialMonstrosity> alpha) {
            super(parent);
            this.layer = layer;
            this.renderType = renderType;
            this.shown = shown;
            this.fixedLight = fixedLight;
            this.alpha = alpha;
        }

        private String layer(MaterialMonstrosity entity, float ageInTicks) {
            // The shriekers' insides go round in 30 ticks, as a real shrieker's does.
            int frame = this.layer.equals("inner") ? (int) (ageInTicks * 16.0f / 30.0f) % 16
                    : this.layer.equals("lines") || this.layer.equals("glow") ? channelFrame(entity.material(), ageInTicks) : -1;
            return frame >= 0 ? this.layer + "_" + frame : this.layer;
        }

        @Override
        public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffers, int light, @NotNull MaterialMonstrosity entity,
                           float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entity.isInvisible() || !this.shown.test(entity)) {
                return;
            }
            float alpha = (float) this.alpha.applyAsDouble(entity);
            if (alpha <= 0.0f) {
                return;
            }
            VertexConsumer buffer = buffers.getBuffer(this.renderType.apply(texture(entity.material(), this.layer(entity, ageInTicks))));
            int layerLight = this.fixedLight >= 0 ? this.fixedLight : light;
            int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0f);
            this.getParentModel().renderToBuffer(poseStack, buffer, layerLight, overlay, 1.0f, 1.0f, 1.0f, alpha);
            MonstrosityExtras.renderEyes(entity.material(), this.getParentModel().root(), false, poseStack, buffer, layerLight, overlay, 1.0f, 1.0f, 1.0f, alpha);
        }
    }
}
