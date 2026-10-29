package com.saints.goetybossservants.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.saints.goetybossservants.client.RisenRenderHooks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Around the call that draws each entity, risen mobs get their rise and their sculk (see
 * RisenRenderHooks). Named twice (dev name and SRG name, of the method and of the call) since this
 * mod ships no refmap; whichever exists in the running game is used.
 */
@Mixin(value = EntityRenderDispatcher.class, remap = false)
public abstract class EntityRenderDispatcherMixin {
    @Unique
    private static final String RENDER = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";
    @Unique
    private static final String RENDER_SRG = "Lnet/minecraft/client/renderer/entity/EntityRenderer;m_7392_(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    @Inject(method = {"render", "m_114384_"}, at = @At(value = "INVOKE", target = RENDER), require = 0)
    private void goetybossservants$beforeDraw(Entity entity, double x, double y, double z, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
        RisenRenderHooks.before(entity, partialTicks, poseStack);
    }

    @Inject(method = {"render", "m_114384_"}, at = @At(value = "INVOKE", target = RENDER_SRG), require = 0)
    private void goetybossservants$beforeDrawSrg(Entity entity, double x, double y, double z, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
        RisenRenderHooks.before(entity, partialTicks, poseStack);
    }

    @Inject(method = {"render", "m_114384_"}, at = @At(value = "INVOKE", target = RENDER, shift = At.Shift.AFTER), require = 0)
    private void goetybossservants$afterDraw(Entity entity, double x, double y, double z, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
        RisenRenderHooks.after((EntityRenderDispatcher) (Object) this, entity, yaw, partialTicks, poseStack, buffers, light);
    }

    @Inject(method = {"render", "m_114384_"}, at = @At(value = "INVOKE", target = RENDER_SRG, shift = At.Shift.AFTER), require = 0)
    private void goetybossservants$afterDrawSrg(Entity entity, double x, double y, double z, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
        RisenRenderHooks.after((EntityRenderDispatcher) (Object) this, entity, yaw, partialTicks, poseStack, buffers, light);
    }
}
