package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.registry.ModEffects;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Anything the Gold Monstrosity has turned to gold is drawn with the gold block texture stretched over its whole model, like a golden statue. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Unique
    private static final ResourceLocation goetybossservants$GOLD = new ResourceLocation("minecraft", "textures/block/gold_block.png");

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void goetybossservants$gildedTexture(LivingEntity entity, boolean bodyVisible, boolean translucent, boolean glowing, CallbackInfoReturnable<RenderType> cir) {
        if (bodyVisible && entity.hasEffect(ModEffects.GILDED)) {
            cir.setReturnValue(RenderType.entityCutoutNoCull(goetybossservants$GOLD));
        }
    }
}
