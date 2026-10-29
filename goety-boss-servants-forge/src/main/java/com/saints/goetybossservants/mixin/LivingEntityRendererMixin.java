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

/**
 * Anything the Gold Monstrosity has turned to gold is drawn with the gold block texture stretched
 * over its whole model, like a golden statue. Named twice (dev name and SRG name) since this mod
 * ships no refmap; whichever exists in the running game is used.
 */
@Mixin(value = LivingEntityRenderer.class, remap = false)
public abstract class LivingEntityRendererMixin {
    @Unique
    private static final ResourceLocation goetybossservants$GOLD = new ResourceLocation("minecraft", "textures/block/gold_block.png");

    @Inject(method = {"getRenderType", "m_7225_"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void goetybossservants$gildedTexture(LivingEntity entity, boolean bodyVisible, boolean translucent, boolean glowing, CallbackInfoReturnable<RenderType> cir) {
        if (bodyVisible && entity.hasEffect(ModEffects.GILDED.get())) {
            cir.setReturnValue(RenderType.entityCutoutNoCull(goetybossservants$GOLD));
        }
    }
}
